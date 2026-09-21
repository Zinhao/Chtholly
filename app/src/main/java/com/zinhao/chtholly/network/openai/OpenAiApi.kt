package com.zinhao.chtholly.network.openai

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json; charset=utf-8",
        @Body request: ChatRequest
    ): ChatResponse

    @Streaming
    @POST("v1/chat/completions")
    fun chatCompletionStream(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json; charset=utf-8",
        @Header("X-Streaming") streaming: String = "true",
        @Body request: ChatRequest
    ): Call<ResponseBody>
}
