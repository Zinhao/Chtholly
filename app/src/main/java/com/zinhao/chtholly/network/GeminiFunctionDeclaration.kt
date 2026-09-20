package com.zinhao.chtholly.network

import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.network.gemini.tools.*
import com.zinhao.chtholly.network.openai.FunctionDefinition
import com.zinhao.chtholly.network.openai.Tool as OpenAiTool
import com.zinhao.chtholly.network.openai.ToolParameters
import com.zinhao.chtholly.network.openai.ToolProperty


val PrintInfo = FunctionDeclaration(
    description = "call function when you need",
    name = "call_function",
    Parameters(
        properties = mapOf(
            Pair(
                "name", Properties(
                    description = "the function name",
                    enum = listOf(
                        "runInfo",
                        "help",
                        "printContext",
                        "closeAutoAction",
                        "openAutoAction",
                        "printSoul",
                        "everyDayCheck",
                        "screenShot",
                        "sendNewestPic",
                        "summarize",
                        "screenShare",
                        "videoCall",
                        "sendGallery",
                        "battery",
                        "takePhoto",
                        "recordVideo",
                    ),
                    type = "string"
                )
            )
        ),
        listOf("name"),
        "object"
    ),
    funImpl = object : FunImpl{
        override fun call(
            functionCall: FunctionCall,
            callback: ToolCallback,
            netAiAskAble: NetAiAskAble,
            thoughtSignature: String?
        ) {

        }

    }
)

val GEMINI_TOOLS = Tool(
    listOf(
        // 文件读写
        ListFilesTool,FileWriterTool,FileReaderTool,
        AppendTextTool,SendFileTool,
        // 禁言
        MutedUserTool,
        // 提醒工具
        CreateReminder,GetReminders,GetSystemTime
    )
)

/**
 * 将 Gemini 的 [FunctionDeclaration] 转换为 OpenAI 兼容的 [OpenAiTool].
 */
fun FunctionDeclaration.toOpenAiTool(): OpenAiTool {
    val toolProperties = parameters.properties.mapValues { (_, prop) ->
        ToolProperty(
            type = prop.type,
            description = prop.description,
            enum = prop.enum
        )
    }
    return OpenAiTool(
        function = FunctionDefinition(
            name = name,
            description = description,
            parameters = ToolParameters(
                type = parameters.type,
                properties = toolProperties,
                required = parameters.required.ifEmpty { null }
            )
        )
    )
}

/**
 * OpenAI 格式的工具列表，与 [GEMINI_TOOLS] 包含相同的工具定义.
 */
val OPENAI_TOOLS: List<OpenAiTool> = listOf(
    // 文件读写
    ListFilesTool,
    FileWriterTool,
    FileReaderTool,
    AppendTextTool,
    SendFileTool,
    // 禁言
    MutedUserTool,
    // 提醒工具
    CreateReminder,
    GetReminders,
    GetSystemTime
).map { it.toOpenAiTool() }

data class FileInfo(val name: String, val isFile: Boolean,val size: Long,val time: Long)

data class FunctionDeclaration(
    val description: String,
    val name: String,
    val parameters: Parameters,
    @Transient
    val funImpl: FunImpl? = null
)

data class Tool(
    val functionDeclarations: List<FunctionDeclaration>
)

interface ToolCallback {
    fun addToolResponse(name: String, key: String, result: Any, thoughtSignature: String? = null)
    fun addToolErr(name: String, e: Exception, thoughtSignature: String? = null)
}

interface FunImpl{
    fun call(functionCall: FunctionCall, callback: ToolCallback, netAiAskAble: NetAiAskAble, thoughtSignature: String? = null)
}

fun dispatchToolCall(
    name: String,
    functionCall: FunctionCall,
    callback: ToolCallback,
    netAiAskAble: NetAiAskAble,
    thoughtSignature: String? = null
) {
    when (name) {
        FileWriterTool.name -> FileWriterTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        ListFilesTool.name -> ListFilesTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        FileReaderTool.name -> FileReaderTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        AppendTextTool.name -> AppendTextTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        SendFileTool.name -> SendFileTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        GetViewNode.name -> GetViewNode.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        DoStepOnNode.name -> DoStepOnNode.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        MutedUserTool.name -> MutedUserTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        CreateReminder.name -> CreateReminder.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        GetReminders.name -> GetReminders.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        GetSystemTime.name -> GetSystemTime.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
    }
}