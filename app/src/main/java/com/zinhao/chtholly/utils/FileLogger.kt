package com.zinhao.chtholly.utils

import android.content.Context
import android.util.Log
import com.zinhao.chtholly.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object FileLogger {

    private const val LOG_DIR = "logs"
    private const val LOG_FILE_NAME = "Chtholly_log.txt"
    private const val MAX_LOG_SIZE_BYTES = 1024 * 1024 // 1MB
    private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"

    private var currentLogFile: File? = null
    private var logWriter: BufferedWriter? = null  // 保持打开的文件句柄
    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 使用 Channel 实现单线程写入队列，彻底避免并发冲突
    private data class LogEntry(val level: String, val tag: String, val message: String)
    private val logChannel = Channel<LogEntry>(Channel.BUFFERED)

    /**
     * 初始化日志器（建议在 Application 或 Service 启动时调用）
     */
    fun init(context: Context) {
        val logDir = File(context.filesDir, LOG_DIR)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        currentLogFile = File(logDir, LOG_FILE_NAME)

        // 启动单消费者协程，串行处理所有写入
        scope.launch {
            for (entry in logChannel) {
                writeLogInternal(entry.level, entry.tag, entry.message)
            }
        }
    }

    /**
     * 写入 INFO 级别日志
     */
    fun i(tag: String, message: String) {
        sendToChannel("I", tag, message)
        if(BuildConfig.DEBUG){
            Log.i(tag, message)
        }
    }

    /**
     * 写入 DEBUG 级别日志
     */
    fun d(tag: String, message: String) {
        if(BuildConfig.DEBUG){
            Log.d(tag, message)
        }
        sendToChannel("D", tag, message)
    }

    /**
     * 写入 WARN 级别日志
     */
    fun w(tag: String, message: String) {
        sendToChannel("W", tag, message)
        if(BuildConfig.DEBUG){
            Log.w(tag, message)
        }
    }

    /**
     * 写入 ERROR 级别日志（可带异常）
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        Log.e(tag, fullMessage)
        sendToChannel("E", tag, fullMessage)
    }

    /**
     * 发送日志到 Channel（非阻塞）
     */
    private fun sendToChannel(level: String, tag: String, message: String) {
        val result = logChannel.trySend(LogEntry(level, tag, message))
        if (!result.isSuccess) {
            // Channel 满了，降级到 Logcat，避免阻塞业务线程
            Log.w("FileLogger", "日志队列已满，丢弃日志: [$level/$tag] $message")
        }
    }

    /**
     * 核心写入方法（单线程串行执行，通过 Channel 保证顺序）
     */
    private suspend fun writeLogInternal(level: String, tag: String, message: String) {
        val logFile = currentLogFile ?: return

        try {
            // 检查文件大小，必要时轮转
            if (logFile.length() > MAX_LOG_SIZE_BYTES) {
                rotateLogFile()
            }

            // 延迟初始化 writer，保持文件句柄打开
            if (logWriter == null) {
                logWriter = BufferedWriter(FileWriter(logFile, true))
            }

            val timestamp = dateFormatter.format(Date())
            val logLine = "$timestamp [$level/$tag]: $message"

            logWriter?.write(logLine)
            logWriter?.newLine()
            logWriter?.flush()  // 立即刷新，确保日志不丢失
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("FileLogger", "写入日志失败: ${e.message}")
            // 出错时重置 writer，下次会重新创建
            closeWriter()
        }
    }

    /**
     * 关闭当前 writer
     */
    private fun closeWriter() {
        try {
            logWriter?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            logWriter = null
        }
    }

    /**
     * 日志轮转：重命名当前文件，创建新文件
     */
    private fun rotateLogFile() {
        closeWriter()  // 轮转前先关闭 writer

        val logFile = currentLogFile ?: return
        val parent = logFile.parentFile ?: return
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val rotatedFile = File(parent, "${LOG_FILE_NAME.substringBeforeLast('.')}_$timestamp.txt")

        try {
            if (logFile.exists() && logFile.renameTo(rotatedFile)) {
                Log.i("FileLogger", "日志轮转成功: ${rotatedFile.name}")
            }
        } catch (e: Exception) {
            Log.e("FileLogger", "日志轮转失败: ${e.message}")
        }

        // 创建新日志文件，writer 会在下次写入时自动重建
        currentLogFile = File(parent, LOG_FILE_NAME)
    }

    /**
     * 获取日志文件（用于分享或导出）
     */
    fun getLogFile(): File? {
        return currentLogFile
    }

    /**
     * 获取所有日志文件（包括轮转的）
     */
    fun getAllLogFiles(context: Context): List<File> {
        // 修复：统一使用 filesDir（原代码使用 cacheDir 是 bug）
        val logDir = File(context.filesDir, LOG_DIR)
        return if (logDir.exists()) {
            logDir.listFiles { file ->
                file.isFile && file.name.startsWith(LOG_FILE_NAME.substringBeforeLast('.'))
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * 清空所有日志（谨慎使用）
     */
    fun clearLogs(context: Context) {
        closeWriter()  // 先关闭 writer，避免文件被占用导致删除失败
        getAllLogFiles(context).forEach { file ->
            try {
                file.delete()
            } catch (e: Exception) {
                Log.e("FileLogger", "删除日志文件失败: ${file.name}, ${e.message}")
            }
        }
        // 重建文件句柄
        currentLogFile = File(context.filesDir, "$LOG_DIR/$LOG_FILE_NAME")
        Log.i("FileLogger", "所有日志已清空")
    }

    /**
     * 释放资源（建议在 Application onTerminate 或 Service onDestroy 中调用）
     */
    fun close() {
        logChannel.close()  // 关闭 Channel，停止接收新日志
        scope.launch {
            closeWriter()     // 关闭文件句柄
            scope.cancel()    // 取消协程
        }
    }
}