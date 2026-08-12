package com.maniiaak.iluvmusic.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maniiaak.iluvmusic.data.MuseumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: MuseumRepository
) : ViewModel() {

    private val _state =
        MutableStateFlow<SearchState>(SearchState.Idle)

    val state: StateFlow<SearchState> = _state

    fun searchAlbum(query: String) {

        viewModelScope.launch {

            _state.value = SearchState.Loading

            repository.searchAlbum(query)
                .onSuccess { response ->

                    if (!response.success) {

                        _state.value = SearchState.Error(
                            response.error ?: "Album not found"
                        )
                        return@onSuccess
                    }

                    _state.value = SearchState.Success(
                        albums = response.results
                    )
                }
                .onFailure { throwable ->

                    _state.value = SearchState.Error(
                        throwable.message ?: "Unknown error"
                    )
                }
        }
    }

    fun importAlbum(
        query: String,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {

            repository.importAlbum(query)
                .onSuccess { albumId ->
                    onSuccess(albumId)
                }
                .onFailure {
                    onError(
                        it.message ?: "Failed to import album"
                    )
                }
        }
    }
}