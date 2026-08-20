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
    private val initialUserId: Int? = null
) : ViewModel() {
    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats
    private val _reviews = MutableStateFlow<List<UserReview>>(emptyList())
    val reviews: StateFlow<List<UserReview>> = _reviews
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore
    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing
    private val _isFollowLoading = MutableStateFlow(false)
    val isFollowLoading: StateFlow<Boolean> = _isFollowLoading

    private var currentPage = 1
    private var isLoading = false
    private var endReached = false
    private var viewingUserId: Int? = null
    private var currentSessionUserId: Int? = null

    init {
        viewModelScope.launch { currentSessionUserId = sessionManager.getUserId() }
        loadUser(initialUserId)
    }

    private fun loadUser(userIdParam: Int? = null) {
        isLoading = true
        viewModelScope.launch {
            try {
                val userId = userIdParam ?: sessionManager.getUserId() ?: return@launch
                viewingUserId = userId
                repository.getUserProfile(userId, currentSessionUserId ?: sessionManager.getUserId())
                    .onSuccess { stats ->
                        _userStats.value = stats
                        _isFollowing.value = stats.isFollowing
                    }
                    .onFailure { println("ProfileViewModel: getUserProfile FAILED for userId=$userId: ${it.message}") }
                repository.getUserReviews(userId, 1)
                    .onSuccess { reviews ->
                        _reviews.value = reviews
                        currentPage = 2
                        endReached = reviews.size < 10
                        println("ProfileViewModel: initial page 1 loaded, ${reviews.size} reviews, currentPage now=$currentPage, endReached=$endReached")
                    }
                    .onFailure { println("ProfileViewModel: initial getUserReviews(page=1) FAILED for userId=$userId: ${it.message}") }
            } finally {
                isLoading = false
            }
        }
    }

    fun updateProfileImage(imageUrl: String?) {
        val userId = viewingUserId ?: return
        viewModelScope.launch {
            repository.updateProfileImage(userId, imageUrl)
                .onSuccess { savedUrl ->
                    _userStats.value = _userStats.value?.copy(profileImageUrl = savedUrl)
                }
                .onFailure { println("Profile image update failed: ${it.message}") }
        }
    }

    fun loadMoreReviews() {
        if (isLoading || endReached) return
        val userId = viewingUserId ?: return
        isLoading = true
        _isLoadingMore.value = true
        val requestedPage = currentPage
        println("ProfileViewModel: loadMoreReviews() requesting page=$requestedPage")
        viewModelScope.launch {
            repository.getUserReviews(userId, requestedPage)
                .onSuccess { nextReviews ->
                    _reviews.value += nextReviews
                    if (nextReviews.size < 10) endReached = true else currentPage++
                    println("ProfileViewModel: loadMoreReviews() got ${nextReviews.size} reviews for page=$requestedPage, currentPage now=$currentPage, endReached=$endReached")
                }
                .onFailure { println("ProfileViewModel: loadMoreReviews() FAILED for page=$requestedPage: ${it.message}") }
            isLoading = false
            _isLoadingMore.value = false
        }
    }

    fun toggleFollow() {
        if (_isFollowLoading.value) return
        val userId = viewingUserId ?: return
        val currentUserId = currentSessionUserId ?: return
        if (userId == currentUserId) return
        _isFollowLoading.value = true
        viewModelScope.launch {
            try {
                if (_isFollowing.value) {
                    repository.unfollowUser(userId, currentUserId).onSuccess {
                        _isFollowing.value = false
                        _userStats.value = _userStats.value?.copy(
                            followerCount = (_userStats.value?.followerCount ?: 1) - 1,
                            isFollowing = false
                        )
                    }
                } else {
                    repository.followUser(userId, currentUserId).onSuccess {
                        _isFollowing.value = true
                        _userStats.value = _userStats.value?.copy(
                            followerCount = (_userStats.value?.followerCount ?: 0) + 1,
                            isFollowing = true
                        )
                    }
                }
            } finally {
                _isFollowLoading.value = false
            }
        }
    }
}