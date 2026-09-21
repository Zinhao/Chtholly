package com.zinhao.chtholly.network.gemini.tools

import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.session.RemoteChatApiSession
import org.json.JSONObject

val ClearContextTool = FunctionDeclaration(
    description = "Clear the current conversation context/history. This resets the AI's memory of previous messages in the current session.",
    name = "clear_context",
    Parameters(
        properties = emptyMap(),
        required = listOf(),
        type = "object"
    ),
    funImpl = object : FunImpl {
        override fun call(
            functionCall: FunctionCall,
            callback: ToolCallback,
            netAiAskAble: NetAiAskAble,
            thoughtSignature: String?
        ) {
            try {
                val session = BotApp.getInstance().apiSession
                if (session is RemoteChatApiSession) {
                    val clearedCount = session.clearContext()
                    val result = JSONObject().apply {
                        put("success", true)
                        put("cleared_messages", clearedCount)
                    }
                    callback.addToolResponse(
                        functionCall.name,
                        "clear_result",
                        result.toString(),
                        thoughtSignature
                    )
                } else {
                    callback.addToolResponse(
                        functionCall.name,
                        "clear_result",
                        JSONObject().apply {
                            put("success", false)
                            put("error", "Context clearing is not supported in the current NekoSession mode. Switch to OpenAI or Gemini mode first.")
                        }.toString(),
                        thoughtSignature
                    )
                }
            } catch (e: Exception) {
                callback.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)
