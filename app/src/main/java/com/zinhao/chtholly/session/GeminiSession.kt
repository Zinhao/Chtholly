package com.zinhao.chtholly.session

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.db.MessageDao
import com.zinhao.chtholly.entity.GeminiAIAskAble
import com.zinhao.chtholly.entity.Message
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.GEMINI_TOOLS
import com.zinhao.chtholly.network.LoggingInterceptor
import com.zinhao.chtholly.network.Tool
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.gemini.*
import com.zinhao.chtholly.session.RemoteChatApiSession.RemoteModel
import com.zinhao.chtholly.utils.AsyncHelper
import com.zinhao.chtholly.utils.FileLogger
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class GeminiSession private constructor(chatApi: String) : NekoSession(),
    RemoteChatApiSession, ToolCallback {
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

    /** When true, use the Interactions API; when false, use the legacy generateContent API. */
    var useInteractionsApi: Boolean = true

    /** Server-side interaction ID for stateful multi-turn with the Interactions API. */
    private var previousInteractionId: String? = null

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(100, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS)
        .addInterceptor(LoggingInterceptor())
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "Android Application <Chtholly>")
                .build()
            chain.proceed(request)
        }
        .build()

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val dataAdapter: JsonAdapter<PostRequest> = moshi.adapter(PostRequest::class.java)

    // Retrofit2
    private val baseUrl: String = if (chatApi.endsWith("/")) chatApi else "$chatApi/"
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(okHttpClient)
        .build()
    private val geminiApi: GeminiApi = retrofit.create(GeminiApi::class.java)

    init {
        loadChatHistory()
        summarizeChatAgent = SummarizeChatAgent(chatApi, BotApp.getInstance().apiKey)
        systemInstruction = SystemInstruction(listOf(Part(BotApp.getInstance().aiSoul,
            null,null,null)))

        tools.add(GEMINI_TOOLS)
        data = PostRequest(tools,
            systemInstruction,
            GenerationConfig(ThinkingConfig("low"))
            ,contents)
    }

    override fun setAgentPrompt(charaDesc: String) {
        val agentSys = charaDesc.replace("\$name", BotApp.getInstance().getAtBotName())
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
        previousInteractionId = null
        return len
    }

    override fun removeFromContext(message: Message?) {
        val index = contents.indexOfLast {
            it.role == ROLE_USER && it.parts?.firstOrNull()?.text == message?.message
        }
        if(index >= 0) {
            contents.removeAt(index)
        }
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
                if(BotApp.getInstance().atBotName == message.speaker){
                    role = ROLE_MODEL
                }else{
                    role = ROLE_USER
                }
                val content = Content(listOf(Part(messageContent,null,null,null)),role)
                hisContent.add(content)
            }
            contents.addAll(hisContent)
        })
    }

    override fun summarize(): Int {
        return 0
    }

    override fun setModelIndex(modelIndex: Int) {
        TODO("Not yet implemented")
    }

    override fun getCurrentModel(): RemoteModel? {
        TODO("Not yet implemented")
    }

    override fun getModelList(): List<RemoteModel?>? {
        TODO("Not yet implemented")
    }

    fun addContent(content: Content){
        contents.add(content)
    }

    override fun callApi(message: NetAiAskAble, add: Boolean): Boolean {
        if(System.currentTimeMillis() - lastMessageTimeStamp > 5 * 60 * 1000){
            lastMessageTimeStamp = System.currentTimeMillis()
            val timeContent = Content(listOf(Part("(当前时间:${dateTimeFormat.format(System.currentTimeMillis())})",null,null,null)),ROLE_USER)
            contents.add(timeContent)
        }
        if(add){
            val realText = if(BotApp.getInstance().isWithSpeaker){message.questionWithSpeaker()}else {message.question.message}
            val newContent = Content(listOf(Part(realText,null,null,null)),ROLE_USER)
            contents.add(newContent)
            FileLogger.i(TAG, "main callApi: $realText")
        }else if(contents.isNotEmpty()){
            FileLogger.i(TAG, "sub callApi: ${contents.last().parts?.firstOrNull()?.functionResponse.toString()}")
        }

        if(contents.size > SUMMARIZE_SIZE){
            AsyncHelper.doAsyncPart {
                val contentToSummarize = contents.subList(0, SUMMARIZE_SIZE - SUMMARIZE_PIN).toList()

                val sumContent = summarizeChatAgent.requestSummarize(contentToSummarize)

                sumContent?.let { summarized ->
                    summarized.parts?.firstOrNull()?.text?.let { FileLogger.i(TAG,"summarizeResult:$it") }
                    val lastPinChats = contents.subList(SUMMARIZE_SIZE - SUMMARIZE_PIN, SUMMARIZE_SIZE).toList()

                    clearContext()

                    contents.add(summarized)
                    contents.addAll(lastPinChats)
                }
                rolePlayChatCompletions(message)
            }
        }else{
            rolePlayChatCompletions(message)
        }
        return true
    }

    override fun requestChatSummarize() {
        FileLogger.e(TAG, "requestChatSummarize:length")
    }

    // ──────────────────────────────────────────────────────────────
    //  generateContent mode (legacy)
    // ──────────────────────────────────────────────────────────────

    override fun rolePlayChatCompletions(message: NetAiAskAble): Boolean {
        if (useInteractionsApi) {
            return requestInteractionCompletions(message)
        }
        // Legacy generateContent path
        val call = geminiApi.generateContent(
            model = currentModel.str,
            apiKey = BotApp.getInstance().apiKey,
            body = data
        )
        call.enqueue(object : Callback<GeminiResponse> {
            override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                if (response.isSuccessful) {
                    val geminiResponse = response.body()
                    if (message is GeminiAIAskAble) {
                        message.handleGeminiResponse(geminiResponse)
                    }
                } else {
                    if (message is GeminiAIAskAble) {
                        message.handleApiError(response.code(), response.message())
                    }
                }
            }

            override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                if (message is GeminiAIAskAble) {
                    message.handleNetworkError(t)
                }
            }
        })
        return true
    }

    // ──────────────────────────────────────────────────────────────
    //  Interactions API mode
    // ──────────────────────────────────────────────────────────────

    /**
     * Convert the current session [contents] into a flat list of [InteractionStep]
     * that the Interactions API expects.
     */
    private fun contentsToInteractionSteps(): List<InteractionStep> {
        val steps = mutableListOf<InteractionStep>()
        for (content in contents) {
            val parts = content.parts ?: continue
            for (part in parts) {
                when {
                    part.functionCall != null -> {
                        steps.add(
                            InteractionStep(
                                type = "function_call",
                                name = part.functionCall.name,
                                arguments = part.functionCall.args,
                                thoughtSignature = part.thoughtSignature
                            )
                        )
                    }
                    part.functionResponse != null -> {
                        // Serialize response map to JSON string for the content field
                        val responseJson = try {
                            moshi.adapter(Map::class.java).toJson(part.functionResponse.response)
                        } catch (e: Exception) {
                            part.functionResponse.response.toString()
                        }
                        steps.add(
                            InteractionStep(
                                type = "function_result",
                                name = part.functionResponse.name,
                                content = listOf(InteractionContent(type = "text", text = responseJson)),
                                thoughtSignature = part.thoughtSignature
                            )
                        )
                    }
                    part.text != null -> {
                        val stepType = if (content.role == ROLE_MODEL) "model_output" else "user_input"
                        steps.add(
                            InteractionStep(
                                type = stepType,
                                content = listOf(InteractionContent(type = "text", text = part.text))
                            )
                        )
                    }
                }
            }
        }
        return steps
    }

    /**
     * Convert the current [tools] into the flat [InteractionTool] format
     * required by the Interactions API.
     */
    private fun toolsToInteractionTools(): List<InteractionTool> {
        val interactionTools = mutableListOf<InteractionTool>()
        for (tool in tools) {
            for (fd in tool.functionDeclarations) {
                interactionTools.add(
                    InteractionTool(
                        type = "function",
                        name = fd.name,
                        description = fd.description,
                        parameters = fd.parameters
                    )
                )
            }
        }
        return interactionTools
    }

    /**
     * Build an [InteractionRequest] from the current session state.
     */
    private fun buildInteractionRequest(): InteractionRequest {
        val systemText = data.systemInstruction.parts.firstOrNull()?.text
        val interactionSteps = contentsToInteractionSteps()
        val interactionTools = toolsToInteractionTools()
        val thinkingLevel = data.generationConfig.thinkingConfig.thinkingLevel

        return InteractionRequest(
            model = currentModel.str,
            input = interactionSteps,
            tools = interactionTools.ifEmpty { null },
            systemInstruction = systemText,
            generationConfig = InteractionGenerationConfig(thinkingLevel = thinkingLevel),
            store = true,
            previousInteractionId = previousInteractionId
        )
    }

    /**
     * Send the current conversation via the Interactions API.
     */
    private fun requestInteractionCompletions(message: NetAiAskAble): Boolean {
        val request = buildInteractionRequest()
        FileLogger.i(TAG, "requestInteractionCompletions: model=${request.model}, steps=${(request.input as? List<*>)?.size}, previousId=${request.previousInteractionId}")

        val call = geminiApi.createInteraction(
            apiKey = BotApp.getInstance().apiKey,
            body = request
        )
        call.enqueue(object : Callback<InteractionResponse> {
            override fun onResponse(call: Call<InteractionResponse>, response: Response<InteractionResponse>) {
                if (response.isSuccessful) {
                    val interactionResponse = response.body()
                    // Cache the interaction ID for server-side state on next turn
                    interactionResponse?.id?.let {
                        previousInteractionId = it
                        FileLogger.i(TAG, "interaction id cached: $it")
                    }
                    if (message is GeminiAIAskAble) {
                        message.handleInteractionResponse(interactionResponse)
                    }
                } else {
                    FileLogger.e(TAG, "interaction API error: ${response.code()} ${response.message()}")
                    if (message is GeminiAIAskAble) {
                        message.handleApiError(response.code(), response.message())
                    }
                }
            }

            override fun onFailure(call: Call<InteractionResponse>, t: Throwable) {
                FileLogger.e(TAG, "interaction API failure: ${t.message}")
                if (message is GeminiAIAskAble) {
                    message.handleNetworkError(t)
                }
            }
        })
        return true
    }

    override fun addToolResponse(name:String, key: String, result: Any, thoughtSignature: String?) {
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

    override fun addToolErr(name:String, e: Exception, thoughtSignature: String?) {
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

    suspend fun chatCompletion(): GeminiResponse? {
        return try {
            geminiApi.generateContentSuspend(
                model = currentModel.str,
                apiKey = BotApp.getInstance().apiKey,
                body = data
            )
        } catch (e: Exception) {
            Log.e(TAG, "chatCompletion error", e)
            null
        }
    }

    /**
     * Suspend variant using the Interactions API.
     */
    suspend fun interactionCompletion(): InteractionResponse? {
        return try {
            val request = buildInteractionRequest()
            val response = geminiApi.createInteractionSuspend(
                apiKey = BotApp.getInstance().apiKey,
                body = request
            )
            response.id?.let { previousInteractionId = it }
            response
        } catch (e: Exception) {
            Log.e(TAG, "interactionCompletion error", e)
            null
        }
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
