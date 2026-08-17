package com.maniiaak.iluvmusic.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.maniiaak.iluvmusic.auth.AndroidFirebaseAuthManager
import com.maniiaak.iluvmusic.data.AndroidPreferencesStorage
import com.maniiaak.iluvmusic.data.PreferencesStorage
import com.maniiaak.iluvmusic.auth.FirebaseAuthManager
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin(application: Application) {
    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { application.preferencesDataStoreFile("settings") }
    )

    val androidModule = module {
        single<DataStore<Preferences>> { dataStore }
        single<FirebaseAuthManager> { AndroidFirebaseAuthManager() }

        // Android-specific override for PreferencesStorage
        single<PreferencesStorage> {
            AndroidPreferencesStorage(get())
        }
    }

    startKoin {
        modules(dataModule, viewModelModule, androidModule)
    }
}