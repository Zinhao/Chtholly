package com.zinhao.chtholly.network

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.session.GeminiSession
import com.zinhao.chtholly.utils.LocalFileCache
import com.zinhao.chtholly.utils.QQChatHandler
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.collections.contains

val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
@OptIn(ExperimentalStdlibApi::class)
private val fileInfoAdapter: JsonAdapter<FileInfo> = moshi.adapter<FileInfo>()
val PrintInfo = FunctionDeclaration(
    description = "call function when you need",
    name = "call_function",
    Parameters(
        properties = mapOf(
            Pair(
                "name", Properties(
                    description = "the function name",
                    enum = listOf(
                        "runInfo",
                        "help",
                        "printContext",
                        "closeAutoAction",
                        "openAutoAction",
                        "printSoul",
                        "everyDayCheck",
                        "screenShot",
                        "sendNewestPic",
                        "summarize",
                        "screenShare",
                        "videoCall",
                        "sendGallery",
                        "battery",
                        "takePhoto",
                        "recordVideo",
                    ),
                    type = "string"
                )
            )
        ),
        listOf("name"),
        "object"
    ),
    geminiCallFun = object : GeminiCallFun{
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {

        }

    }
)

val FileWriter = FunctionDeclaration(
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
    geminiCallFun = object : GeminiCallFun{
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

val FileReader = FunctionDeclaration(
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
    geminiCallFun = object : GeminiCallFun{
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {
            try {
                if (!functionCall.args.contains("file_path")) {
                    throw Exception("file path err: need arg:file_path!")
                }
                val filePath = functionCall.args["file_path"].toString()
                val targetFile =
                    File(LocalFileCache.getInstance().getWorkSpaceDir(), filePath)
                val readFileResult = LocalFileCache.getInstance().readTextSync(targetFile)
                GeminiSession.instance?.addToolResponse(functionCall.name,"file_content",readFileResult,thoughtSignature)
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name,e, thoughtSignature)
            }
        }

    }
)

val ListFiles = FunctionDeclaration(
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
    geminiCallFun = object : GeminiCallFun{
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

val AppendText = FunctionDeclaration(
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
    geminiCallFun = object : GeminiCallFun {
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

val SendFile = FunctionDeclaration(
    description = "Send or share a specific file to the current chat or user",
    name = "send_file",
    Parameters(
        properties = mapOf(
            Pair(
                "file_name", Properties(
                    description = "the name of the file to be sent, e.g., \"report.pdf\". No absolute path!",
                    type = "string", null
                )
            )
        ),
        listOf("file_name"),
        "object"
    ),
    geminiCallFun = object : GeminiCallFun {
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {
            try {
                val fileName = functionCall.args["file_name"].toString()
                // 从工作空间获取文件实例
                val file = File(LocalFileCache.getInstance().getWorkSpaceDir(), fileName)

                if (!file.exists()) {
                    throw Exception("File not found: $fileName")
                }
                if (netAiAskAble.packageName == QQChatHandler.PACKAGE_NAME) {
                    netAiAskAble.initShareStepTo(
                        NekoChatService.getInstance().qqChatHandler.chatTitle,
                        NekoChatService.FUNC_SHARE_FILE,
                        file.path
                    )
                }

                // 反馈给 Gemini 发送指令已执行
                GeminiSession.instance?.addToolResponse(
                    functionCall.name,
                    "send_result",
                    "File ${file.name} has been sent successfully.",
                    thoughtSignature
                )
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)

// 获取AccessibilityNode,遍历节点 转换为json，供Agent阅读
val GetViewNode = FunctionDeclaration(
    description = "Get the current screen layout nodes to understand what's on the screen.",
    name = "get_view_nodes",
    Parameters(
        properties = emptyMap(), // 不需要参数，直接获取当前屏幕
        required = listOf(),
        type = "object"
    ),
    geminiCallFun = object : GeminiCallFun {
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {
            try {
                val rootNode = NekoChatService.getInstance().rootInActiveWindow
                val nodeList = JSONArray()

                // 递归遍历并扁平化节点树，只保留有意义的节点
                flattenNodes(rootNode, nodeList, "")

                GeminiSession.instance?.addToolResponse(
                    functionCall.name,
                    "view_nodes_result",
                    nodeList.toString(),
                    thoughtSignature
                )
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }

        private fun flattenNodes(node: AccessibilityNodeInfo?, list: JSONArray, path: String) {
            if (node == null) return

            // 过滤掉不可见或无意义的容器节点，减少 Token 浪费
            val hasContent = !node.text.isNullOrEmpty() || !node.contentDescription.isNullOrEmpty() || node.isClickable

            if (hasContent) {
                val json = JSONObject().apply {
                    put("node_index", path) // 路径作为唯一标识
                    put("text", node.text?.toString() ?: "")
                    put("desc", node.contentDescription?.toString() ?: "")
                    put("id", node.viewIdResourceName ?: "")
                    put("class", node.className.split(".").last())
                    put("clickable", node.isClickable)
                }
                list.put(json)
            }

            for (i in 0 until node.childCount) {
                flattenNodes(node.getChild(i), list, if (path.isEmpty()) "$i" else "$path-$i")
            }
        }
    }
)

val DoStepOnNode = FunctionDeclaration(
    description = "Perform actions on the screen, including clicking nodes, typing text, going back, or swiping.",
    name = "do_screen_action",
    Parameters(
        properties = mapOf(
            "action" to Properties(
                description = "The action to perform: 'CLICK', 'SET_TEXT', 'BACK', 'SWIPE_UP', 'SWIPE_DOWN'.",
                type = "string", null
            ),
            "node_index" to Properties(
                description = "The 'node_index' from get_view_nodes. Required for CLICK and SET_TEXT.",
                type = "string", null
            ),
            "text_content" to Properties(
                description = "The text to type. Required for SET_TEXT.",
                type = "string", null
            )
        ),
        required = listOf("action"),
        type = "object"
    ),
    geminiCallFun = object : GeminiCallFun {
        override fun call(
            functionCall: FunctionCall,
            thoughtSignature: String?,
            netAiAskAble: NetAiAskAble
        ) {
            try {
                val action = functionCall.args["action"].toString().uppercase()
                val service = NekoChatService.getInstance()
                var success = false

                when (action) {
                    "BACK" -> {
                        // 执行全局返回键
                        success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    }

                    "SWIPE_UP", "SWIPE_DOWN" -> {
                        // 执行滑动逻辑
                        success = performSwipe(service, action == "SWIPE_UP")
                    }

                    "CLICK", "SET_TEXT" -> {
                        val nodeIndex = functionCall.args["node_index"]?.toString()
                            ?: throw Exception("node_index is required for $action")
                        val rootNode = service.rootInActiveWindow ?: throw Exception("Root node is null")
                        val targetNode = findNodeByPath(rootNode, nodeIndex) ?: throw Exception("Node not found")

                        if (action == "CLICK") {
                            // 尝试点击节点，如果节点不可点则尝试点击其父类
                            success = performClickRecursive(targetNode)
                        } else {
                            val text = functionCall.args["text_content"]?.toString() ?: ""
                            val bundle = Bundle().apply {
                                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                            }
                            success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                        }
                    }
                }

                GeminiSession.instance?.addToolResponse(
                    functionCall.name,
                    "action_result",
                    "Action $action executed: $success",
                    thoughtSignature
                )
            } catch (e: Exception) {
                GeminiSession.instance?.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }

        // 递归查找可点击的父节点（增强点击成功率）
        private fun performClickRecursive(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return performClickRecursive(node.parent)
        }

        // 模拟滑动操作
        private fun performSwipe(service: AccessibilityService, isUp: Boolean): Boolean {
            val displayMetrics = service.resources.displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            val path = Path()
            if (isUp) {
                // 向上滑动：从屏幕中下部移动到中上部
                path.moveTo(width / 2f, height * 0.8f)
                path.lineTo(width / 2f, height * 0.2f)
            } else {
                // 向下滑动
                path.moveTo(width / 2f, height * 0.2f)
                path.lineTo(width / 2f, height * 0.8f)
            }

            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 100, 500))
            return service.dispatchGesture(gestureBuilder.build(), null, null)
        }

        // 辅助方法：根据路径查找节点
        private fun findNodeByPath(root: AccessibilityNodeInfo, path: String): AccessibilityNodeInfo? {
            var currentNode: AccessibilityNodeInfo? = root
            path.split("-").filter { it.isNotEmpty() }.map { it.toInt() }.forEach { idx ->
                if (currentNode == null || idx >= currentNode.childCount) return null
                currentNode = currentNode.getChild(idx)
            }
            return currentNode
        }
    }
)


/**
 ## 全局返回
 new Step(null,null,AccessibilityService.GLOBAL_ACTION_BACK,Step.ActionType.global)
 ##根据id点击节点
  public Step(String packageName, String viewId, int actionId, ActionType actionType, long daley)
 new Step(PACKAGE_NAME, ":id/bbt", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,1500)

 ## 根据位置点击节点
 public Step(String packageName, String viewId, int actionId, ActionType actionType, long daley, boolean findChildByPosition, int[] findPosition)
 new Step(QQChatHandler.PACKAGE_NAME,":id/recent_chat_list", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,1500,true,new int[]{1});

 ##根据文本点击节点
 public Step(String packageName, String viewId, int actionId, long daley, ActionType actionType, int targetParentTimes,String targetText,String targetTextViewId)
 new Step(QQChatHandler.PACKAGE_NAME,":id/listView1",AccessibilityNodeInfo.ACTION_CLICK,2000, Step.ActionType.normal,1,targetText,":id/text1")
 ***/

val GEMINI_TOOLS = Tool(
    listOf(
        ListFiles,FileWriter,FileReader,AppendText,SendFile,GetViewNode,DoStepOnNode
    )
)

data class FileInfo(val name: String, val isFile: Boolean,val size: Long,val time: Long)

data class FunctionDeclaration(
    val description: String,
    val name: String,
    val parameters: Parameters,
    @Transient
    val geminiCallFun: GeminiCallFun? = null
)

data class Tool(
    val functionDeclarations: List<FunctionDeclaration>
)

interface GeminiCallFun{
    fun call(functionCall: FunctionCall,thoughtSignature: String?,netAiAskAble: NetAiAskAble)
}