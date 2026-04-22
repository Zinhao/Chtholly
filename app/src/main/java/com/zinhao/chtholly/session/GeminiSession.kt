package com.zinhao.chtholly.session

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.db.MessageDao
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
import com.zinhao.chtholly.utils.AsyncHelper
import com.zinhao.chtholly.utils.FileLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class GeminiSession private constructor(private var chatApi: String) : NekoSession(),
    RemoteChatApiSession {
    private var data: PostRequest
    private val tools: MutableList<Tool>  = arrayListOf()
    private var systemInstruction: SystemInstruction
    private val contents: MutableList<Content>  = arrayListOf()
    private val summarizeChatAgent: SummarizeChatAgent

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
        summarizeChatAgent = SummarizeChatAgent(chatApi, BotApp.getInstance().apiKey)
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
            Log.d(TAG, "loadChatHistory: "+result.size)
            if(result.isEmpty()){return@MessageGetAllListener}
            val intoContentMessage = arrayListOf<Message>()
            if(result.size <= 10){
                intoContentMessage.addAll(result)
            }else{
                intoContentMessage.addAll(result.subList(result.size-10, result.size-1))
            }
            val hisContent = arrayListOf<Content>()
            for (message in intoContentMessage){
                val messageContent = "${message.speaker}:${message.message}"
                Log.d(TAG, "loadChatHistory: ${message.speaker}:${message.message.replace('\n', ' ')}")
                val role: String
                if(BotApp.getInstance().botName == message.speaker){
                    role = ROLE_MODEL
                }else{
                    role = ROLE_USER
                }
                val content = Content(listOf(Part(messageContent,null,null,null)),role)
                hisContent.add(content)
            }
            contents.addAll(0,hisContent)
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

        if(contents.size > SUMMARIZE_SIZE){
            AsyncHelper.doAsyncPart {
                // 使用 toList() 或 toMutableList() 立即创建内容的副本
                val contentToSummarize = contents.subList(0, SUMMARIZE_SIZE - SUMMARIZE_PIN).toList()

                val sumContent = summarizeChatAgent.requestSummarize(contentToSummarize)

                sumContent?.let { summarized ->
                    summarized.parts.firstOrNull()?.text?.let { FileLogger.i(TAG,"summarizeResult:$it") }
                    // 同样，先拷贝出最后部分，防止清除上下文后丢失
                    val lastPinChats = contents.subList(SUMMARIZE_SIZE - SUMMARIZE_PIN, SUMMARIZE_SIZE).toList()

                    clearContext() // 现在可以安全清除了

                    contents.add(summarized)
                    contents.addAll(lastPinChats)
                }
                requestChatCompletions(message)
            }
        }else{
            requestChatCompletions(message)
        }
        return true
    }

    override fun requestChatSummarize() {
        FileLogger.e(TAG, "requestChatSummarize:length")
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
        const val SUMMARIZE_SIZE = 36
        const val SUMMARIZE_PIN = 8
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
