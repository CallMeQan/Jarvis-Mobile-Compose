package com.github.callmeqan.jarviscomposed.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice

@SuppressLint("MissingPermission")
fun getPairedBluetoothDevices(adapter: BluetoothAdapter): List<BluetoothDevice> =
    adapter.bondedDevices?.toList() ?: emptyList()