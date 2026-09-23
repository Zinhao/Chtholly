package com.zinhao.chtholly.network.tools

import android.content.Context
import android.os.BatteryManager
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.BuildConfig
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.session.GeminiSession
import com.zinhao.chtholly.session.NekoSession
import com.zinhao.chtholly.session.OpenAiSession
import com.zinhao.chtholly.session.RemoteChatApiSession
import org.json.JSONObject

val GetAppStatusTool = FunctionDeclaration(
    description = "Get the current app status including version, AI mode, model, bot name, admin name, current character info, roleplay mode, speaker mode, and battery level.",
    name = "get_app_status",
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
                val app = BotApp.getInstance()
                val modeClass = app.mode
                val modeName = when (modeClass) {
                    OpenAiSession::class.java -> "openai"
                    GeminiSession::class.java -> "gemini"
                    NekoSession::class.java -> "neko"
                    else -> "unknown"
                }

                val currentModel = app.apiSession.let { session ->
                    if (session is RemoteChatApiSession) session.currentModel.str else "N/A"
                }

                val character = app.currentCharacter

                // Battery level
                val bm = app.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

                // Masked API key
                val apiKey = app.apiKey ?: ""
                val maskedKey = if (apiKey.length > 5) {
                    "sk-***********${apiKey.substring(apiKey.length - 5)}"
                } else if (apiKey.isNotEmpty()) {
                    "***"
                } else {
                    "(not set)"
                }

                val result = JSONObject().apply {
                    put("version", BuildConfig.VERSION_NAME)
                    put("mode", modeName)
                    put("current_model", currentModel)
                    put("bot_name", app.atBotName ?: "")
                    put("admin_name", app.adminName ?: "")
                    put("current_character_id", character?.id ?: -1)
                    put("current_character_name", character?.name ?: "none")
                    put("roleplay_enabled", character?.isRoleplay ?: true)
                    put("speaker_enabled", app.isWithSpeaker)
                    put("battery_level", battery)
                    put("api_key_masked", maskedKey)
                }

                callback.addToolResponse(
                    functionCall.name,
                    "app_status",
                    result.toString(),
                    thoughtSignature
                )
            } catch (e: Exception) {
                callback.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)
