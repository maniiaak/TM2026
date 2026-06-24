package com.jetbrains.kmpapp

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.jetbrains.kmpapp.data.SessionManager
import com.jetbrains.kmpapp.data.MuseumRepository
import com.jetbrains.kmpapp.data.MuseumStorage
import com.jetbrains.kmpapp.data.MuseumApi
import com.jetbrains.kmpapp.data.KtorMuseumApi
import com.jetbrains.kmpapp.data.InMemoryMuseumStorage
import com.jetbrains.kmpapp.data.AuthRepository // ADD THIS IMPORT
import com.jetbrains.kmpapp.screens.auth.AuthViewModel
import com.jetbrains.kmpapp.screens.detail.DetailViewModel
import com.jetbrains.kmpapp.screens.list.ListViewModel
import com.jetbrains.kmpapp.screens.search.SearchViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.core.module.dsl.factoryOf

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MyApp)
            modules(
                module {
                    // 1. DataStore
                    single { this@MyApp.dataStore }

                    // 2. SessionManager
                    single { SessionManager(get()) }

                    // 3. HttpClient
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

                    // 4. Museum API & Storage
                    single<MuseumApi> { KtorMuseumApi(get()) }
                    single<MuseumStorage> { InMemoryMuseumStorage() }
                    single {
                        MuseumRepository(get(), get()).apply { initialize() }
                    }

                    // 5. Auth Repository
                    single { AuthRepository() }

                    // 6. ViewModels
                    factoryOf(::ListViewModel)
                    factoryOf(::DetailViewModel)
                    factoryOf(::AuthViewModel)
                    factoryOf(::SearchViewModel)
                }
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }
    }
}