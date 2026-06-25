package com.jetbrains.kmpapp.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MuseumRepository(
    private val museumApi: MuseumApi,
    private val museumStorage: MuseumStorage,
) {
    private val scope = CoroutineScope(SupervisorJob())

    fun initialize() {
        scope.launch {
            refresh()
        }
    }

    suspend fun refresh() {
        museumStorage.saveObjects(museumApi.getData())
    }

    fun getObjects(): Flow<List<MuseumObject>> = museumStorage.getObjects()

    fun getObjectById(objectId: Int): Flow<MuseumObject?> = museumStorage.getObjectById(objectId)

    suspend fun submitReview(rating: Float, content: String, albumId: Int, userId: Int): Result<ReviewResponse> {
        val request = ReviewRequest(
            rating = rating,
            content = content,
            user_id = userId,
            album_id = albumId
        )
        return museumApi.submitReview(request)
    }

    suspend fun getReviewsForAlbum(albumId: Int): Result<AlbumReviewsResponse> {
        println("[Repository] Calling API for album $albumId")
        val result = museumApi.getReviews(albumId)
        println("[Repository] API call returned: ${result.isFailure}")
        return result
    }


    suspend fun searchAlbum(
        query: String
    ): Result<SearchResponse> =
        museumApi.searchAlbum(query)


    suspend fun importAlbum(
        query: String
    ): Result<Int> {
        val result = museumApi.importAlbum(query)

        if (result.isSuccess) {
            refresh()
        }

        return result
    }

    suspend fun getUserStats(
        userId: Int
    ): Result<UserStats> =
        runCatching {
            museumApi.getUserStats(userId)
        }

    suspend fun getUserReviews(
        userId: Int,
        page: Int,
        limit: Int = 10
    ): Result<List<UserReview>> {
        return museumApi.getUserReviews(
            userId = userId,
            page = page,
            limit = limit
        )
    }
}