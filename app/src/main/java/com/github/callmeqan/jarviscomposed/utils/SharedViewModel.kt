package com.github.callmeqan.jarviscomposed.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotApplyResult
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.github.callmeqan.jarviscomposed.data.ChatMessage
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.github.callmeqan.jarviscomposed.data.LoginRequest
import com.github.callmeqan.jarviscomposed.data.Uid
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SharedViewModel() : ViewModel() {

    // URL string (without / at the end)
    private val _url = mutableStateOf("")
    val url: String get() = _url.value

    // Api mode for chatbot
    // See SharedViewModel.updateApi() for more info
    private val _apiMode = mutableStateOf("chatbot/vanilla")
    val apiMode: String get() = _apiMode.value

    // Chat history list that Compose can observe
    private var _chatHistory = SnapshotStateList<ChatMessage>()
    val chatHistory: SnapshotStateList<ChatMessage> get() = _chatHistory

    // Available Bluetooth Devices
    private var _devices = SnapshotStateList<BluetoothDevice>()
    val devices: SnapshotStateList<BluetoothDevice> get() = _devices

    // Currently connected Bluetooth device (nullable)
    private val _device = mutableStateOf<BluetoothDevice?>(null)
    val device get() = _device.value

    // Number of classes
    private val _NUM_CLASSES = 2

    // Auth mode enum
    enum class AuthMode {
        LOGIN, REGISTER
    }

    // Auth screen state data class
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

    // Auth state
    private val _uiState = MutableStateFlow(AuthScreenState())
    val uiState: StateFlow<AuthScreenState> = _uiState.asStateFlow()

    // =============================================

    // Updating URL functions
    fun updateUrl(newUrl: String) {
        _url.value = newUrl
    }

    // Updating api mode
    fun updateApi(newApi: String) {
        // Could be of these three
        // [
        //      chatbot/vanilla,
        //      chatbot/function_call_chatbot,
        //      chatbot/bluetooth_processor
        // ]
        _apiMode.value = newApi
    }

    // Bluetooth functions
    fun updateDevices(newDevices: SnapshotStateList<BluetoothDevice>) {
        _devices = newDevices
    }
    fun connectTo(device: BluetoothDevice) {
        _device.value = device
    }
    fun disconnect() {
        _device.value = null
    }

    // Auth functions
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
        _uiState.value = AuthScreenState(authMode = newMode)
    }
    fun submit(context: Context) {
        if (_uiState.value.authMode == AuthMode.LOGIN) {
            loginUser(context)
        } else {
            registerUser(context)
        }
    }
    private fun loginUser(context: Context) {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        val retrofit: Retrofit
        try {
            retrofit = Retrofit.Builder()
                .baseUrl(url + "/")
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
                        UUid.saveAuthTokens(context, loginResponse.access_token, loginResponse.refresh_token)
                        UUid.saveUserEmail(context, state.email)
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
    private fun registerUser(context: Context) {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        val retrofit: Retrofit
        try {
            retrofit = Retrofit.Builder()
                .baseUrl(url + "/")
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
    fun checkLoginStatus(context: Context) {
        viewModelScope.launch {
            UUid.isLoggedInFlow(context).collect { isLoggedIn =>
                if (isLoggedIn) {
                    _uiState.value = _uiState.value.copy(loginSuccess = true)
                }
            }
        }
    }
    fun logout(context: Context) {
        viewModelScope.launch {
            UUid.clearSession(context)
            _uiState.value = AuthScreenState() // Reset state
        }
    }
    fun forgotPassword(context: Context, email: String) {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        val retrofit: Retrofit
        try {
            retrofit = Retrofit.Builder()
                .baseUrl(url + "/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "Network client setup error: ${e.message}")
            return
        }
        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, registrationMessage = null)
            try {
                val response = retrofitAPI.forgotPassword(Uid(email = email))
                if (response != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, registrationMessage = "Reset link sent to your email (mock)")
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Failed to send reset link")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Forgot password error: ${e.message}")
            }
        }
    }
    fun recoverPassword(context: Context, token: String, newPassword: String, confirmPassword: String) {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        val retrofit: Retrofit
        try {
            retrofit = Retrofit.Builder()
                .baseUrl(url + "/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "Network client setup error: ${e.message}")
            return
        }
        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, registrationMessage = null)
            try {
                val response = retrofitAPI.recoverPassword(
                    com.github.callmeqan.jarviscomposed.data.RecoverToken(token, newPassword, confirmPassword)
                )
                if (response != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, registrationMessage = "Password updated successfully")
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Failed to update password")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Recover password error: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        println("SharedViewModel cleared")
    }

}
