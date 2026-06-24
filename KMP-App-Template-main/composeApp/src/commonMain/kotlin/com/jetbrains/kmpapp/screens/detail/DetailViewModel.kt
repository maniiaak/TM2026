package com.jetbrains.kmpapp.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.MuseumRepository
import com.jetbrains.kmpapp.data.Review
import com.jetbrains.kmpapp.data.AlbumReviewsResponse
import com.jetbrains.kmpapp.data.MuseumObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: MuseumRepository
) : ViewModel() {

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    // These properties MUST exist for DetailScreen to compile
    private val _totalRatings = MutableStateFlow(0)
    val totalRatings: StateFlow<Int> = _totalRatings

    private val _averageRating = MutableStateFlow(0.0)
    val averageRating: StateFlow<Double> = _averageRating

    private val _reviewState = MutableStateFlow<ReviewState>(ReviewState.Idle)
    val reviewState: StateFlow<ReviewState> = _reviewState

    private val _isLoadingReviews = MutableStateFlow(false)
    val isLoadingReviews: StateFlow<Boolean> = _isLoadingReviews

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
        println("[ViewModel] loadReviews called with ID: $albumId")

        // Check if we already have data
        if (_reviews.value.isNotEmpty()) {
            println("[ViewModel] Skipping load: Reviews already exist (${_reviews.value.size} items)")
            return
        }

        println("[ViewModel] Starting network request for album $albumId...")

        viewModelScope.launch {
            _isLoadingReviews.value = true
            println("[ViewModel] Set loading state to true")

            val result = repository.getReviewsForAlbum(albumId)

            result.fold(
                onSuccess = { response: AlbumReviewsResponse ->
                    println("[ViewModel] Success! Received ${response.reviews.size} reviews")
                    _reviews.value = response.reviews
                    _totalRatings.value = response.totalRatings
                    _averageRating.value = response.rating
                },
                onFailure = { error ->
                    println("[ViewModel] Error: ${error.message}")
                    error.printStackTrace()
                }
            )
            _isLoadingReviews.value = false
            println("[ViewModel] Set loading state to false")
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