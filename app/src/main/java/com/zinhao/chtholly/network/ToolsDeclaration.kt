package com.zinhao.chtholly.network

import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.openai.FunctionDefinition
import com.zinhao.chtholly.network.openai.Tool as OpenAiTool
import com.zinhao.chtholly.network.openai.ToolParameters
import com.zinhao.chtholly.network.openai.ToolProperty
import com.zinhao.chtholly.network.tools.AppendTextTool
import com.zinhao.chtholly.network.tools.ClearContextTool
import com.zinhao.chtholly.network.tools.CreateReminder
import com.zinhao.chtholly.network.tools.DeleteFileTool
import com.zinhao.chtholly.network.tools.DoStepOnNode
import com.zinhao.chtholly.network.tools.FileReaderTool
import com.zinhao.chtholly.network.tools.FileWriterTool
import com.zinhao.chtholly.network.tools.FinishSetupTool
import com.zinhao.chtholly.network.tools.GetAppStatusTool
import com.zinhao.chtholly.network.tools.GetReminders
import com.zinhao.chtholly.network.tools.GetSystemTime
import com.zinhao.chtholly.network.tools.GetViewNode
import com.zinhao.chtholly.network.tools.ListFilesTool
import com.zinhao.chtholly.network.tools.MutedUserTool
import com.zinhao.chtholly.network.tools.SendFileTool
import com.zinhao.chtholly.network.tools.SetConfigTool
import com.zinhao.chtholly.network.tools.SetModelTool
import com.zinhao.chtholly.network.tools.SwitchCharacterTool
import com.zinhao.chtholly.network.tools.SwitchModeTool
import com.zinhao.chtholly.network.tools.ToggleRoleplayTool
import com.zinhao.chtholly.network.tools.ToggleSpeakerTool


val GEMINI_TOOLS = Tool(
    listOf(
        // 文件读写
        ListFilesTool, FileWriterTool, FileReaderTool,
        AppendTextTool, SendFileTool,DeleteFileTool,
        // 禁言
        MutedUserTool,
        // 提醒工具
        CreateReminder, GetReminders, GetSystemTime,
        // 运行时设置工具
        GetAppStatusTool, SwitchModeTool, SetModelTool,
        ToggleRoleplayTool, ToggleSpeakerTool,
        ClearContextTool, SwitchCharacterTool,
        // 首启对话式配置工具
        SetConfigTool, FinishSetupTool
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
    DeleteFileTool,
    // 禁言
    MutedUserTool,
    // 提醒工具
    CreateReminder,
    GetReminders,
    GetSystemTime,
    // 运行时设置工具
    GetAppStatusTool,
    SwitchModeTool,
    SetModelTool,
    ToggleRoleplayTool,
    ToggleSpeakerTool,
    ClearContextTool,
    SwitchCharacterTool,
    // 首启对话式配置工具
    SetConfigTool,
    FinishSetupTool
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
        DeleteFileTool.name -> DeleteFileTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        AppendTextTool.name -> AppendTextTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        SendFileTool.name -> SendFileTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        GetViewNode.name -> GetViewNode.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        DoStepOnNode.name -> DoStepOnNode.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        MutedUserTool.name -> MutedUserTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        CreateReminder.name -> CreateReminder.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        GetReminders.name -> GetReminders.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        GetSystemTime.name -> GetSystemTime.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        GetAppStatusTool.name -> GetAppStatusTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        SwitchModeTool.name -> SwitchModeTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        SetModelTool.name -> SetModelTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        ToggleRoleplayTool.name -> ToggleRoleplayTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        ToggleSpeakerTool.name -> ToggleSpeakerTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        ClearContextTool.name -> ClearContextTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        SwitchCharacterTool.name -> SwitchCharacterTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        SetConfigTool.name -> SetConfigTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
        FinishSetupTool.name -> FinishSetupTool.funImpl?.call(functionCall, callback, netAiAskAble, thoughtSignature)
    }
}