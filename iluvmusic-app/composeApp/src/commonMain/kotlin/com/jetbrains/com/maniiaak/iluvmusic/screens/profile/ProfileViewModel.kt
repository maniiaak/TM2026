package com.maniiaak.iluvmusic.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maniiaak.iluvmusic.data.MuseumRepository
import com.maniiaak.iluvmusic.data.SessionManager
import com.maniiaak.iluvmusic.data.UserReview
import com.maniiaak.iluvmusic.data.UserStats
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
    private var currentSessionUserId: Int? = null

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing

    private val _isFollowLoading = MutableStateFlow(false)
    val isFollowLoading: StateFlow<Boolean> = _isFollowLoading

    init {
        // start loading for either the provided initial user id or the current session user
        loadUser(initialUserId)

        // Get current session user id for follow actions
        viewModelScope.launch {
            currentSessionUserId = sessionManager.getUserId()
        }
    }

    private fun loadUser(userIdParam: Int? = null) {
        viewModelScope.launch {
            // Prefer explicit id param, then session user id
            val userId = userIdParam

            if (userId != null) {
                viewingUserId = userId
                val currentUserId = currentSessionUserId

                repository.getUserStats(userId, currentUserId)
                    .onSuccess { stats ->
                        _userStats.value = stats
                        _isFollowing.value = stats.isFollowing
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

            repository.getUserStats(sessionUserId, sessionUserId)
                .onSuccess {
                    _userStats.value = it
                    _isFollowing.value = false
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

    fun toggleFollow() {
        if (_isFollowLoading.value) return

        val userId = viewingUserId ?: return
        val currentUserId = currentSessionUserId ?: return

        _isFollowLoading.value = true

        viewModelScope.launch {
            try {
                if (_isFollowing.value) {
                    // Unfollow
                    repository.unfollowUser(userId, currentUserId)
                        .onSuccess {
                            _isFollowing.value = false
                            // Decrement follower count
                            _userStats.value = _userStats.value?.copy(
                                followerCount = (_userStats.value?.followerCount ?: 1) - 1,
                                isFollowing = false
                            )
                        }
                        .onFailure { error ->
                            println("Unfollow failed: ${error.message}")
                        }
                } else {
                    // Follow
                    repository.followUser(userId, currentUserId)
                        .onSuccess {
                            _isFollowing.value = true
                            // Increment follower count
                            _userStats.value = _userStats.value?.copy(
                                followerCount = (_userStats.value?.followerCount ?: 0) + 1,
                                isFollowing = true
                            )
                        }
                        .onFailure { error ->
                            println("Follow failed: ${error.message}")
                        }
                }
            } finally {
                _isFollowLoading.value = false
            }
        }
    }
}