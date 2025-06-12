package com.github.callmeqan.jarviscomposed.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.github.callmeqan.jarviscomposed.ui.components.ChatAppBar
import com.github.callmeqan.jarviscomposed.utils.SharedViewModel
import com.github.callmeqan.jarviscomposed.utils.getPairedBluetoothDevices
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SharedViewModel,
    onNavigateToMain: () -> Unit
) {
    // Get data from view model
    var urlText by remember { mutableStateOf(viewModel.url) }  // Start with ViewModel’s URL
    val devices = remember { viewModel.devices }

    // Init other utils
    val snackbarHostState = remember { SnackbarHostState() }
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)

    fun urlButtonOnClick() {
        // Helper function to check if all permissions are granted
        viewModel.updateUrl(urlText)
        onNavigateToMain()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ChatAppBar(
                statusTxt = "Setting",
                settingBtnOnClick = { onNavigateToMain() },
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Server URL", modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("Nhập URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = ::urlButtonOnClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Send",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

