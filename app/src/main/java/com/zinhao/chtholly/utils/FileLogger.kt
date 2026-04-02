package com.zinhao.chtholly.utils

import android.content.Context
import android.util.Log
import com.zinhao.chtholly.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object FileLogger {

    private const val LOG_DIR = "logs"
    private const val LOG_FILE_NAME = "monitor_log.txt"
    private const val MAX_LOG_SIZE_BYTES = 1024 * 1024 // 5MB，超出可轮转
    private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"

    private var currentLogFile: File? = null
    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    /**
     * 初始化日志器（建议在 Application 或 Service 启动时调用）
     */
    fun init(context: Context) {
        val logDir = File(context.filesDir, LOG_DIR)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        currentLogFile = File(logDir, LOG_FILE_NAME)
    }

    /**
     * 写入 INFO 级别日志
     */
    fun i(tag: String, message: String) {
        writeLog("I", tag, message)
        if(BuildConfig.DEBUG){
            Log.i(tag,message)
        }
    }

    /**
     * 写入 DEBUG 级别日志
     */
    fun d(tag: String, message: String) {
        if(BuildConfig.DEBUG){
            Log.d(tag,message)
        }
        writeLog("D", tag, message)
    }

    /**
     * 写入 WARN 级别日志
     */
    fun w(tag: String, message: String) {
        writeLog("W", tag, message)
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
        Log.e(tag,fullMessage)
        writeLog("E", tag, fullMessage)
    }

    /**
     * 核心写入方法
     */
    private fun writeLog(level: String, tag: String, message: String) {
        scope.launch {
            val logFile = currentLogFile ?: return@launch
            val timestamp = dateFormatter.format(Date())
            val logLine = "$timestamp [$level/$tag]: $message\n"

            try {
                // 检查文件大小，必要时轮转
                if (logFile.length() > MAX_LOG_SIZE_BYTES) {
                    rotateLogFile()
                }

                FileWriter(logFile, true).use { writer ->
                    writer.write(logLine)
                    writer.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                // 可 fallback 到 Logcat
                Log.e("FileLogger", "写入日志失败: ${e.message}")
            }
        }

    }

    /**
     * 日志轮转：重命名当前文件，创建新文件
     */
    private fun rotateLogFile() {
        val logFile = currentLogFile ?: return
        val parent = logFile.parentFile ?: return
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val rotatedFile = File(parent, "${LOG_FILE_NAME.substringBeforeLast('.')}_$timestamp.txt")

        if (logFile.renameTo(rotatedFile)) {
            Log.i("FileLogger", "日志轮转: ${rotatedFile.name}")
        }

        // 创建新日志文件
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
        val logDir = File(context.cacheDir, LOG_DIR)
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
        getAllLogFiles(context).forEach { it.delete() }
        i("FileLogger", "所有日志已清空")
    }
}