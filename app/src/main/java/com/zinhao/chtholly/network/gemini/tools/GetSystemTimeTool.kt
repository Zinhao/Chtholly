package com.zinhao.chtholly.network.gemini.tools

import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.session.GeminiSession
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val GetSystemTime = FunctionDeclaration(
    description = "Get the current system date and time. Use this to calculate relative dates like 'tomorrow' or 'next Friday'.",
    name = "get_system_time",
    Parameters(
        properties = emptyMap(), // 无需参数
        required = listOf(),
        type = "object"
    ),
    funImpl = object : FunImpl {
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {
            try {
                // 定义与 ReminderManager 一致的时间格式
                val sdf = SimpleDateFormat("yyyyMMdd'T'HHmm", Locale.getDefault())
                val currentTime = sdf.format(Date())

                // 获取星期几，方便 Agent 处理“周五”之类的请求
                val dayOfWeek = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())

                val result = JSONObject().apply {
                    put("current_time", currentTime)
                    put("day_of_week", dayOfWeek)
                    put("timezone", TimeZone.getDefault().id)
                }

                GeminiSession.instance?.addToolResponse(
                    functionCall.name,
                    "time_result",
                    result.toString(),
                    thoughtSignature
                )
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)