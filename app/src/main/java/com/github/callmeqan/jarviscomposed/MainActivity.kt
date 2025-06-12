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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.github.callmeqan.jarviscomposed.ui.screens.SettingsScreen
import com.github.callmeqan.jarviscomposed.utils.SharedViewModel

@Composable
fun NavApp(bluetoothAdapter: BluetoothAdapter) {

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        navigation(
            startDestination = "chat",
            route = "main"
        ) {
            composable("chat") { entry ->
                val viewModel = entry.sharedViewModel<SharedViewModel>(navController,)

                ChatScreen(
                    bluetoothAdapter = bluetoothAdapter,
                    viewModel = viewModel,
                    onNavigate = {
                        navController.navigate("setting")
                    }
                )
            }
            composable("setting") { entry ->
                val viewModel = entry.sharedViewModel<SharedViewModel>(navController)

                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToMain = {
                        navController.navigate(route = "chat")
                    }
                )
            }
        }
    }
}

@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
    navController: NavHostController,
): T {
    val navGraphRoute = destination.parent?.route ?: return viewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return viewModel(parentEntry)
}

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
                    NavApp(
                        bluetoothAdapter = bluetoothAdapter,
                    )
                }
            }
        }
    }
}