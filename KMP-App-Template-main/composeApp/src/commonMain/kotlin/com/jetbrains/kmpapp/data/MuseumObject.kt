package com.jetbrains.kmpapp.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Review(
    val rating: Float,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    val username: String
)

@Serializable
data class MuseumObject(
    val objectID: Int,
    val title: String,
    val artistDisplayName: String?,
    val coverImage: String?,
    val objectDate: String?,
    val type: String?,
    val length: String?,
    val tracks: String?,
    val totalRatings: Int?,
    val rating: Double?
)