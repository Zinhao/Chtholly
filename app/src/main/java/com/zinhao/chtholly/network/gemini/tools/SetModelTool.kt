package com.zinhao.chtholly.network.gemini.tools

import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.session.RemoteChatApiSession
import org.json.JSONArray
import org.json.JSONObject

val SetModelTool = FunctionDeclaration(
    description = "Switch the AI model by index, or list all available models if no index is provided. Only works in OpenAI or Gemini mode.",
    name = "set_model",
    Parameters(
        properties = mapOf(
            "model_index" to Properties(
                description = "The index of the model to switch to. Omit this parameter to list all available models.",
                type = "integer", null
            )
        ),
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
                if (session !is RemoteChatApiSession) {
                    callback.addToolResponse(
                        functionCall.name,
                        "model_result",
                        JSONObject().apply {
                            put("success", false)
                            put("error", "Model switching is not supported in NekoSession mode. Switch to OpenAI or Gemini mode first.")
                        }.toString(),
                        thoughtSignature
                    )
                    return
                }

                val indexArg = functionCall.args["model_index"]
                if (indexArg == null) {
                    // List all available models
                    val models = session.modelList
                    val currentModel = session.currentModel
                    val modelsArray = JSONArray()
                    for (i in models.indices) {
                        modelsArray.put(JSONObject().apply {
                            put("index", i)
                            put("name", models[i].str)
                            put("is_current", models[i].str == currentModel.str)
                        })
                    }
                    val result = JSONObject().apply {
                        put("success", true)
                        put("models", modelsArray)
                        put("current_model", currentModel.str)
                    }
                    callback.addToolResponse(
                        functionCall.name,
                        "model_result",
                        result.toString(),
                        thoughtSignature
                    )
                } else {
                    // Switch to the specified model
                    val index = indexArg.toString().toInt()
                    val models = session.modelList
                    if (index < 0 || index >= models.size) {
                        callback.addToolResponse(
                            functionCall.name,
                            "model_result",
                            JSONObject().apply {
                                put("success", false)
                                put("error", "Model index $index is out of range. Available: 0-${models.size - 1}")
                            }.toString(),
                            thoughtSignature
                        )
                        return
                    }
                    session.setModelIndex(index)
                    val result = JSONObject().apply {
                        put("success", true)
                        put("model_index", index)
                        put("model_name", session.currentModel.str)
                    }
                    callback.addToolResponse(
                        functionCall.name,
                        "model_result",
                        result.toString(),
                        thoughtSignature
                    )
                }
            } catch (e: Exception) {
                callback.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)
