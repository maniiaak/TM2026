package com.maniiaak.iluvmusic.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.maniiaak.iluvmusic.data.AuthRepository
import com.maniiaak.iluvmusic.data.InMemoryMuseumStorage
import com.maniiaak.iluvmusic.data.KtorMuseumApi
import com.maniiaak.iluvmusic.data.MuseumApi
import com.maniiaak.iluvmusic.data.MuseumRepository
import com.maniiaak.iluvmusic.data.MuseumStorage
import com.maniiaak.iluvmusic.data.SessionManager
import com.maniiaak.iluvmusic.screens.auth.AuthViewModel
import com.maniiaak.iluvmusic.screens.detail.DetailViewModel
import com.maniiaak.iluvmusic.screens.list.CategoryDetailViewModel
import com.maniiaak.iluvmusic.screens.list.ListViewModel
import com.maniiaak.iluvmusic.screens.profile.ProfileViewModel
import com.maniiaak.iluvmusic.screens.search.SearchViewModel
import io.ktor.client.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json, contentType = ContentType.Application.Json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 30000
                socketTimeoutMillis = 30000
            }
            engine {
                config {
                    // Disable IPv6 to avoid timeout issues with Cloudflare tunnel
                    System.setProperty("java.net.preferIPv4Stack", "true")
                }
            }
        }
    }

    single<MuseumApi> { KtorMuseumApi(get()) }
    single<MuseumStorage> { InMemoryMuseumStorage() }

    // AuthRepository is used by AuthViewModel; register it in the common data module
    single { AuthRepository(get()) }

    single {
        MuseumRepository(get(), get()).apply {
            initialize()
        }
    }

    // We expect the DataStore to be provided by the Android layer
    single<DataStore<Preferences>> { get() }
    single {
        SessionManager(get())
    }
}

val viewModelModule = module {
    viewModel { ListViewModel(get(), get()) }
    viewModel { DetailViewModel(get()) }
    viewModel { CategoryDetailViewModel(get(), get()) }
    viewModel {
        AuthViewModel(
            repository = get(),
            sessionManager = get()
        )
    }

    viewModel { (initialUserId: Int?) ->
        println("Registering ProfileViewModel for initialUserId=$initialUserId")
        ProfileViewModel(
            repository = get(),
            sessionManager = get(),
            initialUserId = initialUserId
        )
    }
    viewModel { SearchViewModel(get()) }
}

fun initKoin() {
    startKoin {
        modules(dataModule, viewModelModule)
    }
}

