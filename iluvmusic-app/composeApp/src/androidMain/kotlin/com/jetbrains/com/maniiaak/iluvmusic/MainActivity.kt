package com.maniiaak.iluvmusic

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
import com.maniaak.iluvmusic.data.SessionManager
import com.maniaak.iluvmusic.data.MuseumRepository
import com.maniaak.iluvmusic.data.MuseumStorage
import com.maniaak.iluvmusic.data.MuseumApi
import com.maniaak.iluvmusic.data.KtorMuseumApi
import com.maniaak.iluvmusic.data.InMemoryMuseumStorage
import com.maniaak.iluvmusic.data.AuthRepository // ADD THIS IMPORT
import com.maniaak.iluvmusic.screens.auth.AuthViewModel
import com.maniaak.iluvmusic.screens.detail.DetailViewModel
import com.maniaak.iluvmusic.screens.list.ListViewModel
import com.maniaak.iluvmusic.screens.search.SearchViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.core.module.dsl.factoryOf
import com.maniaak.iluvmusic.screens.profile.ProfileViewModel

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

                    // 6. ViewModels
                    factoryOf(::ListViewModel)
                    factoryOf(::DetailViewModel)
                    factoryOf(::AuthViewModel)
                    factoryOf(::SearchViewModel)
                    factoryOf(::ProfileViewModel)
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