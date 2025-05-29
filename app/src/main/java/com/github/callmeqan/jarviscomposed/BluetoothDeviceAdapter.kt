package com.github.callmeqan.jarviscomposed

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.items

@Composable
fun BluetoothDeviceList(
    devices: List<BluetoothDevice>,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    var selectedAddress by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(devices, key = { it.address }) { device ->
            BluetoothDeviceListItem(
                device = device,
                isSelected = selectedAddress == device.address,
                onClick = {
                    selectedAddress = device.address
                    onDeviceClick(device)
                }
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothDeviceListItem(
    device: BluetoothDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .alpha(if (isSelected) 0.5f else 1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = device.name ?: "Unknown Device",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = device.address ?: "",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}