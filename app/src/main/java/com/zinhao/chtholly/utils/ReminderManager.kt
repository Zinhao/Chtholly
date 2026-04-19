package com.zinhao.chtholly.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.*


object ReminderManager {
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // 用于处理 List<ReminderItem> 的序列化
    private val listType = Types.newParameterizedType(List::class.java, ReminderItem::class.java)
    private val reminderListAdapter: JsonAdapter<List<ReminderItem>> = moshi.adapter(listType)

    private val reminderList: ArrayList<ReminderItem> = arrayListOf()

    // 定义时间格式：20231027T1030
    private val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmm", Locale.getDefault())

    fun create(title: String, startTimeStr: String, allDay: Boolean): Boolean {
        return try {
            // 将字符串转为毫秒数
            val date = dateFormat.parse(startTimeStr)
            val timeInMillis = date?.time ?: System.currentTimeMillis()

            val newItem = ReminderItem(title, timeInMillis, allDay)
            reminderList.add(newItem)
            reminderList.sortBy { it.startTime }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 根据时间范围获取提醒列表
     * @param fromStr 起始时间字符串 (可为空，默认从 0 开始)
     * @param toStr 结束时间字符串 (可为空，默认到未来无限远)
     */
    fun getReminders(fromStr: String?, toStr: String?): String {
        return try {
            // 解析起始时间，如果为空则设为 0
            val fromTime = if (!fromStr.isNullOrEmpty()) {
                dateFormat.parse(fromStr)?.time ?: 0L
            } else {
                0L
            }

            // 解析结束时间，如果为空则设为最大值
            val toTime = if (!toStr.isNullOrEmpty()) {
                dateFormat.parse(toStr)?.time ?: Long.MAX_VALUE
            } else {
                Long.MAX_VALUE
            }

            // 过滤列表：筛选出在时间范围内的提醒
            val filteredList = reminderList.filter { item ->
                item.startTime in fromTime..toTime
            }.sortedBy { it.startTime } // 按时间先后排序，方便 AI 阅读

            reminderListAdapter.toJson(filteredList)
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }

    fun getNextReminder(): ReminderItem? {
        if(reminderList.isEmpty()) return null
        return reminderList[0]
    }

    fun getReminders(): String {
        return try {
            // 将整个列表转换为 JSON 字符串
            reminderListAdapter.toJson(reminderList)
        } catch (e: Exception) {
            "[]"
        }
    }

    // 辅助方法：清除已过期的提醒（可选）
    fun clearExpired() {
        val now = System.currentTimeMillis()
        reminderList.removeAll { it.startTime < now }
    }

    data class ReminderItem(
        val title: String,
        val startTime: Long,
        val allDay: Boolean
    )
}