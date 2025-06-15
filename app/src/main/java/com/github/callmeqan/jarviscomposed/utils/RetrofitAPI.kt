package com.github.callmeqan.jarviscomposed.utils

import com.github.callmeqan.jarviscomposed.data.ChatMessage
import com.github.callmeqan.jarviscomposed.data.RecoverToken
import com.github.callmeqan.jarviscomposed.data.Uid
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface RetrofitAPI {
    // We are passing a parameter as the representation of chat message

    // Send message to a chatbot with function calling ability
    // the code should be "fcc"
    @POST("chatbot/function_call_chatbot")
    fun sendToFunctionCallChatbot(@Body clientMessage: ChatMessage?): Call<ChatMessage?>?

    // Send message to a chatbot to process command
    // the code should be "bc"
    @POST("chatbot/bluetooth_processor")
    fun sendToCommandProcessor(@Body clientMessage: ChatMessage?): Call<ChatMessage?>?

    // Send request to sever
    // signUp, logIn, refreshToken, forgotPassword, recoverPassword, logOut
    @POST("auth/register")
    fun signUp(@Body clientMessage: Uid?): Call<Uid?>?

    @POST("auth/login")
    fun logIn(@Body clientMessage: Uid?): Call<Uid?>?

    @POST("auth/refresh")
    fun refreshToken(@Body clientMessage: Uid?): Call<Uid?>?

    @POST("auth/forgot-password")
    fun forgotPassword(@Body clientMessage: Uid?): Call<Uid?>?

    @POST("auth//recover-password?a=<token>")
    fun recoverPassword(@Body clientMessage: RecoverToken?): Call<RecoverToken?>?

    @POST("auth//logout")
    fun logOut(@Body clientMessage: Uid?): Call<Uid?>?
}
