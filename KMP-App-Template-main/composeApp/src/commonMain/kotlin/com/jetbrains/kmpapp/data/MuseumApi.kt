package com.jetbrains.kmpapp.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

interface MuseumApi {
    suspend fun getData(): List<MuseumObject>
    suspend fun submitReview(request: ReviewRequest): Result<ReviewResponse>
    suspend fun getUserReviews(
        userId: Int,
        page: Int,
        limit: Int = 10
    ): Result<List<UserReview>>

    suspend fun getReviews(albumId: Int): Result<AlbumReviewsResponse>
    suspend fun searchAlbum(
        query: String
    ): Result<SearchResponse>

    suspend fun importAlbum(
        query: String
    ): Result<Int>

    suspend fun getUserStats(
        userId: Int,
        currentUserId: Int? = null
    ): UserStats

    suspend fun followUser(userId: Int, currentUserId: Int): Result<Unit>
    suspend fun unfollowUser(userId: Int, currentUserId: Int): Result<Unit>
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

    override suspend fun searchAlbum(
        query: String
    ): Result<SearchResponse> {

        return runCatching {
            client.post(baseUrl + "spotify/search") {
                contentType(ContentType.Application.Json)

                setBody(
                    SyncRequest(query)
                )
            }.body<SearchResponse>()
        }
    }

    override suspend fun importAlbum(
        query: String
    ): Result<Int> {

        return runCatching {

            val response = client.post(urlString = baseUrl + "spotify/import") {

                contentType(ContentType.Application.Json)

                setBody(
                    ImportRequest(spotifyId = query)
                )
            }.body<ImportResponse>()

            println("STATUS = ${response}")

            response.album_id
                ?: error("Album ID missing")
        }
    }

     override suspend fun getUserStats(
         userId: Int,
         currentUserId: Int?
     ): UserStats {

        return client.get(
            urlString = baseUrl + "users/$userId/stats"
        ) {
            if (currentUserId != null) {
                parameter("current_user_id", currentUserId)
            }
        }.body()
    }

    override suspend fun getUserReviews(
        userId: Int,
        page: Int,
        limit: Int
    ): Result<List<UserReview>> = runCatching {

        client.get(urlString = baseUrl + "users/$userId/reviews") {
            parameter("page", page)
            parameter("limit", limit)
        }.body()
    }

    override suspend fun followUser(userId: Int, currentUserId: Int): Result<Unit> = runCatching {
        client.post(baseUrl + "users/$userId/follow") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("current_user_id" to currentUserId))
        }
    }

    override suspend fun unfollowUser(userId: Int, currentUserId: Int): Result<Unit> = runCatching {
        client.post(baseUrl + "users/$userId/unfollow") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("current_user_id" to currentUserId))
        }
    }
}