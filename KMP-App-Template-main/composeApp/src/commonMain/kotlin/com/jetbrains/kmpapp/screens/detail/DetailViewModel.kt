package com.jetbrains.kmpapp.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.MuseumObject
import com.jetbrains.kmpapp.data.MuseumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val museumRepository: MuseumRepository
) : ViewModel() {

    fun getObject(objectId: Int): Flow<MuseumObject?> =
        museumRepository.getObjectById(objectId)

    fun saveReview(rating: Float?, note: String, albumId: Int) {
        if (rating == null || note.isBlank()) return

        viewModelScope.launch {
            val result = museumRepository.submitReview(rating, note, albumId)
            result.onSuccess { response ->
                // Handle success (e.g., show a toast in the UI, or update local state)
                println("Review saved successfully: ${response.review_id}")
            }.onFailure { error ->
                // Handle error
                println("Failed to save review: ${error.message}")
            }
        }
    }
}