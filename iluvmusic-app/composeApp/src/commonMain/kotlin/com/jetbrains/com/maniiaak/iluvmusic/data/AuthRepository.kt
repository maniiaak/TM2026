package com.maniiaak.iluvmusic.data

import com.maniaak.iluvmusic.config.ApiConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AuthRepository(private val client: HttpClient) {

    private val baseUrl = "${ApiConfig.BASE_URL}/auth"

    suspend fun exchangeSpotifyCode(code: String): Result<SpotifyLoginResponse> {
        return try {
            val response = client.post("$baseUrl/spotify") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("code" to code))
            }

            if (response.status.isSuccess()) {
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