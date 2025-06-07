package com.github.callmeqan.jarviscomposed.data

data class ChatMessage(
    val message: String,
    val role: String = "user",
)