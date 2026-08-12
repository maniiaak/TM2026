package com.maniiaak.iluvmusic.data

import com.maniiaak.iluvmusic.config.ApiConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseAuthResponse(
    val success: Boolean,
    val needs_profile: Boolean = false,
    val user_id: Int? = null,
    val username: String? = null,
    val handle: String? = null,
    val email: String? = null,
    val firebase_uid: String? = null,
    val error: String? = null
)

class AuthRepository(private val client: HttpClient) {
    private val baseUrl = "${ApiConfig.BASE_URL}/auth"

    suspend fun authenticateFirebase(
        idToken: String,
        username: String? = null,
        handle: String? = null
    ): Result<FirebaseAuthResponse> = try {
        val body = mutableMapOf<String, String>()
        if (!username.isNullOrBlank()) body["username"] = username
        if (!handle.isNullOrBlank()) body["handle"] = handle

        val response = client.post("$baseUrl/firebase") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $idToken")
            setBody(body)
        }

        val responseBody = response.body<FirebaseAuthResponse>()
        if (response.status.isSuccess()) {
            Result.success(responseBody)
        } else {
            Result.failure(Exception(responseBody.error ?: "Backend authentication failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
