package com.jetbrains.kmpapp.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.MuseumRepository
import com.jetbrains.kmpapp.data.MuseumObject
import com.jetbrains.kmpapp.data.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: MuseumRepository
) : ViewModel() {

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    private val _isLoadingReviews = MutableStateFlow(false)
    val isLoadingReviews: StateFlow<Boolean> = _isLoadingReviews

    private val _reviewState = MutableStateFlow<ReviewState>(ReviewState.Idle)
    val reviewState: StateFlow<ReviewState> = _reviewState

    // NEW: Function to get the album object
    fun getObject(objectId: Int): StateFlow<MuseumObject?> {
        val flow = MutableStateFlow<MuseumObject?>(null)
        viewModelScope.launch {
            repository.getObjectById(objectId).collect { obj ->
                flow.value = obj
            }
        }
        return flow
    }

    fun loadReviews(albumId: Int) {
        if (_reviews.value.isNotEmpty()) return

        viewModelScope.launch {
            _isLoadingReviews.value = true
            val result = repository.getReviewsForAlbum(albumId)

            result.fold(
                onSuccess = { reviewList ->
                    _reviews.value = reviewList
                },
                onFailure = { error ->
                }
            )
            _isLoadingReviews.value = false
        }
    }

    fun saveReview(rating: Float, content: String, albumId: Int, userId: Int) {
        if (_reviewState.value is ReviewState.Loading) return

        viewModelScope.launch {
            _reviewState.value = ReviewState.Loading
            try {
                repository.submitReview(rating, content, albumId, userId)
                _reviewState.value = ReviewState.Success
            } catch (e: Exception) {
                _reviewState.value = ReviewState.Error(e.message ?: "Failed to save review")
            }
        }
    }

    fun reset() {
        _reviewState.value = ReviewState.Idle
    }
}

sealed class ReviewState {
    object Idle : ReviewState()
    object Loading : ReviewState()
    object Success : ReviewState()
    data class Error(val message: String) : ReviewState()
}