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
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val USER_EMAIL_KEY = stringPreferencesKey("user_email")

    /**
     * Saves the access and refresh tokens to DataStore.
     * @param context The application context.
     * @param accessToken The access token to save.
     * @param refreshToken The refresh token to save.
     */
    suspend fun saveAuthTokens(context: Context, accessToken: String?, refreshToken: String?) {
        context.dataStore.edit { preferences ->
            if (accessToken != null) {
                preferences[ACCESS_TOKEN_KEY] = accessToken
            } else {
                preferences.remove(ACCESS_TOKEN_KEY) // Remove if null to clear
            }
            if (refreshToken != null) {
                preferences[REFRESH_TOKEN_KEY] = refreshToken
            } else {
                preferences.remove(REFRESH_TOKEN_KEY) // Remove if null to clear
            }
        }
    }

    /**
     * Saves the user's email to DataStore.
     * @param context The application context.
     * @param email The user's email to save.
     */
    suspend fun saveUserEmail(context: Context, email: String?) {
        context.dataStore.edit { preferences ->
            if (email != null) {
                preferences[USER_EMAIL_KEY] = email
            } else {
                preferences.remove(USER_EMAIL_KEY) // Remove if null to clear
            }
        }
    }

    suspend fun saveAccessToken(context: Context, accessToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
        }
    }

    /**
     * Retrieves the access token as a Flow.
     * @param context The application context.
     * @return A Flow emitting the access token string, or null if not found.
     */
    fun getAccessTokenFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }
    }

    /**
     * Retrieves the refresh token as a Flow.
     * @param context The application context.
     * @return A Flow emitting the refresh token string, or null if not found.
     */
    fun getRefreshTokenFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }
    }

    /**
     * Retrieves the user's email as a Flow.
     * @param context The application context.
     * @return A Flow emitting the user's email string, or null if not found.
     */
    fun getUserEmailFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_EMAIL_KEY]
        }
    }

    /**
     * Checks if the user is currently logged in (i.e., has an access token).
     * This is a suspend function that reads the value once.
     * @param context The application context.
     * @return True if an access token exists, false otherwise.
     */
    suspend fun isLoggedIn(context: Context): Boolean {
        return getAccessTokenFlow(context).first() != null
    }

    /**
     * Provides a Flow that emits true if the user is logged in, false otherwise.
     * Useful for observing login status reactively in UI.
     * @param context The application context.
     * @return A Flow emitting the login status.
     */
    fun isLoggedInFlow(context: Context): Flow<Boolean> {
        return getAccessTokenFlow(context).map { it != null }
    }

    /**
     * Clears all session data (tokens, email) from DataStore.
     * @param context The application context.
     */
    suspend fun clearSession(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(USER_EMAIL_KEY)
            // Or preferences.clear() to remove all keys in this DataStore
        }
    }
}