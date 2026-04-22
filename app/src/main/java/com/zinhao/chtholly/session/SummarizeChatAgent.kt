package com.zinhao.chtholly.session

import android.util.Log
import androidx.media3.common.C
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.entity.GeminiAIAskAble.Companion.geminiResponseAdapter
import com.zinhao.chtholly.network.LoggingInterceptor
import com.zinhao.chtholly.network.gemini.*
import com.zinhao.chtholly.session.GeminiSession.Companion.MODEL_GEMINI_3_FL_PRE
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SummarizeChatAgent(val api: String, val key: String) {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(100, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS)
        .addInterceptor(LoggingInterceptor())
        .build()

    private var data: PostRequest
    private val systemInstruction: SystemInstruction = SystemInstruction(listOf(
        Part(
            BotApp.getInstance().summarizeChatAgentDesc,
            null, null, null
        )
    ))
    private val contents: MutableList<Content>  = arrayListOf()

    private var currentModel: RemoteChatApiSession.RemoteModel = RemoteChatApiSession.RemoteModel(MODEL_GEMINI_3_FL_PRE)

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val dataAdapter: JsonAdapter<PostRequest> = moshi.adapter(PostRequest::class.java)

    init {
        Log.d("SummarizeChatAgent", "init:"+ systemInstruction.parts.firstOrNull()?.text)
        data = PostRequest(null,
            systemInstruction,
            GenerationConfig(ThinkingConfig("low"))
            ,contents)
    }

    fun printContents() {
        val sb = StringBuilder()
        contents.forEach {
            it.parts.firstOrNull()?.text?.let {
                sb.append(it.replace('\n','\t')).append("\n")
            }
        }
        Log.d("SummarizeChatAgent", sb.toString())
    }

    fun requestSummarize(inputs: List<Content>): Content? {
        Log.d("SummarizeChatAgent", "requestSummarize:${inputs.size}")
        if(key.isBlank()) return null
        if(api.isBlank()) return null
        if(inputs.isEmpty()) return null

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
                        return candidate.content
                    }
                }
            }
        }catch (e: Exception){
            return null
        }

        return null
    }
}