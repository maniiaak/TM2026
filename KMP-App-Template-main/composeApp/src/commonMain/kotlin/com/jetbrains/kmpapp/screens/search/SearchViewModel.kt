package com.jetbrains.kmpapp.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.MuseumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: MuseumRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state

    fun searchAndSync(query: String) {
        viewModelScope.launch {
            _state.value = SearchState.Loading
            try {
                val result = repository.findAndCreateAlbum(query)

                result.fold(
                    onSuccess = { body ->
                        if (body.album_id != null) {
                            _state.value = SearchState.Success(
                                albumId = body.album_id,
                                albumTitle = body.title ?: "Unknown",
                                artistName = body.artist ?: "Unknown",
                                coverImage = body.coverImage
                            )
                        } else {
                            _state.value = SearchState.NotFound
                        }
                    },
                    onFailure = { error ->
                        _state.value = SearchState.Error(
                            error.message ?: "Unknown error"
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = SearchState.Error(e.message ?: "Exception occurred")
            }
        }
    }
}