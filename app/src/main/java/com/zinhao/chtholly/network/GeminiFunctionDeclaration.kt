package com.zinhao.chtholly.network

import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.network.gemini.tools.*


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
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
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

interface FunImpl{
    fun call(functionCall: FunctionCall,thoughtSignature: String?,netAiAskAble: NetAiAskAble)
}