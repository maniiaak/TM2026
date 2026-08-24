package com.maniiaak.iluvmusic.screens.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maniiaak.iluvmusic.data.MuseumRepository
import com.maniiaak.iluvmusic.data.SessionManager
import com.maniiaak.iluvmusic.data.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel (
    private val sessionManager: SessionManager,
    private val repository: MuseumRepository,
    private val initialUserId: Int? = null
) : ViewModel() {
    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats
    private var isLoading = false
    private var viewingUserId: Int? = null

    private var currentSessionUserId: Int? = null

//    val sceneRoot = someLayoutElement as ViewGroup
//    val viewHierarchy = someOtherLayoutElement as ViewGroup
//    val scene: Scene = Scene(sceneRoot, viewHierarchy)

    init {
        viewModelScope.launch { currentSessionUserId = sessionManager.getUserId() }
        loadUser(initialUserId)
    }

    private fun loadUser(userIdParam: Int? = null) {
        println("Loading user $userIdParam")
        isLoading = true
        viewModelScope.launch {
            try {
                val userId = userIdParam ?: sessionManager.getUserId() ?: return@launch
                viewingUserId = userId
                repository.getUserProfile(userId, currentSessionUserId ?: sessionManager.getUserId())
                    .onSuccess { stats ->
                        _userStats.value = stats
                        println(stats)
                    }
                    .onFailure { println("ProfileViewModel: getUserProfile FAILED for userId=$userId: ${it.message}") }
            } finally {
                isLoading = false
            }
        }
    }

    fun updateProfileImage(imageUrl: String?) {
        val userId = viewingUserId ?: return
        viewModelScope.launch {
            repository.updateProfileImage(userId, imageUrl)
                .onSuccess { savedUrl ->
                    _userStats.value = _userStats.value?.copy(profileImageUrl = savedUrl)
                }
                .onFailure { println("Profile image update failed: ${it.message}") }
        }
    }


}