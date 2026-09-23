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
import java.io.File

val FileReaderTool = FunctionDeclaration(
    description = "Read text from the file",
    name = "read_text",
    Parameters(
        properties = mapOf(
            Pair(
                "path", Properties(
                    description = "Relative path of the file to write to file",
                    type = "string", null
                )
            )
        ),
        listOf("path"),
        "object"
    ),
    funImpl = object : FunImpl{
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
                        val readFileResult = LocalFileCache.getInstance().readTextSync(file)
                        callback.addToolResponse(functionCall.name,"file_content",readFileResult,thoughtSignature)
                    } catch (e: Exception) {
                        callback.addToolErr(functionCall.name,e, thoughtSignature)
                    }
                }

            }.execute(functionCall, callback, netAiAskAble, thoughtSignature)
        }

    }
)