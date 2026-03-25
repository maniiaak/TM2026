package com.jetbrains.kmpapp.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.CancellationException

interface MuseumApi {
    suspend fun getData(): List<MuseumObject>
    suspend fun submitReview(review: AlbumReview): Boolean
}

class KtorMuseumApi(private val client: HttpClient) : MuseumApi {
    companion object {
        private const val API_URL = "http://192.168.1.124:5000/api/music"
        private const val REVIEW_URL = "http://192.168.1.124:5000/api/review"
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

    override suspend fun submitReview(review: AlbumReview): Boolean {
        return try {
            val response = client.post(REVIEW_URL) {
                contentType(ContentType.Application.Json)
                setBody(review)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
