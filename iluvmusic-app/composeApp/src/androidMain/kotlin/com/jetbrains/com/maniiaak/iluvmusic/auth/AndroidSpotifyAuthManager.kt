package com.jetbrains.kmpapp.auth

import android.webkit.WebView
import android.content.Context
import android.net.Uri
import com.jetbrains.kmpapp.screens.auth.AuthState
import com.jetbrains.kmpapp.screens.auth.SpotifyAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidSpotifyAuthManager(
    private val context: Context,
    private val clientId: String,
    private val redirectUri: String = "com.jetbrains.kmpapp://callback"
) : SpotifyAuthManager {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    override val authState: StateFlow<AuthState> = _state

    private var webView: WebView? = null

    override fun initiateLogin() {
        _state.value = AuthState.Loading
    }

    override fun handleUrl(url: String) {
        if (url.startsWith(redirectUri)) {
            val uri = Uri.parse(url)
            val code = uri.getQueryParameter("code")

            if (code != null) {
                _state.value = AuthState.Success(code)
            } else {
                _state.value = AuthState.Error("No authorization code found in URL")
            }
        }
    }

    override fun reset() {
        _state.value = AuthState.Idle
        webView?.destroy()
        webView = null
    }

    fun getAuthUrl(): String {
        val scopes = "user-read-email user-read-private"
        return "https://accounts.spotify.com/authorize?" +
                "client_id=$clientId" +
                "&response_type=code" +
                "&redirect_uri=${Uri.encode(redirectUri)}" +
                "&scope=${Uri.encode(scopes)}" +
                "&show_dialog=true"
    }
}