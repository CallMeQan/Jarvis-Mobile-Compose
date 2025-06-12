package com.github.callmeqan.jarviscomposed.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.github.callmeqan.jarviscomposed.ui.screens.SettingsActivity




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAppBar(statusTxt: String, settingBtnOnClick: () -> Unit) {
    // Lấy Context
    val context = LocalContext.current
    CenterAlignedTopAppBar(
        title = {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mr. Jarvis (ESP32)",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = statusTxt,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                )
            }
        },
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    bottomStart = 30.dp,
                    bottomEnd = 30.dp
                )
            ),
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFF1F1F1F)
        ),
//        navigationIcon = {
//            IconButton(onClick = {
//                // TODO: Navbar logic, but we don't need this?
//            }) {
//                Icon(
//                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
//                    contentDescription = null,
//                    tint = Color.White
//                )
//            }
//        },
        actions = {
            IconButton(onClick = {
                // Gọi callback (nếu có) rồi mở SettingsActivity
                settingBtnOnClick()
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
            }
        },
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    )
}