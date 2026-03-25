package com.jetbrains.kmpapp.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.AlbumReview
import com.jetbrains.kmpapp.data.MuseumRepository
import com.jetbrains.kmpapp.utils.getCurrentTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DetailViewModel(private val museumRepository: MuseumRepository) : ViewModel() {

    fun getObject(objectId: Int): Flow<com.jetbrains.kmpapp.data.MuseumObject?> =
        museumRepository.getObjectById(objectId)

    fun submitReview(objectId: Int, rating: Int, reviewText: String) {
        viewModelScope.launch {
            val review = AlbumReview(
                objectId = objectId,
                rating = rating,
                reviewText = reviewText,
                timestamp = getCurrentTimestamp()
            )

            // Call repository, not API directly
            val success = museumRepository.submitReview(review)

            if (success) {
                // Optionally show success feedback here
                println("Review submitted successfully")
            } else {
                // Handle failure
                println("Failed to submit review")
            }
        }
    }
}