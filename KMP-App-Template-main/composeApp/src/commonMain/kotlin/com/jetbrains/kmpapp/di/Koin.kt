package com.jetbrains.kmpapp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jetbrains.kmpapp.data.InMemoryMuseumStorage
import com.jetbrains.kmpapp.data.KtorMuseumApi
import com.jetbrains.kmpapp.data.MuseumApi
import com.jetbrains.kmpapp.data.MuseumRepository
import com.jetbrains.kmpapp.data.MuseumStorage
import com.jetbrains.kmpapp.data.AuthRepository
import com.jetbrains.kmpapp.data.SessionManager
import com.jetbrains.kmpapp.screens.auth.AuthViewModel
import com.jetbrains.kmpapp.screens.detail.DetailViewModel
import com.jetbrains.kmpapp.screens.list.ListViewModel
import com.jetbrains.kmpapp.screens.list.CategoryDetailViewModel
import com.jetbrains.kmpapp.screens.profile.ProfileViewModel
import com.jetbrains.kmpapp.screens.search.SearchViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
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
        HttpClient {
            install(ContentNegotiation) {
                json(json, contentType = ContentType.Application.Json)
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

