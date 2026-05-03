package com.jetbrains.kmpapp.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.utils.io.CancellationException
import kotlinx.serialization.json.Json

interface MuseumApi {
    suspend fun getData(): List<MuseumObject>
}

class KtorMuseumApi(private val client: HttpClient) : MuseumApi {
    companion object {
        private const val API_URL =
            "https://raw.githubusercontent.com/maniiaak/TM2026/main/KMP-App-Template-main/list.json"
    }

    override suspend fun getData(): List<MuseumObject> {
        return try {
            val response = client.get(API_URL)

            // 1. Check HTTP Status
            if (!response.status.isSuccess()) {
                println("❌ HTTP Error: ${response.status} - ${response.body<String>()}")
                return emptyList()
            }

            // 2. Print Raw Body (to see if it's actually JSON)
            val rawBody = response.body<String>()
            println("📄 Raw Response Length: ${rawBody.length}")
            if (rawBody.isEmpty()) {
                println("⚠️ Response body is empty!")
                return emptyList()
            }

            // 3. Try to parse
            val data = Json.decodeFromString<List<MuseumObject>>(rawBody)
            println("✅ Successfully parsed ${data.size} objects")
            return data

        } catch (e: Exception) {
            println("❌ Exception: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
}