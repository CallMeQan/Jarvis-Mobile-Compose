package com.github.callmeqan.jarviscomposed.utils // Or your actual package

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs_session") // Ensure unique name if you have other datastores

object UUid {

    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    // Add other keys like REFRESH_TOKEN_KEY, USER_EMAIL_KEY as needed

    suspend fun saveAccessToken(context: Context, accessToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
        }
    }

    fun getAccessToken(context: Context): Flow<String?> =
        context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }

    fun isLoggedInFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY] != null
        }

    suspend fun isLoggedIn(context: Context): Boolean {
        return getAccessToken(context).first() != null
    }

    suspend fun clearSession(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            // Remove other keys
        }
    }
}