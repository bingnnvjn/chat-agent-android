package com.chatagent.data.api

import com.chatagent.data.model.ChatRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ChatApiService {
    @Streaming
    @Headers("Content-Type: application/json")
    @POST
    suspend fun chatCompletions(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ResponseBody
}
