package com.github.callmeqan.jarviscomposed.data

import android.graphics.Bitmap

data class ChatMessage(
    val message: String,
    val role: String = "user",
    val image: Bitmap? = null,
)