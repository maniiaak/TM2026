package com.maniiaak.iluvmusic.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    return HttpClient(Js) {
        install(ContentNegotiation) {
            json(
                json,
                contentType = ContentType.Application.Json
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
        }
    }
}