package com.zinhao.chtholly.network.gemini

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApi {

    @POST("models/{model}:generateContent")
    fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body body: PostRequest
    ): Call<GeminiResponse>

    @POST("models/{model}:generateContent")
    suspend fun generateContentSuspend(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body body: PostRequest
    ): GeminiResponse

    /**
     * Interactions API: POST /v1beta/interactions
     * Recommended for all new projects. Supports server-side state, background execution, etc.
     */
    @POST("interactions")
    fun createInteraction(
        @Header("x-goog-api-key") apiKey: String,
        @Body body: InteractionRequest
    ): Call<InteractionResponse>

    @POST("interactions")
    suspend fun createInteractionSuspend(
        @Header("x-goog-api-key") apiKey: String,
        @Body body: InteractionRequest
    ): InteractionResponse
}
