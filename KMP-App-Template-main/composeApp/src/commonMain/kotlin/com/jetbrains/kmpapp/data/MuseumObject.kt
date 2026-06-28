package com.jetbrains.kmpapp.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Review(
    val rating: Float,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    val username: String,
    @SerialName("user_id")
    val userId: Int? = null
)

@Serializable
data class MuseumObject(
    val objectID: Int,
    val title: String,
    val artistDisplayName: String? = null,
    val objectDate: String? = null,
    val coverImage: String? = null,
    val type: String? = null,

    @SerialName("length")
    val length: String? = null,

    @SerialName("tracks")
    val tracks: Int? = null,

    val ratings: Double? = null,
    val num_of_ratings: Int? = null
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
    @SerialName("length") val length: String? = null,
    @SerialName("tracks") val tracks: Int? = null,
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

@Serializable
data class UserStats(
    @SerialName("id")
    val userId: Int,

    val username: String,

    @SerialName("review_count")
    val reviewCount: Int,

    val success: Boolean = true
)

@Serializable
data class UserReview(
    val id: Int,

    @SerialName("album_id")
    val albumId: Int,

    val title: String,

    @SerialName("artist_name")
    val artistName: String,

    @SerialName("cover_image_url")
    val coverImageUrl: String,

    val rating: Double,

    val content: String,

    @SerialName("created_at")
    val createdAt: String
)