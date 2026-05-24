package com.jetbrains.kmpapp.data

import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val userID: Int,
    val username: String,
    val content: String,
    val createdAt: String,
    val rating: Double
)

@Serializable
data class MuseumObject(
    val objectID: Int,
    val title: String,
    val artistDisplayName: String,
    val objectDate: String,
    val type: String,
    val length: String,
    val tracks: String,
    val coverImage: String?,
    val objectURL: String?,
    val rating: Double, // Average rating
    val totalRatings: Int, // Total count
    val reviews: List<Review>? // The list of reviews
)
