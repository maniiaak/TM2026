package com.maniiaak.iluvmusic.data

import com.maniiaak.iluvmusic.config.ApiConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

interface MuseumApi {
    suspend fun getData(): List<MuseumObject>
    suspend fun submitReview(request: ReviewRequest): Result<ReviewResponse>
    suspend fun getUserReviews(userId: Int, page: Int, limit: Int = 10): Result<List<UserReview>>
    suspend fun getReviews(albumId: Int): Result<AlbumReviewsResponse>
    suspend fun searchAlbum(query: String): Result<SearchResponse>
    suspend fun importAlbum(query: String): Result<Int>
    suspend fun getUserStats(userId: Int, currentUserId: Int? = null): UserStats
    suspend fun getUserProfile(userId: Int, currentUserId: Int? = null): Result<UserStats>
    suspend fun updateProfileImage(userId: Int, imageUrl: String?): Result<String?>
    suspend fun followUser(userId: Int, currentUserId: Int): Result<Unit>
    suspend fun unfollowUser(userId: Int, currentUserId: Int): Result<Unit>
    suspend fun getHome(currentUserId: Int? = null): Result<HomeResponse>
    suspend fun getCategoryAlbums(category: String, page: Int, currentUserId: Int? = null): Result<CategoryResponse>
}

@Serializable
data class ReviewRequest(val rating: Float, val content: String, val user_id: Int, val album_id: Int)
@Serializable
data class ReviewResponse(val success: Boolean)
@Serializable
data class AlbumReviewsResponse(val reviews: List<Review>, val totalRatings: Int, val rating: Double)

class KtorMuseumApi(private val client: HttpClient) : MuseumApi {
    private val baseUrl = ApiConfig.BASE_URL + "/"

    override suspend fun getData(): List<MuseumObject> = client.get(baseUrl + "albums").body()

    override suspend fun submitReview(request: ReviewRequest): Result<ReviewResponse> = runCatching {
        val response = client.post(baseUrl + "reviews") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) error("Failed to submit review")
        response.body()
    }

    override suspend fun getReviews(albumId: Int): Result<AlbumReviewsResponse> = runCatching {
        val response = client.get(baseUrl + "albums/$albumId/reviews")
        if (!response.status.isSuccess()) error("Failed to fetch reviews")
        response.body()
    }

    override suspend fun searchAlbum(query: String): Result<SearchResponse> = runCatching {
        client.post(baseUrl + "spotify/search") {
            contentType(ContentType.Application.Json)
            setBody(SyncRequest(query))
        }.body()
    }

    override suspend fun importAlbum(query: String): Result<Int> = runCatching {
        val response = client.post(baseUrl + "spotify/import") {
            contentType(ContentType.Application.Json)
            setBody(ImportRequest(spotifyId = query))
        }.body<ImportResponse>()
        response.album_id ?: error("Album ID missing")
    }

    override suspend fun getUserStats(userId: Int, currentUserId: Int?): UserStats =
        client.get(baseUrl + "users/$userId/stats") {
            if (currentUserId != null) parameter("current_user_id", currentUserId)
        }.body()

    override suspend fun getUserProfile(userId: Int, currentUserId: Int?): Result<UserStats> = runCatching {
        client.get(baseUrl + "users/$userId/profile") {
            if (currentUserId != null) parameter("current_user_id", currentUserId)
        }.body()
    }

    override suspend fun updateProfileImage(userId: Int, imageUrl: String?): Result<String?> = runCatching {
        val response = client.put(baseUrl + "users/$userId/profile-image") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("profile_image_url" to (imageUrl ?: "")))
        }
        if (!response.status.isSuccess()) error("Failed to update profile picture")
        response.body<ProfileImageResponse>().profileImageUrl
    }

    override suspend fun getUserReviews(userId: Int, page: Int, limit: Int): Result<List<UserReview>> = runCatching {
        client.get(baseUrl + "users/$userId/reviews") {
            parameter("page", page); parameter("limit", limit)
        }.body()
    }

    override suspend fun followUser(userId: Int, currentUserId: Int): Result<Unit> = runCatching {
        val response = client.post(baseUrl + "users/$userId/follow") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("current_user_id" to currentUserId))
        }
        if (!response.status.isSuccess()) error("Failed to follow user")
    }

    override suspend fun unfollowUser(userId: Int, currentUserId: Int): Result<Unit> = runCatching {
        val response = client.post(baseUrl + "users/$userId/unfollow") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("current_user_id" to currentUserId))
        }
        if (!response.status.isSuccess()) error("Failed to unfollow user")
    }

    override suspend fun getHome(currentUserId: Int?): Result<HomeResponse> = runCatching {
        client.get(baseUrl + "home") {
            if (currentUserId != null) parameter("current_user_id", currentUserId)
        }.body()
    }

    override suspend fun getCategoryAlbums(category: String, page: Int, currentUserId: Int?): Result<CategoryResponse> = runCatching {
        val categoryPath = when (category) {
            "popular_this_week" -> "home/popular-this-week"
            "newly_reviewed_by_friends" -> "home/newly-reviewed-by-friends"
            "popular_with_friends" -> "home/popular-with-friends"
            else -> "home/$category"
        }
        client.get(baseUrl + categoryPath) {
            parameter("page", page); parameter("limit", 20)
            if (currentUserId != null) parameter("current_user_id", currentUserId)
        }.body()
    }
}

@Serializable
data class ProfileImageResponse(
    val success: Boolean,
    @SerialName("profile_image_url") val profileImageUrl: String? = null
)