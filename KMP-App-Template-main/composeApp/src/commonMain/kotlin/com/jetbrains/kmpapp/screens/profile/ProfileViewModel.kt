package com.jetbrains.kmpapp.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.MuseumRepository
import com.jetbrains.kmpapp.data.SessionManager
import com.jetbrains.kmpapp.data.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: MuseumRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _userStats =
        MutableStateFlow<UserStats?>(null)

    val userStats: StateFlow<UserStats?> =
        _userStats

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {

            val userId = sessionManager.getUserId()

            println("PROFILE: userId = $userId")

            if (userId == null) return@launch

            repository.getUserStats(userId)
                .onSuccess {
                    println("PROFILE: success = $it")
                    _userStats.value = it
                }
                .onFailure {
                    println("PROFILE: error = ${it.message}")
                    it.printStackTrace()
                }
        }
    }
}