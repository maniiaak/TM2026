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
data class SearchResult(
    val exists: Boolean,

    @SerialName("album_id")
    val albumId: Int? = null,

    @SerialName("spotify_id")
    val spotifyId: String? = null,

    val title: String,
    val artist: String,
    val coverImage: String? = null
)

@Serializable
data class SearchResponse(
    val success: Boolean,
    val results: List<SearchResult>,
    val error: String? = null
)