package com.zinhao.chtholly.network.gemini.tools

import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.session.GeminiSession
import com.zinhao.chtholly.session.NekoSession
import com.zinhao.chtholly.session.OpenAiSession
import org.json.JSONObject

val SwitchModeTool = FunctionDeclaration(
    description = "Switch the AI backend mode. 'openai' uses OpenAI-compatible API, 'gemini' uses Google Gemini API, 'neko' uses the built-in Neko session.",
    name = "switch_ai_mode",
    Parameters(
        properties = mapOf(
            "mode" to Properties(
                description = "The AI mode to switch to.",
                type = "string",
                enum = listOf("openai", "gemini", "neko")
            )
        ),
        required = listOf("mode"),
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
                val mode = functionCall.args["mode"].toString().lowercase()
                when (mode) {
                    "openai" -> BotApp.getInstance().mode = OpenAiSession::class.java
                    "gemini" -> BotApp.getInstance().mode = GeminiSession::class.java
                    "neko" -> BotApp.getInstance().mode = NekoSession::class.java
                    else -> throw IllegalArgumentException("Invalid mode: $mode. Must be 'openai', 'gemini', or 'neko'.")
                }

                val result = JSONObject().apply {
                    put("success", true)
                    put("mode", mode)
                }
                callback.addToolResponse(
                    functionCall.name,
                    "mode_result",
                    result.toString(),
                    thoughtSignature
                )
            } catch (e: Exception) {
                callback.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)
