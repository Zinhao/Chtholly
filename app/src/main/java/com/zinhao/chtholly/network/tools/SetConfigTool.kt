package com.zinhao.chtholly.network.tools

import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.utils.AppConfigWriter
import org.json.JSONObject

val SetConfigTool = FunctionDeclaration(
    description = "Set one app config item, such as admin name, bot name, soul/persona, TTS URL, Feishu credentials, server URL or API key. Call get_app_status first to check what is already configured.",
    name = "set_config",
    Parameters(
        properties = mapOf(
            "key" to Properties(
                description = "The config key to set.",
                type = "string",
                enum = AppConfigWriter.VALID_KEYS
            ),
            "value" to Properties(
                description = "The new value of the config key.",
                type = "string",
                null
            )
        ),
        required = listOf("key", "value"),
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
                val key = functionCall.args["key"]?.toString() ?: ""
                val value = functionCall.args["value"]?.toString() ?: ""
                val err = AppConfigWriter.set(key, value)
                val result = JSONObject().apply {
                    put("success", err == null)
                    if (err == null) {
                        put("key", key)
                    } else {
                        put("error", err)
                    }
                }
                callback.addToolResponse(
                    functionCall.name,
                    "config_result",
                    result.toString(),
                    thoughtSignature
                )
            } catch (e: Exception) {
                callback.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)
