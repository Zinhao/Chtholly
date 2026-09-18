package com.zinhao.chtholly.session

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.db.MessageDao
import com.zinhao.chtholly.entity.Choice
import com.zinhao.chtholly.entity.Message
import com.zinhao.chtholly.entity.NekoReply
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.LoggingInterceptor
import com.zinhao.chtholly.network.gemini.Content
import com.zinhao.chtholly.network.gemini.Part
import com.zinhao.chtholly.network.openai.*
import com.zinhao.chtholly.session.RemoteChatApiSession.RemoteModel
import com.zinhao.chtholly.utils.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class OpenAiSession private constructor(private val chatUrl: String) : NekoSession(), RemoteChatApiSession {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val okHttpClient: OkHttpClient =OkHttpClient.Builder()
        .callTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(100, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS) //                .sslSocketFactory()
        .addInterceptor(LoggingInterceptor())
        .build()

    private var currentModel: RemoteModel
    private val modelList: MutableList<RemoteModel> = ArrayList<RemoteModel>()
    private val systemPrompt = "根据场景对话 补充人物对话与聊天意愿数值"
    private val summarizePrompt = "使用不超过50字总结场景对话"
    private var charaDesc = ""

    private val chatList = ArrayList<Message>()


    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @OptIn(ExperimentalStdlibApi::class)
    val nekoSchemAdapter: JsonAdapter<NekoSchem> =
        moshi.adapter<NekoSchem>()

    @OptIn(ExperimentalStdlibApi::class)
    val nekoReplyAdapter: JsonAdapter<NekoReply> =
        moshi.adapter<NekoReply>()

    val retrofit = Retrofit.Builder()
        .baseUrl(chatUrl) // LM Studio / OpenAI 兼容
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(okHttpClient)
        .build()

    val api = retrofit.create(OpenAiApi::class.java)

    private val gemmaUncensored = "gemma-4-e4b-uncensored-hauhaucs-aggressive"

    private val qwen3Nsfw = "qwen3-vl-8b-nsfw-caption-v4.5"
    private val qwen3p5_9b_uncensored = "qwen3.5-9b-uncensored-hauhaucs-aggressive"
    private val qwen3p5_4b_uncensored = "qwen3.5-4b-uncensored-hauhaucs-aggressive"
    private val qwen3p5_4b_nsfw_ara_i1 = "qwen3.5-4b-nsfw-ara-heretic-literotica-i1"

    private val mimo2p5 = "mimo-v2.5"
    private val mimo2p5pro = "mimo-v2.5-pro"

    init {
        modelList.add(RemoteModel(mimo2p5))
        modelList.add(RemoteModel(mimo2p5pro))
//        modelList.add(RemoteModel(qwen3p5_9b_uncensored))
        modelList.add(RemoteModel(qwen3p5_4b_uncensored))
        modelList.add(RemoteModel(qwen3p5_4b_nsfw_ara_i1))
        currentModel = modelList.get(0)
        loadChatHistory()
        val botName = BotApp.getInstance().botName
        charaDesc = BotApp.getInstance().aiSoul.replace("\$name",botName)
        scope.launch {
            loadNekoSchem()
        }
    }

    fun loadNekoSchem(){
        readTextFromAssetsSimplified(BotApp.getInstance(),"neko_schem.json")?.let {
            nekoSchem = nekoSchemAdapter.fromJson(it)
            resFormat = ResponseFormat(
                JsonSchema("chat_reply", nekoSchem!!, true),
                "json_schema"
            )
        }
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
        val botName = BotApp.getInstance().botName
        this.charaDesc = charaDesc.replace("\$name",botName)
    }

    override fun getAgentPrompt(): String { return charaDesc }

    override fun getContextChat(): String {
        return chatList.toString()
    }

    override fun clearContext(): Int {
        val len = chatList.size
        chatList.clear()
        return len
    }

    override fun loadChatHistory() {
        clearContext()
        BotApp.getInstance().getLastTenMessages(MessageDao.MessageGetAllListener { result ->
            if(result.isEmpty()){return@MessageGetAllListener}
            val intoContentMessage = arrayListOf<Message>()
            if(result.size <= 10){
                intoContentMessage.addAll(result)
            }else{
                intoContentMessage.addAll(result.subList(result.size-10, result.size-1))
            }
            intoContentMessage.forEach { FileLogger.i(TAG, "loadChatHistory: ${it.speaker}: \"${it.message}\"") }
            chatList.addAll(intoContentMessage)
            chatList.add(Message(null,
                "现在时间:${dateTimeFormat.format(System.currentTimeMillis())}",
                System.currentTimeMillis(),))
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
        chatList.add(message)
    }

    fun addToolCalls(message: Choice.Message) {

    }

    fun addToolCallResult(content: JSONObject?, callId: String?) {}

    private var lastMessageTime = System.currentTimeMillis()
    private var lastPostTime = System.currentTimeMillis()

    @Throws(JSONException::class)
    override fun callApi(message: NetAiAskAble, add: Boolean): Boolean {
        // 如果距离上一条用户消息超过10分钟，插入一条时间提示
        if (isGapTooLong()) {
            val timeMessage = buildTimeGapMessage()
            chatList.add(timeMessage)
        }

        chatList.add(message.question)

        // 如果距离上次发送请求不足5秒，直接拒绝
        if (isTooFrequent()) {
            return false
        }
        return requestChatCompletions(message)
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
        val fiveSeconds = 5000L
        return System.currentTimeMillis() - lastPostTime < fiveSeconds
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

    override fun requestChatCompletions(message: NetAiAskAble): Boolean {
        sb.clear()
        sb.append(charaDesc).append("\n").append("\n")
        chatList.forEach {
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
        scope.launch {
            try {
                val nekoSchemStr = chatCompletion(
                    prompt = systemPrompt,
                    text = sb.toString(),
                    model = currentModel.str,
                    maxCompletionTokens = 80
                )
                val nekoReply = nekoReplyAdapter.fromJson(nekoSchemStr)
                nekoReply?.let {
                    lastNekoReply = nekoReply
                    FileLogger.d(TAG, "nekoReply: $nekoSchemStr")
                    message.saveToDatabase(it.replyMessage)
                    chatList.add(message.answer)
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

    private val maxTotalToken = 4096
    private var nekoSchem: NekoSchem? = null
    private var resFormat: ResponseFormat? = null
    private suspend fun chatCompletion(
        imageBase64: String? = null,
        text: String? = null,
        prompt: String = "Describe this image in two sentences",
        model: String = modelList[0].str,
        maxCompletionTokens: Int = 1024,
        temperature: Double = 1.05,
        jsonSchema: String? = null):String?
    {
        var targetFormat: ResponseFormat? = null
        if(jsonSchema != null){
            val schem = nekoSchemAdapter.fromJson(jsonSchema)
            val newFormat = ResponseFormat(
                JsonSchema("chat_reply", schem!!, true),
                "json_schema"
            )
            targetFormat = newFormat
        }else{
            if(resFormat == null){
                loadNekoSchem()
            }
            targetFormat = resFormat
        }
        targetFormat?.let { tf ->
            tf.json_schema.schema.properties?.let {
                it.replyMessage.description = "${BotApp.getInstance().botName}将要说的话，不要描写动作，仅话语"
                it.willingnessToChat!!.description =
                    "聊天意愿数值，这个数值决定${BotApp.getInstance().botName}" +
                            "后续继续聊天，取值范围 1 到 100，数值越高表示越愿意聊天"
            }

        }
        try {
            val request = ChatRequest(
                model = model,
                messages = listOf(
                    ChatMessage(
                        role = "developer",
                        content = listOf(
                            ContentPart.TextPart(
                                type = "text",
                                text = prompt
                            )
                        )
                    ),
                    ChatMessage(
                        role = "user",
                        content = listOfNotNull(
                            imageBase64?.let {
                                ContentPart.ImagePart(
                                    type = "image_url",
                                    image_url = ImageUrl("data:image/png;base64,$imageBase64")
                                )
                            },
                            text?.let { ContentPart.TextPart(type = "text", text = it) },
                        )
                    ),
                ),
                max_completion_tokens = maxCompletionTokens,
                reasoning_effort = "none",
                temperature = temperature,
                response_format = targetFormat
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

            if(usage.total_tokens >= maxTotalToken && chatList.isNotEmpty()) {
                chatList.removeAt(0)
            }

            return response.choices.firstOrNull()?.message?.content.toString()
        } catch (e: Exception) {
            FileLogger.e(TAG,"请求失败：${e.localizedMessage}")
        } finally {

        }
        return null
    }

    private var lastNekoReply: NekoReply? = null
    fun wantToTalk(): Boolean{
        if(lastNekoReply != null){
            return lastNekoReply!!.willingnessToChat > 70
        }
        return false
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
                    field = OpenAiSession(BotApp.getInstance().getChatUrl())
                }
                return field
            }
            private set
    }
}
