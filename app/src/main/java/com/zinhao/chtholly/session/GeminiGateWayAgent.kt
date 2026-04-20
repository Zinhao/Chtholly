package com.zinhao.chtholly.session

import android.util.Log
import com.lark.oapi.service.application.v6.model.Bot
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.entity.GeminiAIAskAble.Companion.geminiResponseAdapter
import com.zinhao.chtholly.network.GEMINI_TOOLS
import com.zinhao.chtholly.network.LoggingInterceptor
import com.zinhao.chtholly.network.Tool
import com.zinhao.chtholly.network.gemini.Content
import com.zinhao.chtholly.network.gemini.GenerationConfig
import com.zinhao.chtholly.network.gemini.Part
import com.zinhao.chtholly.network.gemini.PostRequest
import com.zinhao.chtholly.network.gemini.SystemInstruction
import com.zinhao.chtholly.network.gemini.ThinkingConfig
import com.zinhao.chtholly.session.GeminiSession.Companion.MODEL_GEMINI_3_FL_PRE
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiGateWayAgent(val api: String, val key: String) {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(LoggingInterceptor())
        .build()

    private var data: PostRequest
    private val tools: MutableList<Tool>  = arrayListOf()
    private val systemInstruction: SystemInstruction = SystemInstruction(listOf(
        Part(
            BotApp.getInstance().replyGateway.replace("\$name", BotApp.getInstance().botName),
            null, null, null
        )
    ))
    private val contents: MutableList<Content>  = arrayListOf()

    private var currentModel: RemoteChatApiSession.RemoteModel = RemoteChatApiSession.RemoteModel(MODEL_GEMINI_3_FL_PRE)

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val dataAdapter: JsonAdapter<PostRequest> = moshi.adapter(PostRequest::class.java)

    init {
        data = PostRequest(null,
            systemInstruction,
            GenerationConfig(ThinkingConfig("low"))
            ,contents)
        tools.add(GEMINI_TOOLS)
        Log.d("GeminiGateWayAgent", ""+ systemInstruction.parts.firstOrNull()?.text)
    }

    fun printContents() {
        val sb = StringBuilder()
        contents.forEach {
            it.parts.firstOrNull()?.text?.let {
                sb.append(it).append("\n")
            }
        }
        Log.d("GeminiGateWayAgent", sb.toString())
    }

    fun needReply(inputs: List<Content>): Boolean {
        if(key.isBlank()) return false
        if(api.isBlank()) return false
        if(inputs.isEmpty()) return false
        contents.clear()
        contents.addAll(inputs)
        printContents()
        val dataString = dataAdapter.toJson(data)
        val requestBody: RequestBody = dataString.toRequestBody("application/json;charset=utf-8".toMediaType())
        val request = Request.Builder().post(requestBody)
            .url("$api/models/${currentModel.str}:generateContent")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", key)
            .addHeader("User-Agent", "Android Application <Chtholly>")
            .build()
        try {
            val response = okHttpClient.newCall(request).execute()
            if(response.isSuccessful) {
                val body = response.body?.string()
                val geminiAnswerResult = geminiResponseAdapter.fromJson(body)
                val candidate = geminiAnswerResult?.candidates?.firstOrNull()
                candidate?.let {
                    if (candidate.finishReason.lowercase() == "stop"){
                        candidate.content.parts.firstOrNull()?.let {
                            return it.text?.lowercase() == "true" || it.text?.lowercase() == "yes"
                        }
                    }
                }
            }
        }catch (e: Exception){
            return false
        }

        return false
    }
}