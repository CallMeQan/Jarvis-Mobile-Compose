package com.github.callmeqan.jarviscomposed.utils

import com.github.callmeqan.jarviscomposed.data.ChatMessage
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface RetrofitAPI {
    // We are passing a parameter as the representation of chat message

    // Send message to a chatbot with function calling ability
    @POST("chatbot/vanilla")
    fun sendToVanillaChatbot(@Body clientMessage: ChatMessage?): Call<ChatMessage?>?

    // Send message to a chatbot with function calling ability
    @POST("chatbot/function_call_chatbot")
    fun sendToFunctionCallChatbot(@Body clientMessage: ChatMessage?): Call<ChatMessage?>?

    // Send message to a chatbot to process command
    @POST("chatbot/bluetooth_processor")
    fun sendToCommandProcessor(@Body clientMessage: ChatMessage?): Call<ChatMessage?>?
}