package com.maniiaak.iluvmusic.di

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
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import com.maniiaak.iluvmusic.data.PreferencesStorage
import com.maniiaak.iluvmusic.data.createPreferencesStorage

val dataModule = module {
    single {
        createHttpClient()
    }

    single<MuseumApi> {
        KtorMuseumApi(get())
    }

    single<AuthRepository> {
        AuthRepository(get())
    }

    single<MuseumStorage> {
        InMemoryMuseumStorage()
    }

    single<MuseumRepository> {
        MuseumRepository(get(), get()).apply {
            initialize()
        }
    }

    single {
        SessionManager(get())
    }
}

val viewModelModule = module {

    viewModel {
        ListViewModel(get(), get())
    }

    viewModel {
        DetailViewModel(get())
    }

    viewModel {
        CategoryDetailViewModel(get(), get())
    }

    viewModel {
        AuthViewModel(get(), get(), get())
    }

    viewModel { (initialUserId: Int?) ->
        ProfileViewModel(get(), get(), initialUserId)
    }

    viewModel {
        SearchViewModel(get())
    }
}

//fun initKoin() {
//    startKoin {
//        modules(
//            dataModule,
//            viewModelModule
//        )
//    }
//}
