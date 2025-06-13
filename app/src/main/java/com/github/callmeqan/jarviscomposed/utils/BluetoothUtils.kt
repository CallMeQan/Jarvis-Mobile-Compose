package com.github.callmeqan.jarviscomposed.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

@SuppressLint("MissingPermission")
fun getPairedBluetoothDevices(adapter: BluetoothAdapter): SnapshotStateList<BluetoothDevice> {
    return mutableStateListOf<BluetoothDevice>().apply {
        adapter.bondedDevices?.forEach { add(it) }
    }
}