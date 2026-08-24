package com.maniiaak.iluvmusic.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.maniiaak.iluvmusic.auth.FirebaseAuthManager
import com.maniiaak.iluvmusic.data.configureAuthInterceptor

actual fun createHttpClient(firebaseAuthManager: FirebaseAuthManager): HttpClient {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    return HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                json,
                contentType = ContentType.Application.Json
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }

        engine {
            config {
                System.setProperty(
                    "java.net.preferIPv4Stack",
                    "true"
                )
            }
        }
    }
}