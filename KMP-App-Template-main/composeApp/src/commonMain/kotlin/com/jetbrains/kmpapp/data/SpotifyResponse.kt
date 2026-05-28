package com.jetbrains.kmpapp.data

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyLoginResponse(
    val success: Boolean,
    val user_id: Int,
    val username: String,
    val spotify_id: String? = null
)