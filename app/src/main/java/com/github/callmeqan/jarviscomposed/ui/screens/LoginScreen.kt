// screens/LoginScreen.kt
package com.github.callmeqan.jarviscomposed.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.callmeqan.jarviscomposed.ui.components.ChatAppBar
import com.github.callmeqan.jarviscomposed.utils.SharedViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: SharedViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Check login status when the screen is first composed
    LaunchedEffect(Unit) {
        viewModel.checkLoginStatus(context)
    }

    // Navigate away if login becomes successful
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            navController.popBackStack()
        }
    }

    // Add scrollBehavior for the app bar
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ChatAppBar(
                statusTxt = if (uiState.authMode == SharedViewModel.AuthMode.LOGIN) "Login" else "Sign Up",
                settingBtnOnClick = {},
                showBluetoothConfig = false,
                isLoggedIn = false,
                loginLogoutBtnOnClick = { navController.navigate("chat") }, // App bar left icon navigates to ChatScreen
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.authMode == SharedViewModel.AuthMode.REGISTER) {
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = { viewModel.onUsernameChange(it) },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChange(it) },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.authMode == SharedViewModel.AuthMode.REGISTER) {
                OutlinedTextField(
                    value = uiState.retypePassword,
                    onValueChange = { viewModel.onRetypePasswordChange(it) },
                    label = { Text("Retype Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        viewModel.submit(context)
                        // Removed navigation to ChatScreen here
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.authMode == SharedViewModel.AuthMode.LOGIN) "Login" else "Register")
                }
            }

            TextButton(onClick = { viewModel.toggleAuthMode() }) {
                Text(
                    if (uiState.authMode == SharedViewModel.AuthMode.LOGIN) "Don't have an account? Register"
                    else "Already have an account? Login"
                )
            }

            // Forgot password button (only in login mode)
            if (uiState.authMode == SharedViewModel.AuthMode.LOGIN) {
                var showForgot by remember { mutableStateOf(false) }
                if (!showForgot) {
                    TextButton(onClick = { showForgot = true }) {
                        Text("Forgot password?")
                    }
                } else {
                    var forgotEmail by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text("Enter your email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.forgotPassword(context, forgotEmail) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send reset link")
                    }
                }
            }

            // Change password section (show if user has a reset token)
            var showChangePassword by remember { mutableStateOf(false) }
            TextButton(onClick = { showChangePassword = !showChangePassword }) {
                Text("Change password with token")
            }
            if (showChangePassword) {
                var token by remember { mutableStateOf("") }
                var newPassword by remember { mutableStateOf("") }
                var confirmPassword by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Reset Token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.recoverPassword(context, token, newPassword, confirmPassword) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Password")
                }
            }

            // Logout button (if logged in)
            if (uiState.loginSuccess) {
                Button(
                    onClick = { viewModel.logout(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logout")
                }
            }

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            uiState.registrationMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary, // Or a success color
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Show red warning if not connected to a server (url is blank or default)
            val showServerWarning = viewModel.url.isBlank() || viewModel.url == "http://10.0.2.2:8000/"
            if (showServerWarning) {
                Text(
                    text = "You must connect to a server in Settings before logging in!",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
            // Allow login/register button to always be enabled, regardless of server connection
        }
    }
}