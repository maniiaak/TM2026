package com.maniiaak.iluvmusic.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maniiaak.iluvmusic.data.MuseumObject
import com.maniiaak.iluvmusic.data.MuseumRepository
import com.maniiaak.iluvmusic.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ListViewModel(
    private val repository: MuseumRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _popularThisWeek = MutableStateFlow<List<MuseumObject>>(emptyList())
    val popularThisWeek: StateFlow<List<MuseumObject>> = _popularThisWeek

    private val _newlyReviewedByFriends = MutableStateFlow<List<MuseumObject>>(emptyList())
    val newlyReviewedByFriends: StateFlow<List<MuseumObject>> = _newlyReviewedByFriends

    private val _popularWithFriends = MutableStateFlow<List<MuseumObject>>(emptyList())
    val popularWithFriends: StateFlow<List<MuseumObject>> = _popularWithFriends

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val currentUserId = sessionManager.getUserId()
            val result = repository.getHome(currentUserId)
            result.onSuccess { home ->
                _popularThisWeek.value = home.popularThisWeek.take(5)
                _newlyReviewedByFriends.value = home.newlyReviewedByFriends.take(5)
                _popularWithFriends.value = home.popularWithFriends.take(5)
            }.onFailure {
                // fallback to fetching all objects if home endpoint fails
                repository.getObjects().collect { list ->
                    _popularThisWeek.value = list.take(5)
                }
            }
        }
    }
}