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

@Serializable
data class SpotifyTrackItem(
    val id: String,
    val name: String,
    @SerialName("album") val albumData: AlbumData
)

@Serializable
data class AlbumData(
    val name: String,
    @SerialName("release_date") val releaseDate: String? = null,
    val images: List<ImageData>? = null
)

@Serializable
data class ImageData(
    val url: String,
    val height: Int?,
    val width: Int?
)

@Serializable
data class SpotifySearchResponse(
    val tracks: TracksContainer?
)

@Serializable
data class TracksContainer(
    val items: List<SpotifyTrackItem>
)

@Serializable
data class SyncRequest(
    val query: String
)

@Serializable
data class SyncResponse(
    val success: Boolean,
    val album_id: Int? = null,
    val title: String? = null,
    val artist: String? = null,
    val coverImage: String? = null,
    val source: String? = null,
    val error: String? = null
)