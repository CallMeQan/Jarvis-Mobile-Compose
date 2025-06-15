// viewmodels/LoginViewModel.kt
package com.github.callmeqan.jarviscomposed.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.callmeqan.jarviscomposed.data.LoginRequest
import com.github.callmeqan.jarviscomposed.data.Uid
import com.github.callmeqan.jarviscomposed.utils.RetrofitAPI // Assuming you have a RetrofitClient
import com.github.callmeqan.jarviscomposed.utils.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN, REGISTER
}

data class AuthScreenState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val name: String = "",
    val retypePassword: String = "",
    val authMode: AuthMode = AuthMode.LOGIN,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationMessage: String? = null,
    val loginSuccess: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AuthScreenState())
    val uiState: StateFlow<AuthScreenState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username, errorMessage = null)
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name, errorMessage = null)
    }

    fun onRetypePasswordChange(retypePassword: String) {
        _uiState.value = _uiState.value.copy(retypePassword = retypePassword, errorMessage = null)
    }

    fun toggleAuthMode() {
        val newMode = if (_uiState.value.authMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
        _uiState.value = AuthScreenState(authMode = newMode) // Reset fields on mode toggle
    }

    fun submit() {
        if (_uiState.value.authMode == AuthMode.LOGIN) {
            loginUser()
        } else {
            registerUser()
        }
    }

    private fun loginUser() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Email and password cannot be empty.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            try {
                val response = RetrofitAPI.login(LoginRequest(email = state.email, password = state.password))
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    if (loginResponse.access_token != null && loginResponse.refresh_token != null) {
                        UserSessionManager.saveAuthTokens(getApplication(), loginResponse.access_token, loginResponse.refresh_token)
                        UserSessionManager.saveUserEmail(getApplication(), state.email) // Optionally save email
                        _uiState.value = state.copy(isLoading = false, loginSuccess = true)
                    } else {
                        _uiState.value = state.copy(isLoading = false, errorMessage = loginResponse.msg ?: "Login failed: No token received")
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    _uiState.value = state.copy(isLoading = false, errorMessage = "Login failed: $errorMsg")
                }
            } catch (e: Exception) {
                _uiState.value = state.copy(isLoading = false, errorMessage = "Login error: ${e.message}")
            }
        }
    }

    private fun registerUser() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || state.username.isBlank() || state.name.isBlank() || state.retypePassword.isBlank()) {
            _uiState.value = state.copy(errorMessage = "All fields are required for registration.")
            return
        }
        if (state.password != state.retypePassword) {
            _uiState.value = state.copy(errorMessage = "Passwords do not match.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null, registrationMessage = null)
            try {
                val userToRegister = Uid(
                    username = state.username,
                    name = state.name,
                    email = state.email,
                    password = state.password,
                    retype_password = state.retypePassword
                )
                val response = RetrofitAPI.register(userToRegister)
                if (response.isSuccessful && response.body() != null) {
                    // Optionally handle the access token from registration if provided and app flow supports it
                    // For now, just show the success message. User might need to verify email.
                    _uiState.value = state.copy(
                        isLoading = false,
                        registrationMessage = response.body()!!.msg,
                        authMode = AuthMode.LOGIN // Switch to login mode after successful registration message
                    )
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    _uiState.value = state.copy(isLoading = false, errorMessage = "Registration failed: $errorMsg")
                }
            } catch (e: Exception) {
                _uiState.value = state.copy(isLoading = false, errorMessage = "Registration error: ${e.message}")
            }
        }
    }

    fun checkLoginStatus() {
        viewModelScope.launch {
            if (UserSessionManager.isLoggedIn(getApplication())) {
                _uiState.value = _uiState.value.copy(loginSuccess = true)
            }
        }
    }
}