package com.zinhao.chtholly.network.gemini.tools

import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.session.GeminiSession
import com.zinhao.chtholly.utils.LocalFileCache
import org.json.JSONObject
import java.io.File

val AppendTextTool = FunctionDeclaration(
    description = "append text content to a specific file",
    name = "append_text",
    Parameters(
        properties = mapOf(
            Pair(
                "file_path", Properties(
                    description = "the path of the file to append to",
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
        required = listOf("file_path", "content"),
        type = "object"
    ),
    funImpl = object : FunImpl {
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {
            try {
                val filePath = functionCall.args["file_path"].toString()
                val content = functionCall.args["content"].toString()

                // 获取目标文件，基于工作空间目录
                val targetFile = File(LocalFileCache.getInstance().getWorkSpaceDir(), filePath)

                // 以追加模式(append = true)写入文件
                targetFile.appendText(content)

                val result = JSONObject().apply {
                    put("status", "success")
                    put("file_path", filePath)
                    put("bytes_appended", content.length)
                }

                GeminiSession.instance?.addToolResponse(
                    functionCall.name,
                    "append_text_result",
                    result.toString(),
                    thoughtSignature
                )
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)