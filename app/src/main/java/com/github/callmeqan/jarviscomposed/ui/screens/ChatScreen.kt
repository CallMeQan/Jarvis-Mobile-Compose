package com.github.callmeqan.jarviscomposed.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.github.callmeqan.jarviscomposed.data.Message
import com.github.callmeqan.jarviscomposed.ui.components.ChatAppBar
import com.github.callmeqan.jarviscomposed.ui.components.MessageBox
import com.github.callmeqan.jarviscomposed.ui.components.MessageInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)
    var isConnected = remember { mutableStateOf(true) }
    val messages = remember { mutableStateListOf<Message>() }
    var input by remember { mutableStateOf("") }

    fun sendBtnOnClick() {
        Log.i("sendBtnOnClick", "clicked")
        if (input.isNotBlank()) {
            Log.i("sendBtnOnClick", "Found input")
            // TODO: Send message to server

            messages.add(
                Message(
                    message = input,
                    isMe = true
                )
            )
            input = "" // Clear the input after sending
        }
    }

    fun micBtnOnClick() {
        Log.i("micBtnOnClick", "clicked")
    }
    
    fun settingBtnOnClick(){
        Log.i("settingBtnOnClick", "clicked")
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
            ChatAppBar(
                statusTxt = if (isConnected.value) "Connected" else "Not connected",
                settingBtnOnClick = {settingBtnOnClick()}
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
                items(messages.reversed()) { message -> // reversed for newest at bottom
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
}