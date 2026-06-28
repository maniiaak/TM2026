package com.jetbrains.kmpapp.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.MuseumRepository
import com.jetbrains.kmpapp.data.SessionManager
import com.jetbrains.kmpapp.data.UserReview
import com.jetbrains.kmpapp.data.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: MuseumRepository,
    private val sessionManager: SessionManager,
    // If provided, this ViewModel will load data for this user id instead of the current session user
    private val initialUserId: Int? = null
) : ViewModel() {

    private val _userStats =
        MutableStateFlow<UserStats?>(null)

    val userStats: StateFlow<UserStats?> =
        _userStats

    private val _reviews =
        MutableStateFlow<List<UserReview>>(emptyList())

    val reviews: StateFlow<List<UserReview>> =
        _reviews

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private var currentPage = 1
    private var isLoading = false
    private var endReached = false

    private var viewingUserId: Int? = null

    init {
        // start loading for either the provided initial user id or the current session user
        loadUser(initialUserId)
    }

    private fun loadUser(userIdParam: Int? = null) {
        viewModelScope.launch {
            // Prefer explicit id param, then session user id
            val userId = userIdParam

            if (userId != null) {
                viewingUserId = userId

                repository.getUserStats(userId)
                    .onSuccess {
                        _userStats.value = it
                    }

                repository.getUserReviews(
                    userId = userId,
                    page = 1
                ).onSuccess { reviews ->

                    _reviews.value = reviews

                    currentPage = 2

                    if (reviews.size < 10) {
                        endReached = true
                    }
                }
                return@launch
            }

            // fallback to session user
            val sessionUserId = sessionManager.getUserId() ?: return@launch
            viewingUserId = sessionUserId

            repository.getUserStats(sessionUserId)
                .onSuccess {
                    _userStats.value = it
                }

            repository.getUserReviews(
                userId = sessionUserId,
                page = 1
            ).onSuccess { reviews ->

                _reviews.value = reviews

                currentPage = 2

                if (reviews.size < 10) {
                    endReached = true
                }
            }
        }
    }

    fun loadMoreReviews() {

        if (isLoading || endReached) return

        isLoading = true
        _isLoadingMore.value = true

        viewModelScope.launch {

            // If viewing by userId use that, else if viewing by username we call username endpoint
            val userId = viewingUserId

            if (userId != null) {
                repository.getUserReviews(
                    userId = userId,
                    page = currentPage
                ).onSuccess { reviews ->

                    _reviews.value += reviews

                    if (reviews.size < 10) {
                        endReached = true
                    } else {
                        currentPage++
                    }
                }
            }

            isLoading = false
            _isLoadingMore.value = false
        }
    }
}