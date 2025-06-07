package com.github.callmeqan.jarviscomposed

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.github.callmeqan.jarviscomposed.ui.screens.ChatScreen
import com.github.callmeqan.jarviscomposed.ui.theme.JarvisComposedTheme
import io.github.cdimascio.dotenv.dotenv

open class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val dotenv = dotenv {
            directory = "./assets"
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }
        super.onCreate(savedInstanceState)

        // Initialize BluetoothAdapter
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setContent {
            JarvisComposedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1C1B1B)
                ) {
                    ChatScreen(
                        bluetoothAdapter = bluetoothAdapter,
                    )
                }
            }
        }
    }
}