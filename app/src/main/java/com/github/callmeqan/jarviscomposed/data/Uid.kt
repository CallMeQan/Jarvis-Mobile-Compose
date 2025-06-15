package com.github.callmeqan.jarviscomposed.data

data class Uid(
    val username: String = "",
    val name: String = "",
    val email: String,
    val password: String,
    val retype_password: String = "",
)