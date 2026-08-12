package com.maniiaak.iluvmusic.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Android-specific Koin initializer. Creates a Preferences DataStore and
 * registers it so common code can resolve `DataStore<Preferences>`.
 */
fun initKoin(application: Application) {
    // Create Android Preferences DataStore backed by a file named "settings"
    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { application.preferencesDataStoreFile("settings") }
    )

    val androidModule = module {
        single<DataStore<Preferences>> { dataStore }
    }

    // Start Koin and include the Android module so DataStore is available
    startKoin {
        modules(dataModule, viewModelModule, androidModule)
    }
}

