package com.zinhao.chtholly.session

import android.util.Log
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.LoggingInterceptor
import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.entity.Message
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.entity.NetAiAskAble.DelayReplyCallback
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

class GeminiSession private constructor(private var chatUrl: String?) : NekoSession(), ChatSession {
    private val data: JSONObject
    private var chats: JSONArray
    private val okHttpClient: OkHttpClient
    private val systemInstruction: JSONObject

    init {
        okHttpClient = OkHttpClient.Builder()
            .callTimeout(100, TimeUnit.SECONDS)
            .writeTimeout(100, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
            .addInterceptor(LoggingInterceptor())
            .build()
        data = JSONObject()
        chats = JSONArray()
        systemInstruction = JSONObject()
        try {
            setChara(BotApp.getInstance().getCurrentCharacter().desc)

            data.put(ROLE_SYSTEM, systemInstruction)

            data.put("contents", chats)

            val t = JSONObject()
            t.put("thinkingLevel", "low")
            val generationConfigObj = JSONObject()
            generationConfigObj.put("thinkingConfig", t)
            data.put("generationConfig", generationConfigObj)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    fun setModel(model: String?) {
    }

    override fun setChara(charaDesc: String) {
        val partsArray = JSONArray()
        val st = JSONObject()
        val agentSys = charaDesc.replace("\$name", BotApp.getInstance().getBotName())

        try {
            st.put("text", agentSys)
            partsArray.put(st)

            systemInstruction.put(CONTENT, partsArray)
            Log.d(TAG, "setChara: " + chats.get(0))
        } catch (e: JSONException) {
            Log.d(TAG, "setChara: failed.")
        }
    }

    override fun getChara(): String {
        try {
            return systemInstruction.getString(CONTENT)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    override fun getContextChat(): String {
        return chats.toString()
    }

    override fun summarize(): Int {
        return -1
    }

    fun addAssistantChat(message: String?) {
    }

    fun addSystemChat(message: String?) {
    }

    fun addToolCallResult(content: JSONObject?, callId: String?) {
        val function_call_result_message = JSONObject()
        try {
            function_call_result_message.put(ROLE, ROLE_TOOL)
            function_call_result_message.put(CONTENT, content)
            function_call_result_message.put(TOOL_CALL_ID, callId)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
        chats.put(function_call_result_message)
    }

    private fun addTextChat(role: String, text: String?) {
        val newChat = JSONObject()
        val parts = JSONArray()
        try {
            val textObj = JSONObject()
            textObj.put("text", text)
            parts.put(textObj)

            newChat.put("role", role)
            newChat.put("parts", parts)
            Log.d(TAG, String.format(Locale.CHINA, "addChat: %s: %s", role, text))
        } catch (e: JSONException) {
            Log.e(TAG, String.format(Locale.CHINA, "addChat: %s: %s", role, text))
        }
        chats.put(newChat)
    }

    @Throws(JSONException::class)
    override fun startAsk(message: NetAiAskAble): Boolean {
        addTextChat(ROLE_USER, message.getQuestion().getMessage())
        data.put("contents", chats)
        return requestChatCompletions(message)
    }

    override fun setChatUrl(chatUrl: String?) {
        this.chatUrl = chatUrl
    }

    override fun getChatUrl(): String? {
        return chatUrl
    }

    override fun requestChatSummarize() {
        NekoChatService.getInstance().addLogcat("requestChatSummarize:length")
        val question = Message("SYSTEM", "使用不超过50字总结对话", System.currentTimeMillis())
        val summarizeMessage = NetAiAskAble(
            BotApp.getInstance().getPackageName(),
            question,
            object : DelayReplyCallback {
                override fun onReply(message: NetAiAskAble) {
                    chats = JSONArray()
                    chats.put(systemInstruction)
                    NekoChatService.getInstance()
                        .addLogcat("requestChatSummarize:" + message.getAnswer().getMessage())
                    addTextChat(ROLE_SYSTEM, message.getAnswer().getMessage())
                }
            })
        summarizeMessage.ask()
    }

    override fun requestChatCompletions(message: NetAiAskAble): Boolean {
        val requestBody: RequestBody = "application/json;charset=utf-8".toRequestBody()

        Log.d(TAG, "requestAsk: $data")
        val request = Request.Builder().post(requestBody)
            .url("$chatUrl/models/$MODEL_GEMINI_3_FL_PRE:generateContent")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", BotApp.getInstance().apiKey)
            .addHeader("User-Agent", "Android Application <Chtholly>")
            .build()
        okHttpClient.newCall(request).enqueue(message)
        return true
    }

    companion object {
        private const val TAG = "GeminiSession"

        private const val ROLE = "role"
        private const val CONTENT = "content"
        private const val TOOL_CALL_ID = "tool_call_id"

        private const val ROLE_SYSTEM = "system_instruction"
        private const val ROLE_MODEL = "model"
        private const val ROLE_USER = "user"
        private const val ROLE_TOOL = "tool"

        const val MODEL_GEMINI_3_FL_PRE: String = "gemini-3.1-flash-lite-preview"

        private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
        private val dateTimeFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA)
        @JvmStatic
        var instance: GeminiSession? = null
            get() {
                if (field == null) {
                    field = GeminiSession(BotApp.getInstance().getChatUrl())
                }
                return field
            }
            private set
    }
}
