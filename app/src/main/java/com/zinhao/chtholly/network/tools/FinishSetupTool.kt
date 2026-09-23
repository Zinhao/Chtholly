package com.zinhao.chtholly.network.tools

import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.utils.AppConfigWriter
import org.json.JSONObject

val FinishSetupTool = FunctionDeclaration(
    description = "Finish the first-launch setup after all required config items are confirmed with the user. Clears the first-run flag and restores the character persona as system prompt.",
    name = "finish_setup",
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
                val err = AppConfigWriter.finishSetup()
                val result = JSONObject().apply {
                    put("success", err == null)
                    if (err != null) {
                        put("error", err)
                    }
                }
                callback.addToolResponse(
                    functionCall.name,
                    "setup_result",
                    result.toString(),
                    thoughtSignature
                )
            } catch (e: Exception) {
                callback.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)
