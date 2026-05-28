package com.jetbrains.kmpapp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
val USERNAME_KEY = stringPreferencesKey("username")
val USER_ID_KEY = intPreferencesKey("user_id")

class SessionManager(private val dataStore: DataStore<Preferences>) {

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN_KEY] ?: false
    }

    val username: Flow<String> = dataStore.data.map { preferences ->
        preferences[USERNAME_KEY] ?: ""
    }

    val userId: Flow<Int> = dataStore.data.map { preferences ->
        preferences[USER_ID_KEY] ?: 0
    }

    suspend fun login(name: String, id: Int) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_KEY] = true
            preferences[USERNAME_KEY] = name
            preferences[USER_ID_KEY] = id
        }
    }

    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}