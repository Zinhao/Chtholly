package com.zinhao.chtholly.network.openai

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json; charset=utf-8",
        @Body request: ChatRequest
    ): ChatResponse
}