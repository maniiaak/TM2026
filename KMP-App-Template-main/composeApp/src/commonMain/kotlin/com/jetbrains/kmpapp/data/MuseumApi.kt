package com.jetbrains.kmpapp.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// 1. Request Data Class
@Serializable
data class ReviewRequest(
    val rating: Float,
    val content: String,
    val user_id: Int,
    val album_id: Int
)

// 2. Response Data Class
@Serializable
data class ReviewResponse(
    val success: Boolean,
    val review_id: Int,
    val message: String
)

interface MuseumApi {
    suspend fun getData(): List<MuseumObject>
    suspend fun submitReview(review: ReviewRequest): Result<ReviewResponse>
}

class KtorMuseumApi(private val client: HttpClient) : MuseumApi {
    companion object {
        private const val API_URL =
            "http://192.168.1.139:5000/api/albums"
        private const val REVIEW_ENDPOINT =
            "http://192.168.1.139:5000/api/reviews"
    }

    override suspend fun getData(): List<MuseumObject> {
        return try {
            client.get(API_URL).body()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun submitReview(review: ReviewRequest): Result<ReviewResponse> {
        return try {
            val response = client.post(REVIEW_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(review)
            }

            // Check if the request was successful (2xx status)
            if (response.status.value in 200..299) {
                val result = response.body<ReviewResponse>()
                Result.success(result)
            } else {
                // If server returns an error (e.g., 400, 500), try to read the error message
                val errorMsg = try {
                    response.body<String>()
                } catch (e: Exception) {
                    "Unknown error"
                }
                Result.failure(Exception("Server error (${response.status.value}): $errorMsg"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}