package com.github.callmeqan.jarviscomposed.data

data class RecoverToken(
    val token: String,
    val new_password: String,
    val confirm_password: String,
)