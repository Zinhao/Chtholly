package com.zinhao.chtholly.network.tools

import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import org.json.JSONObject

val ToggleSpeakerTool = FunctionDeclaration(
    description = "Toggle speaker name prefix in group chat messages. When enabled, the bot includes the speaker's name before their message in group chats.",
    name = "toggle_speaker",
    Parameters(
        properties = mapOf(
            "enabled" to Properties(
                description = "Set to true to enable speaker prefix, false to disable it.",
                type = "boolean", null
            )
        ),
        required = listOf("enabled"),
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
                val enabled = functionCall.args["enabled"].toString().toBoolean()
                BotApp.getInstance().isWithSpeaker = enabled

                val result = JSONObject().apply {
                    put("success", true)
                    put("speaker_enabled", enabled)
                }
                callback.addToolResponse(
                    functionCall.name,
                    "speaker_result",
                    result.toString(),
                    thoughtSignature
                )
            } catch (e: Exception) {
                callback.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)
