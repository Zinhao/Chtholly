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

val ToggleRoleplayTool = FunctionDeclaration(
    description = "Toggle roleplay mode for the current AI character. Each character has its own independent roleplay setting stored in the database.",
    name = "toggle_roleplay",
    Parameters(
        properties = mapOf(
            "enabled" to Properties(
                description = "Set to true to enable roleplay mode, false to disable it.",
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
                BotApp.getInstance().setRoleplay(enabled)

                val character = BotApp.getInstance().currentCharacter
                val result = JSONObject().apply {
                    put("success", true)
                    put("roleplay_enabled", enabled)
                    put("character_name", character?.name ?: "unknown")
                    put("character_id", character?.id ?: -1)
                }
                callback.addToolResponse(
                    functionCall.name,
                    "roleplay_result",
                    result.toString(),
                    thoughtSignature
                )
            } catch (e: Exception) {
                callback.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)
