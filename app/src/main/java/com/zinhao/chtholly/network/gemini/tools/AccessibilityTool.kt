package com.zinhao.chtholly.network.gemini.tools
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import com.zinhao.chtholly.session.GeminiSession
import org.json.JSONArray
import org.json.JSONObject

// 获取AccessibilityNode,遍历节点 转换为json，供Agent阅读
val GetViewNode = FunctionDeclaration(
    description = "Get the current screen layout nodes to understand what's on the screen.",
    name = "get_view_nodes",
    Parameters(
        properties = emptyMap(), // 不需要参数，直接获取当前屏幕
        required = listOf(),
        type = "object"
    ),
    funImpl = object : FunImpl {
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
    funImpl = object : FunImpl {
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