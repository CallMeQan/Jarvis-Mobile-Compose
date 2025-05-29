package com.github.callmeqan.jarviscomposed.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.callmeqan.jarviscomposed.data.Message
import com.github.callmeqan.jarviscomposed.ui.components.ChatAppBar
import com.github.callmeqan.jarviscomposed.ui.components.MessageBox
import com.github.callmeqan.jarviscomposed.ui.components.MessageInputField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG_NAME = "ChatScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    bluetoothAdapter: BluetoothAdapter,
) {
    val permissions =
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            // Optionally handle permission results here
        }
    )
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)
    val messages = remember { mutableStateListOf<Message>() }
    var input by remember { mutableStateOf("") }

    // For showing Bluetooth picker dialog
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    LaunchedEffect(bluetoothAdapter.isEnabled) {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            context.startActivity(enableBtIntent)
        }
    }

    // Bluetooth connection state
    var connectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var socket by remember { mutableStateOf<BluetoothSocket?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // Connect to device (in coroutine)
    suspend fun connectToDevice(device: BluetoothDevice): BluetoothSocket? {
        return withContext(Dispatchers.IO) {
            try {
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
                val tmpSocket = device.createRfcommSocketToServiceRecord(uuid)
                tmpSocket.connect()
                tmpSocket // Connected!
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun sendBtnOnClick() {
        if (input.isNotBlank() && socket != null && socket!!.isConnected) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    socket!!.outputStream.write(input.toByteArray())
                    socket!!.outputStream.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            messages.add(
                Message(
                    message = input,
                    isMe = true
                )
            )
            input = ""
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.getOrNull(0)
            if (recognizedText != null) {
                Log.i(TAG_NAME, "Recognized text: $recognizedText")
            }
        }
    }

    fun micBtnOnClick() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        }
        speechLauncher.launch(intent)
    }

    fun settingBtnOnClick() {
        pairedDevices = getPairedBluetoothDevices(bluetoothAdapter)
        showBluetoothDialog = true
    }

    Scaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .exclude(WindowInsets.navigationBars)
            .exclude(WindowInsets.ime),
        topBar = {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw Error("No permission")
            }
            ChatAppBar(
                statusTxt = when {
                    isConnecting -> "Connecting..."
                    socket != null && socket!!.isConnected -> "Connected: ${connectedDevice?.name ?: ""}"
                    connectionError != null -> "Error: $connectionError"
                    else -> "Not connected"
                },
                settingBtnOnClick = { settingBtnOnClick() },
            )
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = true,
            ) {
                items(messages.reversed()) { message ->
                    MessageBox(message = message)
                }
            }

            MessageInputField(
                value = input,
                onValueChange = { input = it },
                sendBtnOnClick = ::sendBtnOnClick,
                micBtnOnClick = ::micBtnOnClick
            )
        }
    }

    // Bluetooth device picker dialog
    if (showBluetoothDialog) {
        BluetoothDevicePickerDialog(
            devices = pairedDevices,
            onDismiss = { showBluetoothDialog = false },
            onDeviceSelected = { device ->
                showBluetoothDialog = false
                // Start connecting, update state
                isConnecting = true
                connectionError = null
                connectedDevice = device
                // Try to connect in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = connectToDevice(device)
                        socket = result
                        isConnecting = false
                        connectionError = if (result == null) "Failed to connect" else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }
}

@SuppressLint("MissingPermission")
fun getPairedBluetoothDevices(adapter: BluetoothAdapter): List<BluetoothDevice> =
    adapter.bondedDevices?.toList() ?: emptyList()

@Composable
fun BluetoothDevicePickerDialog(
    devices: List<BluetoothDevice>,
    onDismiss: () -> Unit,
    onDeviceSelected: (BluetoothDevice) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Bluetooth Device") },
        text = {
            if (devices.isEmpty()) {
                Text("No paired devices found.")
            } else {
                Column {
                    devices.forEach { device ->
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            onClick = { onDeviceSelected(device) }
                        ) {
                            if (ActivityCompat.checkSelfPermission(
                                    LocalContext.current,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                throw Error("no permission")
                            }
                            Text("${device.name ?: "Unknown"}\n${device.address}")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}