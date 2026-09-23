package com.zinhao.chtholly.network.tools

import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.WorkspaceFileToolBase
import org.json.JSONObject
import java.io.File

val DeleteFileTool = FunctionDeclaration(
    description = "Delete a file or an empty directory in the workspace",
    name = "delete_file",
    parameters = Parameters(
        properties = mapOf(
            "path" to Properties(
                description = "Relative path of the file or directory to delete. Cannot be empty or root.",
                type = "string",
                enum = null
            )
        ),
        required = listOf("path"),
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
                    val deleted = if (file.isDirectory) {
                        if (file.listFiles()?.isEmpty() == true) {
                            file.delete()
                        } else {
                            return callback.addToolResponse(
                                functionCall.name,
                                "delete_result",
                                JSONObject().apply {
                                    put("deleted", false)
                                    put("path", relativePath)
                                    put("reason", "Directory is not empty")
                                }.toString(),
                                thoughtSignature
                            )
                        }
                    } else {
                        file.delete()
                    }

                    val result = JSONObject().apply {
                        put("deleted", deleted)
                        put("path", relativePath)
                        put("is_file", file.isFile)
                        if (!deleted) {
                            put("reason", "Delete failed (unknown reason)")
                        }
                    }

                    success(
                        callback,
                        functionCall,
                        "delete_result",
                        result,
                        thoughtSignature
                    )
                }

            }.execute(functionCall, callback, netAiAskAble, thoughtSignature)
        }
    }
)