package com.zinhao.chtholly.utils

import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.entity.AICharacter
import com.zinhao.chtholly.session.RemoteChatApiSession

/**
 * 配置写入的统一入口：/setConfig、/finishSetup 斜杠命令与
 * set_config、finish_setup 工具共用，写入的 SharedPreferences key
 * 与 [SetupViewModel.finishSetup] 保持一致。
 */
object AppConfigWriter {

    /** 可写配置键 */
    val VALID_KEYS = listOf(
        "adminName", "botName",
        "ttsUrl",
        "feishuAppId",
        "feishuAppSecret",
        "chatUrl",
        "apiKey"
    )

    /**
     * 写入单项配置（运行时字段 + SharedPreferences）。
     * @return null 表示成功，否则为错误信息
     */
    fun set(key: String, value: String): String? {
        val app = BotApp.getInstance()
        val prefs = app.sharedPreferences
        when (key) {
            "adminName" -> {
                app.adminName = value
                prefs.edit().putString(BotApp.CONFIG_ADMIN_NAME, value).apply()
            }
            "botName" -> {
                app.atBotName = value
                prefs.edit().putString(BotApp.CONFIG_BOT_NAME, value).apply()
            }
            "ttsUrl" -> {
                if (value.isNotEmpty() && !isHttpUrl(value)) {
                    return "ttsUrl 必须以 http:// 或 https:// 开头"
                }
                app.ttsUrl = value
                prefs.edit().putString(BotApp.CONFIG_TTS_URL, value).apply()
            }
            "feishuAppId" -> {
                app.feishuAppId = value
                prefs.edit().putString(BotApp.CONFIG_FEISHU_APP_ID, value).apply()
            }
            "feishuAppSecret" -> {
                app.feishuAppSecret = value
                prefs.edit().putString(BotApp.CONFIG_FEISHU_APP_SECRET, value).apply()
            }
            "chatUrl" -> {
                if (!isHttpUrl(value)) {
                    return "chatUrl 必须以 http:// 或 https:// 开头"
                }
                app.chatUrl = value
                prefs.edit().putString(BotApp.CONFIG_CHAT_URL, value).apply()
            }
            "apiKey" -> {
                // setApiKey 会同步切换会话模式（空 -> Neko，非空 -> OpenAi）
                app.apiKey = value
                prefs.edit().putString(BotApp.CONFIG_API_KEY, value).apply()
            }
            else -> return "未知配置键: $key，可用键: ${VALID_KEYS.joinToString()}"
        }
        return null
    }

    /**
     * 结束首启配置：清除 isFirstRun 标志，并把 system prompt 从
     * 首启引导恢复为当前角色人设。
     * @return null 表示成功，否则为错误信息
     */
    fun finishSetup(): String? {
        val app = BotApp.getInstance()
        if (!isHttpUrl(app.chatUrl)) {
            return "Base URL 尚未配置，请先用 setConfig 设置 chatUrl"
        }
        app.isFirstRun = false
        app.sharedPreferences.edit().putBoolean(BotApp.CONFIG_IS_FIRST_RUN, false).apply()
        val session = app.apiSession
        if (session is RemoteChatApiSession) {
            app.currentCharacter?.let { session.agentPrompt = it.desc }
        }
        return null
    }

    private fun isHttpUrl(url: String): Boolean {
        return (url.startsWith("http://") || url.startsWith("https://")) && url.length > 10
    }
}
