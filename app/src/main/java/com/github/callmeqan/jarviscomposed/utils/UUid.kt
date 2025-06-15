// utils/UUid.kt
package com.github.callmeqan.jarviscomposed.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.collections.remove

// Create a DataStore instance
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserSessionManager {

    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val USER_EMAIL_KEY = stringPreferencesKey("user_email") // Example: store email

    suspend fun saveAuthTokens(context: Context, accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun saveUserEmail(context: Context, email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL_KEY] = email
        }
    }

    fun getAccessToken(context: Context) = context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }

    fun getRefreshToken(context: Context) = context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }

    fun getUserEmail(context: Context) = context.dataStore.data.map { it[USER_EMAIL_KEY] }


    suspend fun isLoggedIn(context: Context): Boolean {
        return getAccessToken(context).first() != null
    }

    suspend fun clearSession(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(USER_EMAIL_KEY)
        }
    }
}
