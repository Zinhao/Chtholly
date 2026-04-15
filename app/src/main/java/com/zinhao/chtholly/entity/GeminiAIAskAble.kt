package com.zinhao.chtholly.entity

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.network.gemini.CodeGenerate
import com.zinhao.chtholly.network.gemini.Content
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.FunctionResponse
import com.zinhao.chtholly.network.gemini.GeminiResponse
import com.zinhao.chtholly.network.gemini.Part
import com.zinhao.chtholly.network.gemini.PrintInfo
import com.zinhao.chtholly.session.GeminiSession.Companion.ROLE_USER
import com.zinhao.chtholly.session.GeminiSession.Companion.instance
import com.zinhao.chtholly.utils.AsyncHelper
import com.zinhao.chtholly.utils.FileLogger
import com.zinhao.chtholly.utils.LocalFileCache
import okhttp3.Call
import okhttp3.Response
import org.json.JSONException
import java.io.File
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
        return instance!!.callApi(this)
    }

    override fun onFailure(call: Call, e: IOException) {
        getAnswer().setMessage(String.format(Locale.CHINA, "\uD83D\uDE44发生错误了:%s %s", e.message, e.cause))
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
                    val geminiAnswerResult = jsonAdapter.fromJson(body.string())
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
                            candidate.content.parts.forEach { part ->
                                if(part.functionCall!=null){
                                    functionCounter++
                                }
                                part.functionCall?.callToolFunction(part.thoughtSignature)
                                part.text?.let { text->
                                    answerTextBuilder.append(text)
                                }
                            }
                            if(functionCounter==0){
                                doTextReply(answerTextBuilder.toString())
                                answerFinish()
                                delayReplyCallback?.onReplySuccess(this)
                            }
                        }
                    }

                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            }
        }else{
            answer.speaker = "ServerError [${response.code}]"
            answer.message = response.message
            answerFinish()
            delayReplyCallback?.onReplySuccess(this)
        }
        response.close()
    }

    val answerTextBuilder = StringBuilder()

    fun answerFinish(){
        replyReady = true
    }

    fun FunctionCall.callToolFunction(thoughtSignature: String?) {
        if(question.speaker == BotApp.getInstance().adminName){
            if(name == PrintInfo.name){
                val methodName = args["name"].toString()
                FileLogger.i(TAG,"call PrintInfo=> ${methodName}")
                val method = Command::class.java.getDeclaredMethod(methodName)
                val result = method.invoke(this@GeminiAIAskAble)
                if (result != null) {
                    instance?.addContent(
                        Content(
                            listOf(Part(null,null, FunctionResponse(
                                name,mapOf(
                                    Pair("invoke_return", result.toString()),
                                )
                            ),thoughtSignature)),ROLE_USER
                        )
                    )
                }
                if (answer.message.isNotEmpty()){
                    answerTextBuilder.append(answer.message)
                }
                doTextReply(answerTextBuilder.toString())
                answerFinish()
                delayReplyCallback?.onReplySuccess(this@GeminiAIAskAble)
                //我需要帮助文档
                // 很好，你帮我大忙了
                // 查看消息上下文
            }else if(name == CodeGenerate.name){
                val fileName = args["file_name"].toString()
                val textContent = args["text_content"].toString()
                val file = File(LocalFileCache.getInstance().getWorkSpaceDir(), fileName)
                AsyncHelper.doAsyncPart{
                    NekoChatService.getInstance().shareFile(file)
                    var writeSuccess = false
                    var exception: Exception? = null
                    try {
                        LocalFileCache.getInstance().writeTextSync(file,textContent)
                        writeSuccess = true
                    }catch (e: Exception){
                        exception =e
                        writeSuccess = false
                    }
                    instance?.addContent(
                        Content(
                            listOf(Part(null,null, FunctionResponse(
                                name,mapOf(
                                    Pair("write_result", writeSuccess),
                                )
                            ),thoughtSignature)),ROLE_USER
                        )
                    )
                    answerTextBuilder.append( if(writeSuccess) "已写入到: ${file.path}" else "写入失败:${exception?.message}")
                    initShareStepTo(NekoChatService.getInstance().qqChatHandler.chatTitle)

                    if (answerTextBuilder.isNotEmpty()){
                        doTextReply(answerTextBuilder.toString())
                    }
                    answerFinish()
                    delayReplyCallback?.onReplySuccess(this@GeminiAIAskAble)
                    FileLogger.i(TAG,"write_to_file: ${file.path} result:${writeSuccess}")
                }


                //帮我写一个快速排序，用java，写入到文件
            }
        }else{
            doTextReply(HARD)
        }
    }

    companion object{
        val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        @OptIn(ExperimentalStdlibApi::class)
        private val jsonAdapter: JsonAdapter<GeminiResponse> = moshi.adapter<GeminiResponse>()
    }
}
