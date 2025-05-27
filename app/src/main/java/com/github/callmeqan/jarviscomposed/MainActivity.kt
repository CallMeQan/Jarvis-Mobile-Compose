package com.github.callmeqan.jarviscomposed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.callmeqan.jarviscomposed.ui.screens.ChatScreen
import com.github.callmeqan.jarviscomposed.ui.theme.JarvisComposedTheme

open class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisComposedTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = Color(0xFF1C1B1B)
                ) {
                    ChatScreen()
                }
            }
        }
    }
}