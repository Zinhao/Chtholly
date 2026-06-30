package com.zinhao.chtholly.network.gemini.tools

import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.session.GeminiSession
import com.zinhao.chtholly.utils.LocalFileCache
import com.zinhao.chtholly.handlerImpl.QQChatHandler
import java.io.File

val SendFileTool = FunctionDeclaration(
    description = "Send or share a specific file to the current chat or user",
    name = "send_file",
    Parameters(
        properties = mapOf(
            Pair(
                "file_name", Properties(
                    description = "the name of the file to be sent, e.g., \"report.pdf\". No absolute path!",
                    type = "string", null
                )
            )
        ),
        listOf("file_name"),
        "object"
    ),
    funImpl = object : FunImpl {
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {
            try {
                val fileName = functionCall.args["file_name"].toString()
                // 从工作空间获取文件实例
                val file = File(LocalFileCache.getInstance().getWorkSpaceDir(), fileName)

                if (!file.exists()) {
                    throw Exception("File not found: $fileName")
                }
                if (netAiAskAble.packageName == QQChatHandler.PACKAGE_NAME) {
                    netAiAskAble.initShareStepTo(
                        NekoChatService.getInstance().qqChatHandler.chatTitle,
                        NekoChatService.FUNC_SHARE_FILE,
                        file.path
                    )
                }

                // 反馈给 Gemini 发送指令已执行
                GeminiSession.instance?.addToolResponse(
                    functionCall.name,
                    "send_result",
                    "File ${file.name} has been sent successfully.",
                    thoughtSignature
                )
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)