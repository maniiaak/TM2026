package com.maniiaak.iluvmusic.di

import com.maniiaak.iluvmusic.data.PreferencesStorage
import com.maniiaak.iluvmusic.data.createPreferencesStorage
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

fun initKoin() {
    val wasmModule = module {
        single<PreferencesStorage> { createPreferencesStorage() }
    }

    startKoin {
        modules(dataModule, viewModelModule, wasmModule)
    }
}