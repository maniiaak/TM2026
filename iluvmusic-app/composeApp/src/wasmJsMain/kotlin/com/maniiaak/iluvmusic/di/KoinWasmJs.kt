package com.maniiaak.iluvmusic.di

import com.maniiaak.iluvmusic.auth.FirebaseAuthManager
import com.maniiaak.iluvmusic.auth.WasmFirebaseAuthManager
import com.maniiaak.iluvmusic.auth.createFirebaseAuthManager
import com.maniiaak.iluvmusic.data.PreferencesStorage
import com.maniiaak.iluvmusic.data.createPreferencesStorage
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

fun initKoin() {
    // Module with FirebaseAuthManager - must be loaded before dataModule
    val authModule = module {
        single<FirebaseAuthManager> { createFirebaseAuthManager() }
    }

    val wasmModule = module {
        single<PreferencesStorage> { createPreferencesStorage() }
    }

    startKoin {
        modules(authModule, dataModule, viewModelModule, wasmModule)
    }
}