package com.maniiaak.iluvmusic.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyLoginResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("user_id") val user_id: Int,
    @SerialName("username") val username: String,
    @SerialName("spotify_id") val spotify_id: String? = null
)