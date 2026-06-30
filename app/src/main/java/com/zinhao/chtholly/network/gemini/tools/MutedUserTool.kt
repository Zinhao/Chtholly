package com.zinhao.chtholly.network.gemini.tools

import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.session.GeminiSession
import com.zinhao.chtholly.handlerImpl.QQChatHandler

val MutedUserTool = FunctionDeclaration(
    description = "User banned from speaking",
    name = "muted",
    Parameters(
        properties = mapOf(
            Pair(
                "muted_user_name", Properties(
                    description = "The name of the person who needs to be muted,does not include identification",
                    type = "string", null
                )
            ),
        ),
        listOf("muted_user_name",),
        "object"
    ),
    funImpl = object : FunImpl {
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {
            try {
                val blockName = functionCall.args["muted_user_name"].toString()
                if (netAiAskAble.packageName == QQChatHandler.PACKAGE_NAME) {
                    netAiAskAble.blockUserSpeak(blockName)
                }
                // 反馈给 Gemini 发送指令已执行
                GeminiSession.instance?.addToolResponse(
                    functionCall.name,
                    "muted_result",
                    "${blockName} have been muted for 10 minutes.",
                    thoughtSignature
                )
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)