package com.github.callmeqan.jarviscomposed.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.github.callmeqan.jarviscomposed.ui.theme.JarvisComposedTheme
import com.github.callmeqan.jarviscomposed.ui.components.ChatAppBar

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisComposedTheme {
                // Tạo các state giống SettingsScreen
                val snackbarHostState = remember { SnackbarHostState() }
                val topBarState = rememberTopAppBarState()
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        ChatAppBar(
                            statusTxt = "Cài đặt",
                            settingBtnOnClick = { finish() }  // nút back giờ finish Activity
                        )
                    },
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                ) { padding ->
                    // Gọi thẳng SettingsScreen, truyền onBack thành finish()
                    SettingsScreen(onBack = { finish() })
                }
            }
        }
    }
}