package com.maniiaak.iluvmusic.di

import io.ktor.client.HttpClient
import com.maniiaak.iluvmusic.auth.FirebaseAuthManager

expect fun createHttpClient(firebaseAuthManager: FirebaseAuthManager): HttpClient