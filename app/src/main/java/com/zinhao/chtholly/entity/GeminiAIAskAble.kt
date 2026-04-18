package com.zinhao.chtholly.entity

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.network.*
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.GeminiResponse
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


    //* 模型停止生成令牌的原因。如果模型达到自然停止点或提供的停止序列，则这将stop；
    //* 如果达到请求中指定的最大令牌数，则将length；
    //* 如果由于内容过滤器中的标志而省略内容，则为 content_filter；
    //* 如果模型达到 tool_calls，则为 tool_calls称为工具。
    // 智能机器人
    @OptIn(ExperimentalStdlibApi::class)
    @Throws(IOException::class)
    override fun onResponse(call: Call, response: Response) {
        if (response.code == 200) {
            val body = response.body
            if (body != null) {
                try {
                    val geminiAnswerResult = geminiResponseAdapter.fromJson(body.string())
                    val candidate = geminiAnswerResult?.candidates?.firstOrNull()
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
                                candidate.content.parts.forEach { part ->
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

                        }
                    }

                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            }
        } else {
            answer.speaker = "ServerError [${response.code}]"
            answer.message = response.message
            answerFinish()
            delayReplyCallback?.onReplySuccess(this)
        }
        response.close()
    }

    val answerTextBuilder = StringBuilder()

    fun answerFinish() {
        replyReady = true
    }

    fun FunctionCall.callToolFunction(thoughtSignature: String?) {
        FileLogger.i(TAG, "callToolFunction called:$name, arg:$args")
        // 根目录下有哪些文件@冰糖
        // test_share_file.txt 的内容是什么？@冰糖
        // 创建一个新文件，把“20260417，今天天气多云，看起来随时可能下雨”记录下来@冰糖
        // 在 test_share_file.txt 添加一行：今天天气多云，随时都可能下雨
        // @冰糖 把test_share_file.txt的内容写入到一个新的文件，新文件名称为new_file_test.txt
        if(!question.isEnableCommand){
            instance?.addToolErr(name, Exception("Insufficient permissions"),thoughtSignature)
            return
        }
        // 读取 diary_0415.txt 的内容
        when (name) {
            FileWriter.name -> {
                FileWriter.geminiCallFun?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            ListFiles.name -> {
                ListFiles.geminiCallFun?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            FileReader.name -> {
                FileReader.geminiCallFun?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            AppendText.name->{
                AppendText.geminiCallFun?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            SendFile.name->{
                SendFile.geminiCallFun?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            // 打开应用Chtholly
            GetViewNode.name->{
                GetViewNode.geminiCallFun?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            DoStepOnNode.name ->{
                DoStepOnNode.geminiCallFun?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
        }
    }

    companion object {
        val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        @OptIn(ExperimentalStdlibApi::class)
        private val geminiResponseAdapter: JsonAdapter<GeminiResponse> =
            moshi.adapter<GeminiResponse>()


    }
}
