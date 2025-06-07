package com.github.callmeqan.jarviscomposed.utils

import com.github.callmeqan.jarviscomposed.data.ChatMessage
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface RetrofitAPI {
    // we are passing a parameter as the representation of chat message
    // on below line we are creating a method to post our data.
    @POST("chatbot/gemma3")
    fun sendMessage2Server(@Body clientMessage: ChatMessage?): Call<ChatMessage?>?
}