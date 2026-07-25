package com.zinhao.chtholly.entity

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.GeminiResponse
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
                    Log.e("GeminiAIAskAble error", e.message,e)
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
        // 打开应用Chtholly
        if(!question.isEnableCommand && name!=MutedUserTool.name){
            instance?.addToolErr(name, Exception("Insufficient permissions"),thoughtSignature)
            return
        }
        // 读取 diary_0415.txt 的内容
        when (name) {
            FileWriterTool.name -> {
                FileWriterTool.funImpl?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            ListFilesTool.name -> {
                ListFilesTool.funImpl?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            FileReaderTool.name -> {
                FileReaderTool.funImpl?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            AppendTextTool.name->{
                AppendTextTool.funImpl?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            SendFileTool.name->{
                SendFileTool.funImpl?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            GetViewNode.name->{
                GetViewNode.funImpl?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            DoStepOnNode.name ->{
                DoStepOnNode.funImpl?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            MutedUserTool.name -> {
                MutedUserTool.funImpl?.call(this,thoughtSignature,this@GeminiAIAskAble)
            }
            CreateReminder.name->{CreateReminder.funImpl?.call(this,thoughtSignature,this@GeminiAIAskAble)}
            GetReminders.name->{GetReminders.funImpl?.call(this,thoughtSignature,this@GeminiAIAskAble)}
            GetSystemTime.name->{GetSystemTime.funImpl?.call(this,thoughtSignature,this@GeminiAIAskAble)}
        }
    }

    companion object {
        val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        @OptIn(ExperimentalStdlibApi::class)
        val geminiResponseAdapter: JsonAdapter<GeminiResponse> =
            moshi.adapter<GeminiResponse>()


    }
}
