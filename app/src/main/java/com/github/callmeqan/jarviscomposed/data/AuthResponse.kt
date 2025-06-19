package com.github.callmeqan.jarviscomposed.data

data class LoginResponse(
    val access_token: String?,
    val refresh_token: String?,
    val msg: String? // For error messages
)

data class RegisterResponse(
    val msg: String,
    val access_token: String? // Optional, as email verification might be needed first
)

data class ProfileResponse(
    val username: String?,
    val email: String?,
    val name: String?,
    // Omit password from profile response for security
    // val create_at: String? // Assuming it's a String, adjust if it's a Date/Long
    val msg: String? // For error messages
)

data class MessageResponse( // Generic response for simple messages
    val msg: String
)