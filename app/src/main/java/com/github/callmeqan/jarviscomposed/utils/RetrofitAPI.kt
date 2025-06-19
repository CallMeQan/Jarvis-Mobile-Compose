package com.github.callmeqan.jarviscomposed.utils

import com.github.callmeqan.jarviscomposed.data.ChatMessage
import com.github.callmeqan.jarviscomposed.data.LoginRequest
import com.github.callmeqan.jarviscomposed.data.LoginResponse
import com.github.callmeqan.jarviscomposed.data.MessageResponse
import com.github.callmeqan.jarviscomposed.data.ProfileResponse
import com.github.callmeqan.jarviscomposed.data.RecoverToken
import com.github.callmeqan.jarviscomposed.data.Uid
import com.github.callmeqan.jarviscomposed.data.RegisterResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.GET
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

    // Send request to sever
    // signUp, logIn, refreshToken, forgotPassword, recoverPassword, logOut
    @POST("auth/register")
    fun signUp(@Body uid: Uid): Response<RegisterResponse>

    @POST("auth/login")
    fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @GET("auth/profile")
    fun getProfile(@Header("Authorization") token: String): Response<ProfileResponse> // TODO: For future use (e.g., user info, chat log, etc.)

    @POST("auth/refresh")
    fun refreshToken(@Header("Authorization") refreshToken: String): Response<LoginResponse> // Used for JWT refresh

    @POST("auth//logout")
    fun logout(): Response<MessageResponse> // Used to clear client tokens

    // wtf am i doing here
    @POST("auth/forgot-password")
    fun forgotPassword(@Body clientMessage: Uid?): Call<Uid?>?

    @POST("auth//recover-password?a=<token>")
    fun recoverPassword(@Body clientMessage: RecoverToken?): Call<RecoverToken?>?

}
