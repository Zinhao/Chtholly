package com.zinhao.chtholly.entity

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.network.gemini.GeminiResponse
import com.zinhao.chtholly.session.GeminiSession.Companion.instance
import okhttp3.Call
import okhttp3.Response
import org.json.JSONException
import java.io.IOException
import java.util.*

class GeminiAIAskAble : NetAiAskAble {
    val TAG = "GeminiAIAskAble"

    constructor(packageName: String?, question: Message?, delayReplyCallback: DelayReplyCallback?) : super(
        packageName,
        question,
        delayReplyCallback
    )

    override fun throwToChild(): Boolean {
        Log.i("Command", "GeminiAIAskAble throwToChild")
        return instance!!.startAsk(this)
    }

    override fun onFailure(call: Call, e: IOException) {
        getAnswer().setMessage(String.format(Locale.CHINA, "\uD83D\uDE44发生错误了:%s %s", e.message, e.cause))
        replay = true
        if (delayReplyCallback != null) delayReplyCallback.onReply(this)
    }



    //* 模型停止生成令牌的原因。如果模型达到自然停止点或提供的停止序列，则这将stop；
    //* 如果达到请求中指定的最大令牌数，则将length；
    //* 如果由于内容过滤器中的标志而省略内容，则为 content_filter；
    //* 如果模型达到 tool_calls，则为 tool_calls称为工具。
    @OptIn(ExperimentalStdlibApi::class)
    @Throws(IOException::class)
    override fun onResponse(call: Call, response: Response) {
        if (response.code == 200) {
            val body = response.body
            if (body != null) {
                try {
                    val geminiAnswerResult = jsonAdapter.fromJson(body.string())
                    val candidate = geminiAnswerResult?.candidates?.firstOrNull()
                    candidate?.let {
                        if (candidate.finishReason == "length") {
                            //自动总结
                            instance?.requestChatSummarize()
                        } else if (candidate.finishReason == "tool_calls") {
                        } else if (candidate.finishReason.lowercase() == "stop") {
                            val content = candidate.content.parts.firstOrNull()?.text
                            if (content != null && content.trim { it <= ' ' } != "null") {
                                doTextReply(content)
                                doTTSReply(content)
                                instance!!.addAssistantChat(content)
                            }
                        }
                    }

                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            }
        }else{
            answer.message = response.message
            answer.speaker = "ServerError"
        }
        replay = true
        if (delayReplyCallback != null)
            delayReplyCallback.onReply(this)
        response.close()
    }

    companion object{
        private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        @OptIn(ExperimentalStdlibApi::class)
        private val jsonAdapter: JsonAdapter<GeminiResponse> = moshi.adapter<GeminiResponse>()
    }
}
