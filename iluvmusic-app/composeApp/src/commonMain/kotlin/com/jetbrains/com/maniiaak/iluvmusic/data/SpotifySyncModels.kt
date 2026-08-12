package com.maniiaak.iluvmusic.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(
    val query: String
)

@Serializable
data class SyncResponse(

    val success: Boolean = false,

    @SerialName("album_id")
    val albumId: Int? = null,

    val title: String? = null,

    val artist: String? = null,

    val coverImage: String? = null,

    val source: String? = null,

    val error: String? = null,
    @SerialName("spotify_id")
    val spotifyId: String? = null
)

@Serializable
data class ImportRequest(
    @SerialName("spotify_id")
    val spotifyId: String
)

@Serializable
data class ImportResponse(
    val success: Boolean,
    val album_id: Int?
)