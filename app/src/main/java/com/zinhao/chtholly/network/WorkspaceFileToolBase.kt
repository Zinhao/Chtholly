package com.zinhao.chtholly.network

import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.utils.LocalFileCache
import org.json.JSONObject
import java.io.File

abstract class WorkspaceFileToolBase {

    /** 是否允许操作符号链接 */
    protected open val allowSymlinks: Boolean = false

    /** 禁止访问的文件名（精确匹配） */
    protected open val blockedFileNames: Set<String> =
        setOf(".env", "credentials.json", "secret.json", "id_rsa")

    /** 禁止访问的扩展名 */
    protected open val blockedExtensions: Set<String> =
        setOf("key", "pem", "jks", "p12")

    /** 单次最大文件数（list 用） */
    protected open val maxListFiles: Int = 1000

    /**
     * 统一入口
     */
    fun execute(
        functionCall: FunctionCall,
        callback: ToolCallback,
        netAiAskAble: NetAiAskAble,
        thoughtSignature: String?
    ) {
        try {
            val relativePath = resolveRelativePath(functionCall)
            val targetFile = resolveAndValidateFile(relativePath)

            onValidatedFile(
                functionCall,
                targetFile,
                relativePath,
                callback,
                thoughtSignature
            )

        } catch (e: SecurityException) {
            callback.addToolErr(
                functionCall.name,
                IllegalArgumentException("Access denied: ${e.message}"),
                thoughtSignature
            )
        } catch (e: IllegalArgumentException) {
            callback.addToolErr(functionCall.name, e, thoughtSignature)
        } catch (e: Exception) {
            callback.addToolErr(
                functionCall.name,
                IllegalStateException("Tool execution failed: ${e.message}"),
                thoughtSignature
            )
        }
    }

    /** 子类实现真实逻辑 */
    protected abstract fun onValidatedFile(
        functionCall: FunctionCall,
        file: File,
        relativePath: String,
        callback: ToolCallback,
        thoughtSignature: String?
    )

    /** 解析相对路径（子类可覆盖） */
    protected open fun resolveRelativePath(functionCall: FunctionCall): String {
        return functionCall.args["path"]?.toString()
            ?.takeIf { it.isNotBlank() && it != "." && it != "/" }
            ?: ""
    }

    /** 核心安全校验 */
    protected open fun resolveAndValidateFile(relativePath: String): File {
        val workspaceDir = LocalFileCache.getInstance()
            .getWorkSpaceDir()
            .canonicalFile

        val targetFile = if (relativePath.isEmpty()) {
            workspaceDir
        } else {
            File(workspaceDir, relativePath).canonicalFile
        }

        // 1. workspace 边界
        if (!targetFile.path.startsWith(workspaceDir.path)) {
            throw SecurityException("Path escapes workspace")
        }

        // 2. symlink 检查
        if (!allowSymlinks && targetFile.exists() && targetFile.toPath().let {
                java.nio.file.Files.isSymbolicLink(it)
            }) {
            throw SecurityException("Symbolic links are not allowed")
        }

        // 3. 黑名单检查
        if (targetFile.name in blockedFileNames) {
            throw SecurityException("File is blocklisted")
        }

        if (blockedExtensions.any { targetFile.name.endsWith(".$it") }) {
            throw SecurityException("File extension is blocklisted")
        }

        return targetFile
    }

    /** 统一成功返回 */
    protected fun success(
        callback: ToolCallback,
        functionCall: FunctionCall,
        action: String,
        json: JSONObject,
        thoughtSignature: String?
    ) {
        callback.addToolResponse(
            functionCall.name,
            action,
            json.toString(),
            thoughtSignature
        )
    }
}