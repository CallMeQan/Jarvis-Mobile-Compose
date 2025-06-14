package com.github.callmeqan.jarviscomposed.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotApplyResult
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.github.callmeqan.jarviscomposed.data.ChatMessage

class SharedViewModel() : ViewModel() {

    // URL string (without / at the end)
    private val _url = mutableStateOf("")
    val url: String get() = _url.value

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

    // =============================================

    // Updating URL functions
    fun updateUrl(newUrl: String) {
        _url.value = newUrl
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

    override fun onCleared() {
        super.onCleared()
        println("SharedViewModel cleared")
    }

}
