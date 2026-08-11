package com.jetbrains.kmpapp.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SpotifyLoginResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("user_id") val user_id: Int,
    @SerialName("username") val username: String,
    @SerialName("spotify_id") val spotify_id: String? = null
)