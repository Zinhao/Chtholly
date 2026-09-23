package com.zinhao.chtholly.network.tools

import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.utils.LocalFileCache
import com.zinhao.chtholly.handlerImpl.QQChatHandler
import com.zinhao.chtholly.network.WorkspaceFileToolBase
import java.io.File

val SendFileTool = FunctionDeclaration(
    description = "Send or share a specific file to the current chat or user",
    name = "send_file",
    Parameters(
        properties = mapOf(
            Pair(
                "path", Properties(
                    description = "Relative path of the file to sent, e.g., \"report.pdf\".",
                    type = "string", null
                )
            )
        ),
        listOf("path"),
        "object"
    ),
    funImpl = object : FunImpl {
        override fun call(
            functionCall: FunctionCall,
            callback: ToolCallback,
            netAiAskAble: NetAiAskAble,
            thoughtSignature: String?
        ) {
            object : WorkspaceFileToolBase(){
                override fun onValidatedFile(
                    functionCall: FunctionCall,
                    file: File,
                    relativePath: String,
                    callback: ToolCallback,
                    thoughtSignature: String?
                ) {
                    try {
                        if (!file.exists()) {
                            throw Exception("File not found: ${file.absolutePath}")
                        }
                        if (netAiAskAble.packageName == QQChatHandler.PACKAGE_NAME) {
                            netAiAskAble.initShareStepTo(
                                NekoChatService.getInstance().qqChatHandler.chatTitle,
                                NekoChatService.FUNC_SHARE_FILE,
                                file.path
                            )
                        }

                        // 反馈给 Gemini 发送指令已执行
                        callback.addToolResponse(
                            functionCall.name,
                            "send_result",
                            "File ${file.name} has been sent successfully.",
                            thoughtSignature
                        )
                    } catch (e: Exception) {
                        callback.addToolErr(functionCall.name, e, thoughtSignature)
                    }
                }
            }.execute(functionCall, callback, netAiAskAble, thoughtSignature)

        }
    }
)