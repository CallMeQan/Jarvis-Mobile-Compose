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
import com.github.callmeqan.jarviscomposed.ui.screens.SettingScreen
import com.github.callmeqan.jarviscomposed.utils.SharedViewModel
import com.github.callmeqan.jarviscomposed.data.LoginRequest
import com.github.callmeqan.jarviscomposed.data.Uid
import com.github.callmeqan.jarviscomposed.data.LoginResponse
import com.github.callmeqan.jarviscomposed.data.RegisterResponse
import com.github.callmeqan.jarviscomposed.data.ProfileResponse
import com.github.callmeqan.jarviscomposed.data.MessageResponse
import com.github.callmeqan.jarviscomposed.data.RecoverToken
import com.github.callmeqan.jarviscomposed.utils.RetrofitAPI
import com.github.callmeqan.jarviscomposed.utils.UUid

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

                SettingScreen(
                    viewModel = viewModel,
                    onNavigateToMain = {
                        navController.navigate(route = "chat")
                    }
                )
            }
        }
        // Add LoginScreen route
        composable("login") { entry ->
            val viewModel = entry.sharedViewModel<SharedViewModel>(navController)
            com.github.callmeqan.jarviscomposed.screens.LoginScreen(
                navController = navController,
                viewModel = viewModel
            )
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
    private lateinit var retrofitAPI: RetrofitAPI
    private lateinit var sharedViewModel: SharedViewModel

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

        // Initialize RetrofitAPI (basic example, you may want to move this to a DI or ViewModel)
        retrofitAPI = com.github.callmeqan.jarviscomposed.utils.SharedViewModel().let { vm ->
            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl(vm.url.ifEmpty { "https://3238-2405-4802-a458-8e90-f1fd-bf94-f4ae-9db.ngrok-free.app" }) // fallback base URL
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()
            retrofit.create(RetrofitAPI::class.java)
        }

        // Initialize SharedViewModel
        sharedViewModel = SharedViewModel()

        setContent {
            JarvisComposedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1C1B1B)
                ) {
                    // Pass sharedViewModel to NavApp if you want to use the same instance
                    NavApp(
                        bluetoothAdapter = bluetoothAdapter,
                    )
                }
            }
        }
    }
}