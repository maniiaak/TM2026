package com.jetbrains.kmpapp.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.MuseumRepository
import com.jetbrains.kmpapp.data.MuseumObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ListViewModel(
    private val repository: MuseumRepository
) : ViewModel() {

    private val _objects = MutableStateFlow<List<MuseumObject>>(emptyList())
    val objects: StateFlow<List<MuseumObject>> = _objects

    init {
        loadObjects()
    }

    private fun loadObjects() {
        viewModelScope.launch {
            repository.refresh()
            repository.getObjects().collect { list ->
                _objects.value = list
            }
        }
    }
}