package com.jetbrains.kmpapp.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.MuseumObject
import com.jetbrains.kmpapp.data.MuseumRepository
import com.jetbrains.kmpapp.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CategoryDetailViewModel(
    private val repository: MuseumRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _albums = MutableStateFlow<List<MuseumObject>>(emptyList())
    val albums: StateFlow<List<MuseumObject>> = _albums

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private var currentPage = 1
    private var endReached = false

    fun loadCategory(category: String) {
        if (_albums.value.isNotEmpty()) return  // Already loaded

        viewModelScope.launch {
            _isLoading.value = true
            currentPage = 1
            endReached = false

            val currentUserId = sessionManager.getUserId()

            when (category) {
                "popular_this_week" -> {
                    repository.getCategoryAlbums("popular_this_week", currentPage, currentUserId)
                        .onSuccess { response ->
                            _albums.value = response.albums
                            if (response.albums.size < response.limit) {
                                endReached = true
                            }
                            currentPage = 2
                        }
                        .onFailure {
                            // Fallback
                        }
                }
                "newly_reviewed_by_friends" -> {
                    repository.getCategoryAlbums("newly_reviewed_by_friends", currentPage, currentUserId)
                        .onSuccess { response ->
                            _albums.value = response.albums
                            if (response.albums.size < response.limit) {
                                endReached = true
                            }
                            currentPage = 2
                        }
                        .onFailure {
                            // Fallback
                        }
                }
                "popular_with_friends" -> {
                    repository.getCategoryAlbums("popular_with_friends", currentPage, currentUserId)
                        .onSuccess { response ->
                            _albums.value = response.albums
                            if (response.albums.size < response.limit) {
                                endReached = true
                            }
                            currentPage = 2
                        }
                        .onFailure {
                            // Fallback
                        }
                }
            }

            _isLoading.value = false
        }
    }

    fun loadMore(category: String) {
        if (endReached || _isLoadingMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true

            val currentUserId = sessionManager.getUserId()

            repository.getCategoryAlbums(category, currentPage, currentUserId)
                .onSuccess { response ->
                    _albums.value += response.albums
                    if (response.albums.size < response.limit) {
                        endReached = true
                    } else {
                        currentPage++
                    }
                }
                .onFailure {
                    // Handle error
                }

            _isLoadingMore.value = false
        }
    }
}



