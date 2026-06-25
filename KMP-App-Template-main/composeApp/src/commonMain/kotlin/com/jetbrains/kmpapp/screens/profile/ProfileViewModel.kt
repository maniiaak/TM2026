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
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _userStats =
        MutableStateFlow<UserStats?>(null)

    val userStats: StateFlow<UserStats?> =
        _userStats

    private val _reviews =
        MutableStateFlow<List<UserReview>>(emptyList())

    val reviews: StateFlow<List<UserReview>> =
        _reviews

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {

            val userId =
                sessionManager.getUserId() ?: return@launch

            repository.getUserStats(userId)
                .onSuccess {
                    _userStats.value = it
                }

            repository.getUserReviews(userId)
                .onSuccess {
                    _reviews.value = it
                }
        }
    }
}