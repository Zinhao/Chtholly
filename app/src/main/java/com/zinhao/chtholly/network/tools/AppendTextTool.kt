package com.zinhao.chtholly.network.tools

import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.WorkspaceFileToolBase
import com.zinhao.chtholly.utils.LocalFileCache
import org.json.JSONObject
import java.io.File

val AppendTextTool = FunctionDeclaration(
    description = "append text content to a specific file",
    name = "append_text",
    Parameters(
        properties = mapOf(
            Pair(
                "path", Properties(
                    description = "Relative path of the file to append to file",
                    type = "string", null
                )
            ),
            Pair(
                "content", Properties(
                    description = "the text content to be appended",
                    type = "string", null
                )
            )
        ),
        required = listOf("path", "content"),
        type = "object"
    ),
    funImpl = object : FunImpl {
        override fun call(
            functionCall: FunctionCall,
            callback: ToolCallback,
            netAiAskAble: NetAiAskAble,
            thoughtSignature: String?
        ) {
            object : WorkspaceFileToolBase(){
                override fun onValidatedFile(
                    functionCall: FunctionCall,
                    file: File,
                    relativePath: String,
                    callback: ToolCallback,
                    thoughtSignature: String?
                ) {
                    try {

                        val content = functionCall.args["content"].toString()
                        file.appendText(content)

                        val result = JSONObject().apply {
                            put("status", "success")
                            put("path", file)
                            put("bytes_appended", content.length)
                        }

                        callback.addToolResponse(
                            functionCall.name,
                            "append_text_result",
                            result.toString(),
                            thoughtSignature
                        )
                    } catch (e: Exception) {
                        callback.addToolErr(functionCall.name, e, thoughtSignature)
                    }
                }

            }.execute(functionCall, callback, netAiAskAble, thoughtSignature)
        }
    }
)