package com.zinhao.chtholly.network

import retrofit2.http.GET
import retrofit2.http.Header

interface LMStudioApi {

    @GET("api/v1/models")
    suspend fun getModels(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json; charset=utf-8",
    ): ModelsResponse
}