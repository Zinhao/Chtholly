package com.zinhao.chtholly.network.tools

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
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.WorkspaceFileToolBase
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
                "path", Properties(
                    description = "the path of the directory wait to list. If need list root,stay empty",
                    type = "string", null
                )
            ),
        ),
        listOf("path",),
        "object"
    ),
    funImpl = object : FunImpl{
        override fun call(
            functionCall: FunctionCall,
            callback: ToolCallback,
            netAiAskAble: NetAiAskAble,
            thoughtSignature: String?
        ) {
            object : WorkspaceFileToolBase() {

                override fun onValidatedFile(
                    functionCall: FunctionCall,
                    file: File,
                    relativePath: String,
                    callback: ToolCallback,
                    thoughtSignature: String?
                ) {
                    if (!file.isDirectory) {
                        throw IllegalArgumentException("Path is not a directory")
                    }

                    val result = org.json.JSONArray()
                    file.listFiles()
                        ?.take(maxListFiles)
                        ?.forEach {
                            if (!java.nio.file.Files.isSymbolicLink(it.toPath())) {
                                result.put(
                                    JSONObject().apply {
                                        put("name", it.name)
                                        put("is_file", it.isFile)
                                        put("size", it.length())
                                        put("last_modified", it.lastModified())
                                    }
                                )
                            }
                        }

                    success(
                        callback,
                        functionCall,
                        "list_dir_result",
                        JSONObject().put("files", result),
                        thoughtSignature
                    )
                }
            }.execute(functionCall, callback, netAiAskAble, thoughtSignature)
        }
    }
)