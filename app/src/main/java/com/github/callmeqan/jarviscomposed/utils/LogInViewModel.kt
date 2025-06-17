// viewmodels/LoginViewModel.kt
package com.github.callmeqan.jarviscomposed.utils

// import android.content.Context // No longer explicitly needed as a type here if only passing getApplication()
import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.callmeqan.jarviscomposed.data.LoginRequest
import com.github.callmeqan.jarviscomposed.data.Uid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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

// This file is now obsolete. All login/register logic has been merged into SharedViewModel.
// Please use SharedViewModel for all authentication-related state and actions.
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val _url = mutableStateOf("")
    val url: String get() = _url.value
    private val _uiState = MutableStateFlow(AuthScreenState())
    val uiState: StateFlow<AuthScreenState> = _uiState.asStateFlow()

    fun setApiUrl(newUrl: String) {
        _url.value = newUrl
    }

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

    private fun retrofitInit() {
        // Consider if you need this method or if Retrofit setup can be more localized
        // or handled by a dependency injection framework.
    }

    private fun loginUser() {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit: Retrofit
        try {
            retrofit = Retrofit.Builder()
                .baseUrl(url + "/") // TODO: Replace with your actual base URL from a config file or constants
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Network client setup error: ${e.message}")
            return
        }

        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Email and password cannot be empty.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            try {
                val response = retrofitAPI.login(LoginRequest(state.email, state.password))
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    if (loginResponse.access_token != null && loginResponse.refresh_token != null) {
                        UUid.saveAuthTokens(getApplication(), loginResponse.access_token, loginResponse.refresh_token)
                        UUid.saveUserEmail(getApplication(), state.email)
                        _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)
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
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit: Retrofit
        try {
            retrofit = Retrofit.Builder()
                .baseUrl(url + "/") // TODO: Replace with your actual base URL
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Network client setup error: ${e.message}")
            return
        }
        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
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
                val response = retrofitAPI.signUp(uid = userToRegister)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = state.copy(
                        isLoading = false,
                        registrationMessage = response.body()!!.msg,
                        authMode = AuthMode.LOGIN
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
            UUid.isLoggedInFlow(getApplication()).collect { isLoggedIn ->
                if (isLoggedIn) {
                    _uiState.value = _uiState.value.copy(loginSuccess = true)
                }
                // Consider if you want to set loginSuccess = false here if !isLoggedIn
                // and the user is not already in a loginSuccess = true state.
                // This depends on how you want the UI to react if the token is cleared elsewhere.
            }
        }
    }
}