// File: commonMain/kotlin/com/jetbrains/kmpapp/data/AlbumReview.kt
package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.utils.getCurrentTimestamp
import kotlinx.serialization.Serializable

/**
 * Represents a user review for an album.
 * Used for sending data to the server.
 */
@Serializable
data class AlbumReview(
    val objectId: Int,
    val rating: Int,           // Expected range: 1 to 5
    val reviewText: String,
    // We use a helper function to get the timestamp to ensure cross-platform compatibility
    val timestamp: Long = getCurrentTimestamp()
)
// File: commonMain/kotlin/com/jetbrains/kmpapp/data/AlbumReview.kt (add to the same file)

/**
 * Validates that the review meets basic criteria.
 */
fun AlbumReview.isValid(): Boolean {
    return rating in 1..5 &&
            reviewText.isNotBlank() &&
            reviewText.length <= 500 // Adjust max length as needed
}