package com.zinhao.chtholly.entity

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.GeminiResponse
import com.zinhao.chtholly.network.gemini.InteractionResponse
import com.zinhao.chtholly.network.gemini.tools.AppendTextTool
import com.zinhao.chtholly.network.gemini.tools.CreateReminder
import com.zinhao.chtholly.network.gemini.tools.DoStepOnNode
import com.zinhao.chtholly.network.gemini.tools.FileReaderTool
import com.zinhao.chtholly.network.gemini.tools.FileWriterTool
import com.zinhao.chtholly.network.gemini.tools.GetReminders
import com.zinhao.chtholly.network.gemini.tools.GetSystemTime
import com.zinhao.chtholly.network.gemini.tools.GetViewNode
import com.zinhao.chtholly.network.gemini.tools.ListFilesTool
import com.zinhao.chtholly.network.gemini.tools.MutedUserTool
import com.zinhao.chtholly.network.gemini.tools.SendFileTool
import com.zinhao.chtholly.network.dispatchToolCall
import com.zinhao.chtholly.session.GeminiSession.Companion.instance
import com.zinhao.chtholly.utils.AsyncHelper
import com.zinhao.chtholly.utils.FileLogger
import okhttp3.Call
import okhttp3.Response
import org.json.JSONException
import java.io.IOException
import java.util.*

class GeminiAIAskAble : NetAiAskAble {
    val TAG = "GeminiAIAskAble"
    var retryCount = 0
    constructor(
        packageName: String?,
        question: Message?,
        delayReplyCallback: DelayReplyCallback?
    ) : super(
        packageName,
        question,
        delayReplyCallback
    )

    override fun throwToChild(): Boolean {
        Log.i("Command", "GeminiAIAskAble throwToChild")
        return instance!!.callApi(this, true)
    }

    override fun onFailure(call: Call, e: IOException) {
        getAnswer().setMessage(
            String.format(
                Locale.CHINA,
                "\uD83D\uDE44发生错误了:%s %s",
                e.message,
                e.cause
            )
        )
        answerFinish()
        if (delayReplyCallback != null) delayReplyCallback.onReplySuccess(this)
    }

    fun handleNetworkError(error: Throwable) {
        getAnswer().setMessage(
            String.format(
                Locale.CHINA,
                "\uD83D\uDE44发生错误了:%s %s",
                error.message,
                error.cause
            )
        )
        answerFinish()
        delayReplyCallback?.onReplySuccess(this)
    }

    fun handleApiError(code: Int, message: String) {
        answer.speaker = "ServerError [$code]"
        answer.message = message
        answerFinish()
        delayReplyCallback?.onReplySuccess(this)
    }

    /**
     * 处理已解析的 GeminiResponse — 可由 Retrofit 回调直接调用，
     * 也可由原始 OkHttp onResponse 在 JSON 解析后委托调用。
     *
     * 模型停止生成令牌的原因:
     * - stop: 达到自然停止点
     * - length: 达到最大 token 数
     * - content_filter: 内容过滤器触发
     * - tool_calls / function_call: 模型请求调用工具
     */
    fun handleGeminiResponse(geminiResponse: GeminiResponse?) {
        try {
            val candidate = geminiResponse?.candidates?.firstOrNull()
            candidate?.let {
                if (candidate.finishReason.lowercase() == "length") {
                    //自动总结
                    instance?.requestChatSummarize()
                    answerFinish()
                } else if (candidate.finishReason.lowercase() == "stop") {
                    instance!!.addContent(candidate.content)
                    answerTextBuilder.clear()
                    var functionCounter = 0
                    AsyncHelper.doAsyncPart {
                        candidate.content.parts?.forEach { part ->
                            if (part.functionCall != null) {
                                functionCounter++
                            }
                            part.functionCall?.callToolFunction(part.thoughtSignature)
                            part.text?.let { text ->
                                answerTextBuilder.append(text)
                            }
                        }
                        if (functionCounter == 0) {
                            saveToDatabase(answerTextBuilder.toString())
                            answerFinish()
                            delayReplyCallback?.onReplySuccess(this)
                        }else{
                            instance?.callApi(this,false)
                        }
                    }
                }else if(candidate.finishReason.uppercase() == "MALFORMED_RESPONSE") {
                    //retry
                    if(retryCount < 3){
                        retryCount++
                        instance?.callApi(this,false)
                    }
                }
            }
        } catch (e: JSONException) {
            Log.e("GeminiAIAskAble error", e.message, e)
        }
    }

    /**
     * Handle an InteractionResponse from the Interactions API.
     * The response contains a list of steps: model_output, function_call, etc.
     */
    fun handleInteractionResponse(interactionResponse: InteractionResponse?) {
        try {
            val steps = interactionResponse?.steps
            if (steps.isNullOrEmpty()) {
                FileLogger.w(TAG, "handleInteractionResponse: empty steps")
                answerFinish()
                delayReplyCallback?.onReplySuccess(this)
                return
            }

            answerTextBuilder.clear()
            var functionCounter = 0

            AsyncHelper.doAsyncPart {
                for (step in steps) {
                    when (step.type) {
                        "model_output" -> {
                            step.content?.forEach { content ->
                                content.text?.let { text ->
                                    answerTextBuilder.append(text)
                                }
                            }
                        }
                        "function_call" -> {
                            functionCounter++
                            val name = step.name
                            val args = step.arguments
                            if (name != null) {
                                val functionCall = FunctionCall(name, args ?: emptyMap())
                                functionCall.callToolFunction(step.thoughtSignature)
                            } else {
                                FileLogger.w(TAG, "function_call step with null name")
                            }
                        }
                    }
                }

                if (functionCounter == 0) {
                    saveToDatabase(answerTextBuilder.toString())
                    answerFinish()
                    delayReplyCallback?.onReplySuccess(this)
                } else {
                    instance?.callApi(this, false)
                }
            }
        } catch (e: JSONException) {
            Log.e("GeminiAIAskAble error", e.message, e)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Throws(IOException::class)
    override fun onResponse(call: Call, response: Response) {
        if (response.code == 200) {
            val body = response.body
            if (body != null) {
                try {
                    val geminiAnswerResult = geminiResponseAdapter.fromJson(body.string())
                    handleGeminiResponse(geminiAnswerResult)
                } catch (e: JSONException) {
                    Log.e("GeminiAIAskAble error", e.message, e)
                }
            }
        } else {
            handleApiError(response.code, response.message)
        }
        response.close()
    }

    val answerTextBuilder = StringBuilder()

    fun answerFinish() {
        replyReady = true
    }

    fun FunctionCall.callToolFunction(thoughtSignature: String?) {
        FileLogger.i(TAG, "callToolFunction called:$name, arg:$args")
        if(!question.isEnableCommand && name!=MutedUserTool.name){
            instance?.addToolErr(name, Exception("Insufficient permissions"),thoughtSignature)
            return
        }
        dispatchToolCall(name, this, instance!!, this@GeminiAIAskAble, thoughtSignature)
    }

    companion object {
        val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        @OptIn(ExperimentalStdlibApi::class)
        val geminiResponseAdapter: JsonAdapter<GeminiResponse> =
            moshi.adapter<GeminiResponse>()

        @OptIn(ExperimentalStdlibApi::class)
        val interactionResponseAdapter: JsonAdapter<InteractionResponse> =
            moshi.adapter<InteractionResponse>()
    }
}
