package com.github.callmeqan.jarviscomposed.utils

import com.github.callmeqan.jarviscomposed.data.Message
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface RetrofitAPI {

    // as we are making a post request to post a data
    // so we are annotating it with post
    // and along with that we are passing a parameter as users
    @POST("chatbot/gemma3")
    // on below line we are creating a method to post our data.
    fun sendMessage2Server(@Body clientMessage: Message?): Call<Message?>?
}