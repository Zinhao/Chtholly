package com.zinhao.chtholly.network.gemini.tools
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.utils.LocalFileCache
import java.io.File

val FileReaderTool = FunctionDeclaration(
    description = "Read text from the file",
    name = "read_text",
    Parameters(
        properties = mapOf(
            Pair(
                "file_path", Properties(
                    description = "the file path",
                    type = "string", null
                )
            )
        ),
        listOf("file_path"),
        "object"
    ),
    funImpl = object : FunImpl{
        override fun call(
            functionCall: FunctionCall,
            callback: ToolCallback,
            netAiAskAble: NetAiAskAble,
            thoughtSignature: String?
        ) {
            try {
                if (!functionCall.args.contains("file_path")) {
                    throw Exception("file path err: need arg:file_path!")
                }
                val filePath = functionCall.args["file_path"].toString()
                val targetFile =
                    File(LocalFileCache.getInstance().getWorkSpaceDir(), filePath)
                val readFileResult = LocalFileCache.getInstance().readTextSync(targetFile)
                callback.addToolResponse(functionCall.name,"file_content",readFileResult,thoughtSignature)
            } catch (e: Exception) {
                callback.addToolErr(functionCall.name,e, thoughtSignature)
            }
        }

    }
)