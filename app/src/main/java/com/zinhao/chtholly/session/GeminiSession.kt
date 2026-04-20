package com.zinhao.chtholly.session

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.db.MessageDao
import com.zinhao.chtholly.entity.GeminiAIAskAble
import com.zinhao.chtholly.entity.Message
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.GEMINI_TOOLS
import com.zinhao.chtholly.network.LoggingInterceptor
import com.zinhao.chtholly.network.Tool
import com.zinhao.chtholly.network.gemini.Content
import com.zinhao.chtholly.network.gemini.FunctionResponse
import com.zinhao.chtholly.network.gemini.GenerationConfig
import com.zinhao.chtholly.network.gemini.Part
import com.zinhao.chtholly.network.gemini.PostRequest
import com.zinhao.chtholly.network.gemini.SystemInstruction
import com.zinhao.chtholly.network.gemini.ThinkingConfig
import com.zinhao.chtholly.session.RemoteChatApiSession.RemoteModel
import com.zinhao.chtholly.utils.FileLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class GeminiSession private constructor(private var chatApi: String?) : NekoSession(),
    RemoteChatApiSession {
    private var data: PostRequest
    private val tools: MutableList<Tool>  = arrayListOf()
    private var systemInstruction: SystemInstruction
    private val contents: MutableList<Content>  = arrayListOf()

    private var currentModel: RemoteModel = RemoteModel(MODEL_GEMINI_3_FL_PRE)
    private var lastMessageTimeStamp: Long = 0
    private val modelList: List<RemoteModel> = arrayListOf(
        RemoteModel(MODEL_GEMINI_3_PRO_PRE),
        RemoteModel(MODEL_GEMINI_3_FL_PRE)
    )

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(100, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS)
        .addInterceptor(LoggingInterceptor())
        .build()

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val dataAdapter: JsonAdapter<PostRequest> = moshi.adapter(PostRequest::class.java)

    init {
        loadChatHistory()
        systemInstruction = SystemInstruction(listOf(Part(BotApp.getInstance().aiSoul,
            null,null,null)))

        data = PostRequest(null,
            systemInstruction,
            GenerationConfig(ThinkingConfig("low"))
            ,contents)
        tools.add(GEMINI_TOOLS)
    }

    override fun setAgentPrompt(charaDesc: String) {
        val agentSys = charaDesc.replace("\$name", BotApp.getInstance().getBotName())
        data.systemInstruction = SystemInstruction(listOf(Part(agentSys,
            null,null,null)))
    }

    fun lastContent(): Content?{
        if(contents.isEmpty()){return null}
        return contents.last()
    }

    override fun getAgentPrompt(): String? {
        return data.systemInstruction.parts.firstOrNull()?.text
    }

    override fun getContextChat(): String {
        return dataAdapter.toJson(data)
    }

    override fun clearContext():Int {
        val len = contents.size
        contents.clear()
        return len
    }

    override fun loadChatHistory(){
        clearContext()
        BotApp.getInstance().getLastTenMessages(MessageDao.MessageGetAllListener { result ->
            if(result.isEmpty()){return@MessageGetAllListener}
            val intoContentMessage = arrayListOf<Message>()
            if(result.size <= 10){
                intoContentMessage.addAll(result)
            }else{
                intoContentMessage.addAll(result.subList(result.size-10, result.size-1))
            }
            for (message in intoContentMessage){
                val messageContent = "${message.message}"
                val role: String
                if(BotApp.getInstance().botName == message.speaker){
                    role = ROLE_MODEL
                }else{
                    role = ROLE_USER
                }
                val content = Content(listOf(Part(messageContent,null,null,null)),role)
                contents.add(content)
            }
        })
    }

    override fun summarize(): Int {
        val chatLen = contents.size
        requestChatSummarize()
        return chatLen
    }

    override fun setModelIndex(modelIndex: Int) {
        if(modelIndex < modelList.size && modelIndex >= 0){
            currentModel = modelList[modelIndex]
        }
    }

    override fun getCurrentModel(): RemoteModel {
        return currentModel
    }

    override fun getModelList(): List<RemoteModel> {
        return modelList
    }

    fun addContent(content: Content) {
        lastMessageTimeStamp = System.currentTimeMillis()
        contents.add(content)
    }

    @Throws(JSONException::class)
    override fun callApi(message: NetAiAskAble,add: Boolean): Boolean {
        if(System.currentTimeMillis() - lastMessageTimeStamp > 10*60*1000L) {
            lastMessageTimeStamp = System.currentTimeMillis()
            val timeContent = Content(listOf(Part("(当前时间:${dateTimeFormat.format(System.currentTimeMillis())})",null,null,null)),ROLE_USER)
            contents.add(timeContent)
        }
        if(add){
            val realText = if(BotApp.getInstance().isWithSpeaker){message.questionWithSpeaker()}else {message.question.message}
            val newContent = Content(listOf(Part(realText,null,null,null)),ROLE_USER)
            contents.add(newContent)
            FileLogger.i(TAG, "callApi: ${newContent.parts.firstOrNull()?.text}")
        }else if(contents.isNotEmpty()){
            FileLogger.i(TAG, "callApi: ${contents.last().parts.firstOrNull()?.functionResponse.toString()}")
        }
        return requestChatCompletions(message)
    }

    override fun requestChatSummarize() {
        NekoChatService.getInstance().addLogcat("requestChatSummarize:length")
        val question = Message("SYSTEM", "使用不超过50字总结对话", System.currentTimeMillis())
        val summarizeMessage = GeminiAIAskAble(
            BotApp.getInstance().getPackageName(),
            question
        ) { message ->
            clearContext()
            addContent(Content(arrayListOf(Part(message.answer.message, null,null,"")), ROLE_MODEL))
            NekoChatService.getInstance()
                .addLogcat("requestChatSummarize:" + message.getAnswer().getMessage())
            NekoChatService.getInstance().onReplySuccess(message)
        }
        summarizeMessage.handle()
    }

    override fun requestChatCompletions(message: NetAiAskAble): Boolean {
        val requestBody: RequestBody = dataAdapter.toJson(data).toRequestBody("application/json;charset=utf-8".toMediaType())
        val request = Request.Builder().post(requestBody)
            .url("$chatApi/models/${currentModel.str}:generateContent")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", BotApp.getInstance().apiKey)
            .addHeader("User-Agent", "Android Application <Chtholly>")
            .build()
        okHttpClient.newCall(request).enqueue(message)
        return true
    }

    fun addToolResponse(name:String, key: String, result: Any, thoughtSignature: String?) {
        addContent(
            Content(
                listOf(
                    Part(
                        null, null, FunctionResponse(
                            name, mapOf(Pair(key, result))
                        ), thoughtSignature
                    )
                ), ROLE_USER
            )
        )
    }

    fun addToolErr(name:String, e: Exception, thoughtSignature: String?) {
        addContent(
            Content(
                listOf(
                    Part(
                        null, null, FunctionResponse(
                            name, mapOf(Pair("err", e.message.toString()))
                        ), thoughtSignature
                    )
                ), ROLE_USER
            )
        )
    }

    companion object {
        private const val TAG = "GeminiSession"

        private const val CONTENTS = "contents"

        private const val ROLE_SYSTEM = "system_instruction"
        private const val ROLE_MODEL = "model"
        const val ROLE_USER = "user"

        const val MODEL_GEMINI_3_FL_PRE: String = "gemini-3.1-flash-lite-preview"
        const val MODEL_GEMINI_3_PRO_PRE: String = "gemini-3.1-pro-preview"

        private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
        private val dateTimeFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA)
        @JvmStatic
        var instance: GeminiSession? = null
            get() {
                if (field == null) {
                    field = GeminiSession(BotApp.getInstance().chatUrl)
                }
                return field
            }
            private set
    }
}
