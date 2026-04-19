package com.zinhao.chtholly.network.gemini.tools

import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.session.GeminiSession
import com.zinhao.chtholly.utils.LocalFileCache
import com.zinhao.chtholly.utils.QQChatHandler
import java.io.File

val FileWriterTool = FunctionDeclaration(
    description = "Write text to the file, Save text to the file",
    name = "write_text_to_file",
    Parameters(
        properties = mapOf(
            Pair(
                "text_content", Properties(
                    description = "the text wait to write to file",
                    type = "string", null
                )
            ),
            Pair(
                "file_name", Properties(
                    description = "the file name, like \"main.java, app.dart\". No abs path! ",
                    type = "string", null
                )
            )
        ),
        listOf("text_content", "file_name"),
        "object"
    ),
    funImpl = object : FunImpl{
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {
            try {
                val fileName = functionCall.args["file_name"].toString()
                val textContent = functionCall.args["text_content"].toString()
                val file = File(LocalFileCache.getInstance().getWorkSpaceDir(), fileName)
                LocalFileCache.getInstance().writeTextSync(file, textContent)
                if(netAiAskAble.packageName == QQChatHandler.PACKAGE_NAME){
                    netAiAskAble.initShareStepTo(
                        NekoChatService.getInstance().qqChatHandler.chatTitle,
                        NekoChatService.FUNC_SHARE_FILE,
                        file.path)
                }
                GeminiSession.instance?.addToolResponse(functionCall.name,"write_result", true, thoughtSignature)
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name,e, thoughtSignature)
            }
        }

    }
)