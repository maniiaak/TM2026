package com.jetbrains.kmpapp.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.utils.io.CancellationException

interface MuseumApi {
    suspend fun getData(): List<MuseumObject>
}

class KtorMuseumApi(private val client: HttpClient) : MuseumApi {
    companion object {
***REMOVED***
            "http://192.168.1.124:5000/api/music"
    }

    override suspend fun getData(): List<MuseumObject> {
        return try {
***REMOVED***
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()

            emptyList()
        }
    }
}
