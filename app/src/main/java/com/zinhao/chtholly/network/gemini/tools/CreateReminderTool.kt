package com.zinhao.chtholly.network.gemini.tools
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.session.GeminiSession
import com.zinhao.chtholly.utils.ReminderManager

val CreateReminder = FunctionDeclaration(
    description = "Create a reminder or task with a specific time and title.",
    name = "create_reminder",
    Parameters(
        properties = mapOf(
            "title" to Properties(
                description = "The content of the reminder, e.g., 'Buy milk'.",
                type = "string", null
            ),
            "start_datetime" to Properties(
                description = "The scheduled time in yyyymmddTHHMM format, e.g., '20231027T1030'.",
                type = "string", null
            ),
            "is_all_day" to Properties(
                description = "Whether it's an all-day event.",
                type = "boolean", null
            )
        ),
        required = listOf("title", "start_datetime"),
        type = "object"
    ),
    funImpl = object : FunImpl {
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {
            try {
                val title = functionCall.args["title"].toString()
                val startTime = functionCall.args["start_datetime"].toString()
                val isAllDay = functionCall.args["is_all_day"]?.toString()?.toBoolean() ?: false

                // 这里调用你的提醒服务逻辑（例如写入系统日历或本地数据库）
                ReminderManager.create(title, startTime, isAllDay)

                val successMessage = "Successfully set reminder: '$title' at $startTime"

                GeminiSession.instance?.addToolResponse(
                    functionCall.name,
                    "create_result",
                    successMessage,
                    thoughtSignature
                )
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)

val GetReminders = FunctionDeclaration(
    description = "List existing reminders within a specific time range.",
    name = "get_reminders",
    Parameters(
        properties = mapOf(
            "from_datetime" to Properties(
                description = "Start range for searching reminders (yyyymmddTHHMM).",
                type = "string", null
            ),
            "to_datetime" to Properties(
                description = "End range for searching reminders (yyyymmddTHHMM).",
                type = "string", null
            )
        ),
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
                val from = functionCall.args["from_datetime"]?.toString()
                val to = functionCall.args["to_datetime"]?.toString()

                // 模拟从数据库获取提醒列表
                val reminders = ReminderManager.getReminders(from,to)

                GeminiSession.instance?.addToolResponse(
                    functionCall.name,
                    "reminders_list",
                    reminders,
                    thoughtSignature
                )
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)