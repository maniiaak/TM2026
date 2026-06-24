package com.jetbrains.kmpapp.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

interface MuseumApi {
    suspend fun getData(): List<MuseumObject>
    suspend fun submitReview(request: ReviewRequest): Result<ReviewResponse>
    suspend fun getReviews(albumId: Int): Result<AlbumReviewsResponse>
}

@Serializable
data class ReviewRequest(
    val rating: Float,
    val content: String,
    val user_id: Int,
    val album_id: Int
)

@Serializable
data class ReviewResponse(
    val success: Boolean
)
@Serializable
data class AlbumReviewsResponse(
    val reviews: List<Review>,
    val totalRatings: Int,
    val rating: Double
)
class KtorMuseumApi(private val client: HttpClient) : MuseumApi {
    private val baseUrl = "http://192.168.1.139:5000/api/"

    override suspend fun getData(): List<MuseumObject> {
        return client.get(baseUrl + "albums").body()
    }

    override suspend fun submitReview(request: ReviewRequest): Result<ReviewResponse> {
        return try {
            val response = client.post(baseUrl + "reviews") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to submit review"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReviews(albumId: Int): Result<AlbumReviewsResponse> {
        return try {
            val response = client.get(baseUrl + "albums/$albumId/reviews")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch reviews"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}