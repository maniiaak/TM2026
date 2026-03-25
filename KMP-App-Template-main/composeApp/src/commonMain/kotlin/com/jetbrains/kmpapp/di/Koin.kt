package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.data.InMemoryMuseumStorage
import com.jetbrains.kmpapp.data.KtorMuseumApi
import com.jetbrains.kmpapp.data.MuseumApi
import com.jetbrains.kmpapp.data.MuseumRepository
import com.jetbrains.kmpapp.data.MuseumStorage
import com.jetbrains.kmpapp.screens.detail.DetailViewModel
import com.jetbrains.kmpapp.screens.list.ListViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val dataModule = module {
    // 1. HttpClient with JSON Serialization
    single {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true // Helps with strict JSON servers
        }
        HttpClient {
            install(ContentNegotiation) {
                // Explicitly set JSON content type for better compatibility
                json(json, contentType = ContentType.Application.Json)
            }
        }
    }

    // 2. API Implementation
    single<MuseumApi> { KtorMuseumApi(get()) }

    // 3. Storage Implementation
    single<MuseumStorage> { InMemoryMuseumStorage() }

    // 4. Repository (Singleton to maintain state)
    single {
        MuseumRepository(get(), get()).apply {
            // Initialize data on startup
            initialize()
        }
    }
}

val viewModelModule = module {
    // Factory creates a new instance for each screen/view
    factoryOf(::ListViewModel)
    factoryOf(::DetailViewModel)
}

fun initKoin() {
    startKoin {
        modules(
            dataModule,
            viewModelModule,
        )
    }
}
