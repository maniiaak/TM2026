package com.jetbrains.kmpapp.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class AuthRepository {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val baseUrl = "http://192.168.1.139:5000/api/auth"

    suspend fun exchangeSpotifyCode(code: String): Result<SpotifyLoginResponse> {
        return try {
            val response = client.post("$baseUrl/spotify") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("code" to code))
            }

            if (response.status.isSuccess()) {
                // This line MUST work now that the class is correct
                val responseBody = response.body<SpotifyLoginResponse>()
                Result.success(responseBody)
            } else {
                val errorBody = response.body<String>()
                Result.failure(Exception("Backend error: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}