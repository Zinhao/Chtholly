package com.zinhao.chtholly.session

import android.util.Log
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.db.MessageDao
import com.zinhao.chtholly.entity.GeminiAIAskAble
import com.zinhao.chtholly.entity.Message
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.LoggingInterceptor
import com.zinhao.chtholly.network.gemini.Content
import com.zinhao.chtholly.network.gemini.Part
import com.zinhao.chtholly.network.GEMINI_TOOLS
import com.zinhao.chtholly.network.Tool
import com.zinhao.chtholly.network.gemini.FunctionResponse
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
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiSession private constructor(private var chatApi: String?) : NekoSession(),
    RemoteChatApiSession {
    private val data: JSONObject = JSONObject()
    private var currentModel: RemoteModel = RemoteModel(MODEL_GEMINI_3_FL_PRE)
    private var lastMessageTimeStamp: Long = 0
    private val modelList: List<RemoteModel> = arrayListOf(
        RemoteModel(MODEL_GEMINI_3_PRO_PRE),
        RemoteModel(MODEL_GEMINI_3_FL_PRE)
    )
    private var contents: MutableList<Content>  = arrayListOf()
    private val tools: MutableList<Tool>  = arrayListOf()
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(100, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS)
        .addInterceptor(LoggingInterceptor())
        .build()
    private val systemInstruction: JSONObject = JSONObject()

    init {
        contents.clear()
        tools.add(GEMINI_TOOLS)
        try {
            setAgentPrompt(BotApp.getInstance().aiSoul)

            data.put(ROLE_SYSTEM, systemInstruction)

            data.put(CONTENTS, contentsToJsonArray())
            data.put("tools", toolsToJsonArray())

            val t = JSONObject()
            t.put("thinkingLevel", "low")
            val generationConfigObj = JSONObject()
            generationConfigObj.put("thinkingConfig", t)
            data.put("generationConfig", generationConfigObj)
            loadChatHistory()
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    fun contentsToJsonArray(): JSONArray{
        val contentArr = JSONArray()
        for (content in contents){
            contentArr.put(JSONObject(contentToJson(content)))
        }
        return contentArr
    }

    fun contentToJson(content: Content): String {
        // 获取针对 Content 类的 Adapter
        val jsonAdapter = GeminiAIAskAble.moshi.adapter(Content::class.java)
        // 将对象转换为 JSON 字符串
        return jsonAdapter.toJson(content)
    }

    fun toolsToJsonArray(): JSONArray{
        val toolArr = JSONArray()
        val toolsAdapter = GeminiAIAskAble.moshi.adapter(Tool::class.java)
        for (tool in tools){
            toolArr.put(JSONObject(toolsAdapter.toJson(tool)))
        }
        return toolArr
    }

    override fun setAgentPrompt(charaDesc: String) {
        val partsArray = JSONArray()
        val st = JSONObject()
        val agentSys = charaDesc.replace("\$name", BotApp.getInstance().getBotName())

        try {
            st.put("text", agentSys)
            partsArray.put(st)

            systemInstruction.put("parts", partsArray)
        } catch (e: JSONException) {
            Log.d(TAG, "setChara: failed.")
        }
    }

    override fun getAgentPrompt(): String {
        try {
            return systemInstruction.optString(CONTENTS)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    override fun getContextChat(): String {
        return data.getJSONArray(CONTENTS).toString()
    }

    override fun clearContext():Int {
        val len = contents.size
        contents.clear()
        return len
    }

    override fun loadChatHistory(){
        contents.clear()
        BotApp.getInstance().loadMessage(MessageDao.MessageGetAllListener { result ->
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
        data.put(CONTENTS, contentsToJsonArray())
        return requestChatCompletions(message)
    }

    override fun requestChatSummarize() {
        NekoChatService.getInstance().addLogcat("requestChatSummarize:length")
        val question = Message("SYSTEM", "使用不超过50字总结对话", System.currentTimeMillis())
        val summarizeMessage = GeminiAIAskAble(
            BotApp.getInstance().getPackageName(),
            question
        ) { message ->
            contents = arrayListOf()
            addContent(Content(arrayListOf(Part(message.answer.message, null,null,"")), ROLE_MODEL))
            NekoChatService.getInstance()
                .addLogcat("requestChatSummarize:" + message.getAnswer().getMessage())
            NekoChatService.getInstance().onReplySuccess(message)
        }
        summarizeMessage.handle()
    }

    override fun requestChatCompletions(message: NetAiAskAble): Boolean {
        val requestBody: RequestBody = data.toString().toRequestBody("application/json;charset=utf-8".toMediaType())
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
