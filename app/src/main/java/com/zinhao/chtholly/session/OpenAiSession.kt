package com.zinhao.chtholly.session

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.db.MessageDao
import com.zinhao.chtholly.entity.Message
import com.zinhao.chtholly.entity.NekoReply
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.LoggingInterceptor
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.dispatchToolCall
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.tools.MutedUserTool
import com.zinhao.chtholly.network.openai.*
import com.zinhao.chtholly.network.OPENAI_TOOLS
import com.zinhao.chtholly.session.RemoteChatApiSession.RemoteModel
import com.zinhao.chtholly.utils.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OpenAiSession private constructor(private val chatUrl: String) : NekoSession(), RemoteChatApiSession, ToolCallback {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val okHttpClient: OkHttpClient =OkHttpClient.Builder()
        .callTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(100, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS) //                .sslSocketFactory()
        .addInterceptor(LoggingInterceptor())
        .build()

    private var currentModel: RemoteModel
    private val modelList: MutableList<RemoteModel> = ArrayList<RemoteModel>()
    private var systemPrompt = ""
    private val summarizePrompt = "使用不超过50字总结场景对话"
    private var charaDesc = ""

    private val rolePlayPrompt = "根据场景对话 补充人物对话与聊天意愿数值"
    var roleplayMode = true

    private val roleMessageList = ArrayList<Message>()
    private val contextMessageList = ArrayList<ChatMessage>()


    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @OptIn(ExperimentalStdlibApi::class)
    val schemAdapter: JsonAdapter<Schem> =
        moshi.adapter<Schem>()

    @OptIn(ExperimentalStdlibApi::class)
    val nekoReplyAdapter: JsonAdapter<NekoReply> =
        moshi.adapter<NekoReply>()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(chatUrl) // LM Studio / OpenAI 兼容
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .callbackExecutor(Executors.newSingleThreadExecutor())
        .client(okHttpClient)
        .build()


    val api = retrofit.create(OpenAiApi::class.java)

    private val gemmaUncensored = "gemma-4-e4b-uncensored-hauhaucs-aggressive"

    private val qwen3Nsfw = "qwen3-vl-8b-nsfw-caption-v4.5"
    private val qwen3p5_9b_uncensored = "qwen3.5-9b-uncensored-hauhaucs-aggressive"
    private val qwen3p5_4b_uncensored = "qwen3.5-4b-uncensored-hauhaucs-aggressive"
    private val qwen3p5_4b_nsfw_ara_i1 = "qwen3.5-4b-nsfw-ara-heretic-literotica-i1"

    private val mimo_2p6_flash = "mimo-v2.6-flash"
    private val mimo_2p6_pro = "mimo-v2.6-pro"

    init {
        modelList.add(RemoteModel(qwen3p5_4b_uncensored))
        modelList.add(RemoteModel(mimo_2p6_flash))
        modelList.add(RemoteModel(mimo_2p6_pro))
        modelList.add(RemoteModel(qwen3p5_9b_uncensored))
        modelList.add(RemoteModel(qwen3p5_4b_nsfw_ara_i1))
        currentModel = modelList.get(0)

        scope.launch {
            rolePlayResponseFormat = loadResponseFormat("neko_schem.json")
            rolePlayResponseFormat?.let { rp ->
                rp.json_schema?.schema?.properties?.let {
                    it.replyMessage.description = "${BotApp.getInstance().botName}将要说的话，不要描写动作，仅话语"
                    it.willingnessToChat!!.description =
                        "聊天意愿数值，这个数值决定${BotApp.getInstance().botName}" +
                                "后续继续聊天，取值范围 1 到 100，数值越高表示越愿意聊天"
                }
            }
        }
    }

    private var rolePlayResponseFormat: ResponseFormat? = null
    private val noneResponseFormat: ResponseFormat? = null
    fun loadResponseFormat(path: String):ResponseFormat {
        val schemString = readTextFromAssetsSimplified(BotApp.getInstance(),path)
        schemString?.let {
            val schemObject = schemAdapter.fromJson(it)
            return ResponseFormat(
                JsonSchema("chat_reply", schemObject!!, true),
                "json_schema"
            )
        }
        throw IOException()
    }

    /**
     * 使用 Kotlin 扩展函数和更简洁的写法
     */
    fun readTextFromAssetsSimplified(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun setAgentPrompt(charaDesc: String) {
        if(roleplayMode){
            val botName = BotApp.getInstance().botName
            this.charaDesc = charaDesc.replace("\$name",botName)
        }else{
            systemPrompt = charaDesc
        }
    }

    override fun getAgentPrompt(): String {
        if(roleplayMode){
            return charaDesc
        }else{
            return systemPrompt
        }
    }

    override fun getContextChat(): String {
        //todo 导出当前聊天记录
        if(roleplayMode){
            return roleMessageList.toString()
        }else{
            return contextMessageList.toString()
        }
    }

    override fun clearContext(): Int {
        if(roleplayMode){
            val len = roleMessageList.size
            roleMessageList.clear()
            return len
        }else{
            val len = contextMessageList.size
            contextMessageList.clear()
            return len
        }
    }

    override fun loadChatHistory() {
        clearContext()
        FileLogger.i(TAG, "loadChatHistory start" )
        BotApp.getInstance().getLastTenMessages(MessageDao.MessageGetAllListener { result ->
            if(result.isEmpty()){return@MessageGetAllListener}
            val intoContentMessage = arrayListOf<Message>()
            if(result.size <= 10){
                intoContentMessage.addAll(result)
            }else{
                intoContentMessage.addAll(result.subList(result.size-10, result.size-1))
            }
            intoContentMessage.forEach { FileLogger.i(TAG, "loadChatHistory: ${it.speaker}: \"${it.message}\"") }
            if(roleplayMode){
                roleMessageList.addAll(intoContentMessage)
                roleMessageList.add(Message(null,
                    "现在时间:${dateTimeFormat.format(System.currentTimeMillis())}",
                    System.currentTimeMillis(),))
            }else{
                intoContentMessage.forEach {
                    if(it.message.isNotBlank()){
                        val role = if(it.speaker== BotApp.getInstance().botName)ROLE_ASSISTANT else ROLE_USER
                        val his = it.message.toChatMessage(role)
                        contextMessageList.add(his)
                    }
                }
            }

        })
    }

    override fun summarize(): Int {
        //todo 总结
        return 0
    }

    override fun setModelIndex(index: Int) {
        if (index >= 0 && index < modelList.size) {
            this.currentModel = modelList.get(index)
        }
    }

    override fun getCurrentModel(): RemoteModel {
        return currentModel
    }

    override fun getModelList(): MutableList<RemoteModel> {
        return modelList
    }

    fun addContent(message: Message) {

        roleMessageList.add(message)
    }

    // Tool call state
    private val pendingToolResults = mutableListOf<ChatMessage>()
    private val pendingToolCallIdMap = mutableMapOf<String, String>()

    override fun addToolResponse(name: String, key: String, result: Any, thoughtSignature: String?) {
        pendingToolResults.add(
            ChatMessage(
                role = ROLE_TOOL,
                content = listOf(ContentPart.TextPart(type = "text", text = result.toString())),
                tool_call_id = pendingToolCallIdMap[name]
            )
        )
    }

    override fun addToolErr(name: String, e: Exception, thoughtSignature: String?) {
        pendingToolResults.add(
            ChatMessage(
                role = ROLE_TOOL,
                content = listOf(ContentPart.TextPart(type = "text", text = "Error: ${e.message}")),
                tool_call_id = pendingToolCallIdMap[name]
            )
        )
    }

    private fun parseToolArgs(arguments: String): Map<String, Any> {
        return try {
            val json = JSONObject(arguments)
            val map = mutableMapOf<String, Any>()
            json.keys().forEach { key -> map[key] = json.get(key) }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private var lastMessageTime = System.currentTimeMillis()
    private var lastPostTime = 0L

    @Throws(JSONException::class)
    override fun callApi(message: NetAiAskAble, add: Boolean): Boolean {
        if (isTooFrequent()) {
            return false
        }
        return if(roleplayMode){
            rolePlayChatCompletions(message)
        }else{
            defaultChatCompletions(message)
        }
    }

    /**
     * 判断距离上一条用户消息是否超过10分钟
     */
    private fun isGapTooLong(): Boolean {
        val tenMinutes = 10 * 60 * 1000L
        return System.currentTimeMillis() - lastMessageTime > tenMinutes
    }

    /**
     * 判断是否发送请求太频繁（5秒内）
     */
    private fun isTooFrequent(): Boolean {
        return System.currentTimeMillis() - lastPostTime < 2000L
    }

    /**
     * 生成一条“时间间隔”提示消息，例如“过去了 1小时10分钟”
     */
    private fun buildTimeGapMessage(): Message {
        val gapMillis = System.currentTimeMillis() - lastPostTime
        val gapText = formatDuration(gapMillis)

        lastMessageTime = System.currentTimeMillis()  // 更新时间戳

        return Message(null, "过去了 $gapText", System.currentTimeMillis())
    }

    /**
     * 把毫秒数格式化为“X小时X分钟”或“X分钟”
     */
    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟"
            else -> "不到1分钟"
        }
    }

    override fun requestChatSummarize() {

    }

    private val sb = StringBuilder()

    private fun warpRolePlayPrompt(): String{
        sb.clear()
        sb.append(charaDesc).append("\n").append("\n")
        roleMessageList.forEach {
            if(it.speaker!=null){
                FileLogger.i(TAG, "${it.speaker?:""}: \"${it.message}\"")
                sb.append(it.speaker).append(": ").append("\"").append(it.message).append("\"").append("\n")
            }else{
                FileLogger.i(TAG, "${it.message}")
                sb.append(it.message).append("\n")
            }
        }
        sb.append("\n")
        sb.append("接下来${BotApp.getInstance().botName}会说什么？")
        return sb.toString()
    }

    private fun defaultChatCompletions(message: NetAiAskAble): Boolean {
        if(message.question.message.isNotBlank()){
            val last = message.question.message.toChatMessage(ROLE_USER)
            contextMessageList.add(last)
        }else{
            FileLogger.e(TAG,"不允许空消息",NullPointerException("content must not be null"))
            return false
        }


        scope.launch {
            try {
                // Use streaming if callback is set
                if (message.streamCallback != null) {
                    chatCompletionStream(
                        message = message,
                        chatMessageList = contextMessageList,
                        prompt = systemPrompt,
                        model = currentModel.str,
                        responseFormat = noneResponseFormat,
                    )
                    return@launch
                }

                var normalResult = chatCompletion(
                    prompt = systemPrompt,
                    chatMessageList = contextMessageList,
                    model = currentModel.str,
                    responseFormat = noneResponseFormat,
                )

                // Tool call loop
                while (normalResult != null && !normalResult.tool_calls.isNullOrEmpty()) {
                    // Append assistant's tool_calls message to context
                    contextMessageList.add(normalResult)

                    // Dispatch each tool call
                    pendingToolResults.clear()
                    pendingToolCallIdMap.clear()

                    for (toolCall in normalResult.tool_calls) {
                        val argsMap = parseToolArgs(toolCall.function.arguments)
                        val functionCall = FunctionCall(toolCall.function.name, argsMap)
                        pendingToolCallIdMap[toolCall.function.name] = toolCall.id

                        if (!message.question.isEnableCommand && toolCall.function.name != MutedUserTool.name) {
                            addToolErr(toolCall.function.name, Exception("Insufficient permissions"))
                        } else {
                            FileLogger.i(TAG, "${toolCall.function.name}: ${toolCall.function.arguments}")
                            dispatchToolCall(toolCall.function.name, functionCall, this@OpenAiSession, message)
                        }
                    }

                    // Append all tool results to context
                    contextMessageList.addAll(pendingToolResults)

                    // Re-call API with tool results
                    normalResult = chatCompletion(
                        prompt = systemPrompt,
                        chatMessageList = contextMessageList,
                        model = currentModel.str,
                        responseFormat = noneResponseFormat,
                    )
                }

                // Normal text response
                normalResult?.let {
                    val text = if(it.content is ContentPart.TextPart){
                        it.content.text
                    } else{
                        it.content.toString()
                    }
                    message.saveToDatabase(text)
                    contextMessageList.add(it)
                    message.isReplyReady = true
                    message.delayReplyCallback.onReplySuccess(message)
                }
            }catch (e: Exception){
                FileLogger.e(TAG,e.localizedMessage?:e.javaClass.name,e)
            }finally {

            }
        }
        return true
    }


    override fun rolePlayChatCompletions(message: NetAiAskAble): Boolean {
        // 如果距离上一条用户消息超过10分钟，插入一条时间提示
        if (isGapTooLong()) {
            val timeMessage = buildTimeGapMessage()
            roleMessageList.add(timeMessage)
        }
        roleMessageList.add(message.question)

        val chatMessageList = listOf(warpRolePlayPrompt().toChatMessage(ROLE_USER))

        scope.launch {
            try {
                val roleplayResult = chatCompletion(
                    prompt = rolePlayPrompt,
                    chatMessageList = chatMessageList,
                    model = currentModel.str,
                    maxCompletionTokens = 80,
                    responseFormat = rolePlayResponseFormat,
                )
                val nekoSchemStr = roleplayResult!!.content.toString()
                val nekoReply = nekoReplyAdapter.fromJson(nekoSchemStr)
                nekoReply?.let {
                    lastNekoReply = nekoReply
                    FileLogger.d(TAG, "nekoReply: $nekoSchemStr")
                    message.saveToDatabase(it.replyMessage)
                    roleMessageList.add(message.answer)
                    message.isReplyReady = true
                    message.delayReplyCallback.onReplySuccess(message)
                }
            }catch (e: Exception){
                FileLogger.e(TAG,e.localizedMessage?:e.javaClass.name,e)
            }finally {

            }
        }
        return true
    }

    private val maxTotalToken = 8192

    private suspend fun chatCompletion(
        imageBase64: String? = null,
        chatMessageList: List<ChatMessage>,
        prompt: String = "Describe this image in two sentences",
        model: String = modelList[0].str,
        maxCompletionTokens: Int = maxTotalToken,
        temperature: Double = 1.05,
        responseFormat: ResponseFormat? = null): ChatMessage?
    {

        val messages = arrayListOf<ChatMessage>()
        prompt.toChatMessage(ROLE_SYSTEM).let { messages.add(it) }
        messages.addAll(chatMessageList)
        imageBase64?.let {
            messages.add(ChatMessage(
                role = "user",
                content = listOf(
                    ContentPart.ImagePart(
                        type = "image_url",
                        image_url = ImageUrl("data:image/png;base64,$imageBase64")
                    )
                )
            ))
        }

        try {
            val request = ChatRequest(
                model = model,
                messages = messages,
                max_completion_tokens = maxCompletionTokens,
                reasoning_effort = "none",
                temperature = temperature,
                response_format = responseFormat,
                tools = if (responseFormat == null) OPENAI_TOOLS else null
            )
            lastPostTime = System.currentTimeMillis()
            val response = api.chatCompletion(
                authorization = "Bearer ${BotApp.getInstance().apiKey}",
                request = request
            )
            val usage = response.usage

            FileLogger.i(TAG, "prompt_tokens: ${usage.prompt_tokens}")
            FileLogger.i(TAG, "completion_tokens: ${usage.completion_tokens}")
            FileLogger.i(TAG, "total_tokens: ${usage.total_tokens}")

            if(usage.total_tokens >= maxTotalToken && roleMessageList.isNotEmpty()) {
                roleMessageList.removeAt(0)
            }

            return response.choices.firstOrNull()?.message
        } catch (e: Exception) {
            FileLogger.e(TAG,"请求失败：${e.localizedMessage}")
        } finally {

        }
        return null
    }

    private suspend fun chatCompletionStream(
        message: NetAiAskAble,
        chatMessageList: List<ChatMessage>,
        prompt: String = "Describe this image in two sentences",
        model: String = modelList[0].str,
        maxCompletionTokens: Int = maxTotalToken,
        temperature: Double = 1.05,
        responseFormat: ResponseFormat? = null
    ) {
        val messages = arrayListOf<ChatMessage>()
        messages.add(prompt.toChatMessage(ROLE_SYSTEM))
        messages.addAll(chatMessageList)
        val chatRequest = ChatRequest(
            model = model,
            messages = messages,
            max_completion_tokens = maxCompletionTokens,
            reasoning_effort = "none",
            temperature = temperature,
            stream = true,
            response_format = responseFormat,
            tools = if (responseFormat == null) OPENAI_TOOLS else null
        )

        try {
            lastPostTime = System.currentTimeMillis()

            val channel = Channel<String>(Channel.BUFFERED)
            val call = api.chatCompletionStream(
                authorization = "Bearer ${BotApp.getInstance().apiKey}",
                request = chatRequest
            )
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    try {
                        if (!response.isSuccessful) {
                            channel.close(IOException("HTTP ${response.code()}"))
                            return
                        }
                        val body = response.body() ?: run {
                            channel.close(IOException("Empty body"))
                            return
                        }
                        val source = body.source()
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (line.startsWith("data: ")) {
                                channel.trySend(line)
                            }
                        }
                        channel.close()
                    } catch (e: Exception) {
                        FileLogger.e(TAG, e.message.toString(),e)
                        channel.close(e)
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    FileLogger.e(TAG, "Stream onFailure: ${t.javaClass.simpleName}: ${t.message}")
                    channel.close(t as? Exception ?: IOException(t.message))
                }
            })

            val accumulated = StringBuilder()
            val streamAdapter = moshi.adapter(StreamChunk::class.java)
            message.streamCallback?.onStreamStart(message)

            // Tool call accumulators: index -> (id, name, arguments)
            val toolCallAccumulators = mutableMapOf<Int, Triple<StringBuilder, StringBuilder, StringBuilder>>()

            for (line in channel) {
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                try {
                    val chunk = streamAdapter.fromJson(data)
                    val choice = chunk?.choices?.firstOrNull()
                    val delta = choice?.delta

                    // Handle text content
                    delta?.content?.let { content ->
                        accumulated.append(content)
                        message.streamCallback?.onStreamChunk(message, accumulated.toString())
                    }

                    // Handle tool calls
                    delta?.tool_calls?.forEach { tc ->
                        val idx = tc.index ?: 0
                        val acc = toolCallAccumulators.getOrPut(idx) {
                            Triple(StringBuilder(), StringBuilder(), StringBuilder())
                        }
                        tc.id?.let { acc.first.append(it) }
                        tc.function?.name?.let { acc.second.append(it) }
                        tc.function?.arguments?.let { acc.third.append(it) }
                    }
                } catch (e: Exception) {
                    FileLogger.e(TAG, "Stream parse error: ${e.message}")
                }
            }

            // After stream: if tool calls were received, execute them
            if (toolCallAccumulators.isNotEmpty()) {
                FileLogger.i(TAG,"tool calls — loop")
                // Save any text content that preceded the tool calls
                if (accumulated.isNotBlank()) {
                    contextMessageList.add(accumulated.toString().toChatMessage(ROLE_ASSISTANT))
                }

                // Build ToolCall list from accumulators
                val toolCalls = toolCallAccumulators.entries.sortedBy { it.key }.map { (_, acc) ->
                    ToolCall(
                        id = acc.first.toString(),
                        function = ToolCallFunction(
                            name = acc.second.toString(),
                            arguments = acc.third.toString()
                        )
                    )
                }

                // Add assistant message with tool_calls to context
                contextMessageList.add(ChatMessage(
                    role = ROLE_ASSISTANT,
                    content = null,
                    tool_calls = toolCalls
                ))

                // Dispatch each tool call
                pendingToolResults.clear()
                pendingToolCallIdMap.clear()
                for (toolCall in toolCalls) {
                    val argsMap = parseToolArgs(toolCall.function.arguments)
                    val functionCall = FunctionCall(toolCall.function.name, argsMap)
                    pendingToolCallIdMap[toolCall.function.name] = toolCall.id
                    if (!message.question.isEnableCommand && toolCall.function.name != MutedUserTool.name) {
                        val e = Exception("Insufficient permissions")
                        FileLogger.e(TAG, toolCall.function.name,e)
                        addToolErr(toolCall.function.name, e)
                    } else {
                        FileLogger.i(TAG, "Stream tool call: ${toolCall.function.name}: ${toolCall.function.arguments}")
                        dispatchToolCall(toolCall.function.name, functionCall, this@OpenAiSession, message)
                    }
                }

                // Append tool results to context
                contextMessageList.addAll(pendingToolResults)

                // Re-call API (non-streaming for the follow-up to handle nested tool calls)
                val followUpResult = chatCompletion(
                    prompt = prompt,
                    chatMessageList = contextMessageList,
                    model = model,
                    maxCompletionTokens = maxCompletionTokens,
                    responseFormat = responseFormat,
                )
                FileLogger.i(TAG, followUpResult.toString())
                // Handle follow-up response (may contain more tool calls)
                var current = followUpResult
                while (current != null && !current.tool_calls.isNullOrEmpty()) {
                    contextMessageList.add(current)
                    pendingToolResults.clear()
                    pendingToolCallIdMap.clear()
                    for (tc in current.tool_calls) {
                        val argsMap = parseToolArgs(tc.function.arguments)
                        val functionCall = FunctionCall(tc.function.name, argsMap)
                        pendingToolCallIdMap[tc.function.name] = tc.id
                        if (!message.question.isEnableCommand && tc.function.name != MutedUserTool.name) {
                            addToolErr(tc.function.name, Exception("Insufficient permissions"))
                        } else {
                            dispatchToolCall(tc.function.name, functionCall, this@OpenAiSession, message)
                        }
                    }
                    contextMessageList.addAll(pendingToolResults)
                    current = chatCompletion(
                        prompt = prompt,
                        chatMessageList = contextMessageList,
                        model = model,
                        maxCompletionTokens = maxCompletionTokens,
                        responseFormat = responseFormat,
                    )
                }

                // Final text response from tool call follow-up
                current?.let {
                    val text = if (it.content is ContentPart.TextPart) {
                        it.content.text
                    } else {
                        it.content.toString()
                    }
                    message.saveToDatabase(text)
                    contextMessageList.add(it)
                    message.streamCallback?.onStreamChunk(message, text)
                }
                message.streamCallback?.onStreamComplete(message)
            } else {
                FileLogger.i(TAG,"No tool calls — normal text response")
                // No tool calls — normal text response
                accumulated.toString().let {
                    if (it.isNotBlank()) {
                        contextMessageList.add(it.toChatMessage(ROLE_ASSISTANT))
                    }
                }
                message.streamCallback?.onStreamComplete(message)
            }

        } catch (e: Exception) {
            FileLogger.e(TAG, "Stream request failed: ${e.javaClass.simpleName}: ${e.message}")
            message.streamCallback?.onStreamError(message, e)
        }
    }

    /**
     * call form roleplay mode
     */
    private var lastNekoReply: NekoReply? = null
    fun wantToTalk(): Boolean{
        if(lastNekoReply != null){
            return lastNekoReply!!.willingnessToChat > 70
        }
        return false
    }

    fun String.toChatMessage(role: String): ChatMessage{
        return ChatMessage(
            role,listOf(
                ContentPart.TextPart(
                    type = "text",
                    text = this
                )
            )
        )
    }

    companion object {
        private const val TAG = "OpenAiSession"

        private const val ROLE_SYSTEM = "system"
        private const val ROLE_ASSISTANT = "assistant"
        private const val ROLE_USER = "user"
        private const val ROLE_TOOL = "tool"

        const val MODEL_GPT_3_5_TURBO: String = "gpt-3.5-turbo"
        const val MODEL_GPT_4_TURBO: String = "gpt-4-turbo"
        const val MODEL_GPT_4O_MINI: String = "gpt-4o-mini"
        const val MODEL_GPT_4O: String = "gpt-4o"



        private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
        private val dateTimeFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA)
        @JvmStatic
        var instance: OpenAiSession? = null
            get() {
                if (field == null) {
                    field = OpenAiSession(BotApp.getInstance().chatUrl)
                    field?.roleplayMode = BotApp.getInstance().isRoleplay
                }
                return field
            }
            private set
    }
}
