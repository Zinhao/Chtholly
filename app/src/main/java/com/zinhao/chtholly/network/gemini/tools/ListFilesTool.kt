package com.zinhao.chtholly.network.gemini.tools

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FileInfo
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.session.GeminiSession
import com.zinhao.chtholly.utils.LocalFileCache
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
@OptIn(ExperimentalStdlibApi::class)
private val fileInfoAdapter: JsonAdapter<FileInfo> = moshi.adapter<FileInfo>()
val ListFilesTool = FunctionDeclaration(
    description = "list all file in the directory",
    name = "list_directory",
    Parameters(
        properties = mapOf(
            Pair(
                "dir_path", Properties(
                    description = "the path of the directory wait to list. If need list root,stay empty",
                    type = "string", null
                )
            ),
        ),
        listOf("dir_path",),
        "object"
    ),
    funImpl = object : FunImpl{
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {
            try {
                val dirPath = functionCall.args["dir_path"].toString()
                val targetDir: File
                if (functionCall.args.isEmpty() || dirPath.isEmpty() || dirPath == "." || dirPath == "\\") {
                    targetDir = LocalFileCache.getInstance().getWorkSpaceDir()
                } else {
                    targetDir =
                        File(LocalFileCache.getInstance().getWorkSpaceDir(), dirPath)
                }
                val fileArray = JSONArray()
                targetDir.listFiles()?.forEach { file ->
                    val fileInfo =
                        FileInfo(file.name, file.isFile, file.length(), file.lastModified())
                    val strFileInfo = fileInfoAdapter.toJson(fileInfo)
                    val jsonObject = JSONObject(strFileInfo)
                    fileArray.put(jsonObject)
                }
                GeminiSession.instance?.addToolResponse(functionCall.name,"list_dir_result",fileArray.toString(),thoughtSignature)
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name,e, thoughtSignature)
            }
        }
    }
)