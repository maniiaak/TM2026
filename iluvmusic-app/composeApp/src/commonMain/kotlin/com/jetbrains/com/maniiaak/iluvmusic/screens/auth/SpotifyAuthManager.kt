package com.maniiaak.iluvmusic.screens.auth

import kotlinx.coroutines.flow.StateFlow

interface SpotifyAuthManager {
    val authState: StateFlow<AuthState>
    fun initiateLogin()
    fun handleUrl(url: String)
    fun reset()
}

