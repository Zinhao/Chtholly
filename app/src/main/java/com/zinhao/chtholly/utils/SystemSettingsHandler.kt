package com.zinhao.chtholly.utils

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
import android.view.accessibility.AccessibilityNodeInfo
import com.zinhao.chtholly.entity.Command
import com.zinhao.chtholly.entity.Message

class SystemSettingsHandler(messageCallback: MessageCallback) : BaseChatHandler(messageCallback) {

    override fun isAtName(message: Message?, name: String?): Boolean {
       return false
    }

    override fun handle(event: AccessibilityEvent?) {
        if(event == null) return
        if((event.eventType and AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)  == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if((event.contentChangeTypes and CONTENT_CHANGE_TYPE_SUBTREE) == CONTENT_CHANGE_TYPE_SUBTREE){
                val source = event.source
                source?.let {
                    val cancelBt= findFirstNodeInfo(source,"android:id/button2")
                    val titleTv= findFirstTextInTargetNodeChildren(source,"USB 的用途","miui:id/alertTitle")
                    val onlyChargeTv= findFirstTextInTargetNodeChildren(source,"仅限充电","android:id/text1")
                    val mtpTv= findFirstTextInTargetNodeChildren(source,"传输文件 (MTP)","android:id/text1")
                    if(cancelBt!=null&&titleTv!=null&&onlyChargeTv!=null&&mtpTv!=null){
                        cancelBt.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                }
            }
        }
    }

    override fun getPackageName(): String {
        return PACKAGE_NAME
    }

    override fun writeAndSend(command: Command?): Boolean {
        return true
    }

    override fun beforeWriteMessage(command: Command?): String? {
        return command?.answer?.message
    }

    companion object {
        const val PACKAGE_NAME = "com.android.settings"
    }
}