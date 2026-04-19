package com.zinhao.chtholly.handlerImpl

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.entity.Message
import com.zinhao.chtholly.utils.FileLogger
import com.zinhao.chtholly.utils.LayoutTreeUtils
import com.zinhao.chtholly.utils.LocalFileCache
import com.zinhao.chtholly.utils.MessageCallback
import com.zinhao.chtholly.utils.QQChatHandler
import java.util.Locale

class QQV13690Impl(context: Context, messageCallback: MessageCallback) : QQChatHandler(context, messageCallback) {
    var lastItemCount = 0
    var titleNode: AccessibilityNodeInfo? = null

    var doubleClickWaitFillMessage: Message? = null
    override fun handle(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.source == null) return
        val rootActive = NekoChatService.getInstance().rootInActiveWindow
        val etInputNode = findFirstNodeInfo(event.source,inputId)
        val btSendNode = findFirstNodeInfo(event.source,sendButtonId)
        etInputNode?.let {
            this.etInputNode?.recycle()
            this.etInputNode = it
        }
        btSendNode?.let {
            this.btSendNode?.recycle()
            this.btSendNode = it
        }
        val titleNode = findFirstNodeInfo(rootActive,chatTitleId)
        titleNode?.let {
            if(this.targetChatTitle == null){
                this.targetChatTitle = it.text.toString()
            }
            this.titleNode?.recycle()
            this.titleNode = it
        }
        val pageName: String = checkWhatPage(rootActive)
        pageName.let {
            currentPageName = it
        }
        /**
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045540; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE]; WindowChangeTypes: [] [ ClassName: androidx.recyclerview.widget.RecyclerView; Text: []; ContentDescription: null; ItemCount: 12; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: true; BeforeText: null; FromIndex: 0; ToIndex: 8; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045540; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE]; WindowChangeTypes: [] [ ClassName: androidx.recyclerview.widget.RecyclerView; Text: []; ContentDescription: null; ItemCount: 12; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: true; BeforeText: null; FromIndex: 0; ToIndex: 8; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045640; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE, CONTENT_CHANGE_TYPE_TEXT, CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION]; WindowChangeTypes: [] [ ClassName: android.widget.FrameLayout; Text: []; ContentDescription: null; ItemCount: -1; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: false; BeforeText: null; FromIndex: -1; ToIndex: -1; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045640; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE, CONTENT_CHANGE_TYPE_TEXT, CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION]; WindowChangeTypes: [] [ ClassName: android.widget.FrameLayout; Text: []; ContentDescrip
         */
        if ((event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
            && event.className == "androidx.recyclerview.widget.RecyclerView") {
            if (CHAT_GROUP == currentPageName) {
                if(event.itemCount == lastItemCount){

                }else{
                    FileLogger.d(TAG,"lastItemCount:${lastItemCount}, event.itemCount = ${event.itemCount}")
                    lastItemCount = event.itemCount
                    // chat 文本消息
                    findLastMessage(NekoChatService.getInstance().rootInActiveWindow)
                }
            }
        }
        if("com.tencent.mobileqq.activity.TextPreviewActivity" == event.className){
            event.source?.let {
                j8lFindText(it)
            }
        }
    }

    private fun j8lFindText(nodeInfo: AccessibilityNodeInfo){
        if(doubleClickWaitFillMessage == null){
            return
        }
        if(nodeInfo.childCount!=0){
            val j8lNode = findFirstNodeInfo(nodeInfo,PACKAGE_NAME+":id/j8l")
            j8lNode?.let {
                val messageText = it.getChild(0).text.toString()
                doubleClickWaitFillMessage!!.message = messageText
                if (!isAtName(doubleClickWaitFillMessage, BotApp.getInstance().botName)) {
                    if (!doubleClickWaitFillMessage!!.isOther) {
                        return
                    }
                }
                doubleClickWaitFillMessage?.message = messageText.replace("@" + BotApp.getInstance().botName, "").trim { it <= ' ' }
                if (messageList.isNotEmpty()) {
                    val last = messageList.last()
                    if (last.message == doubleClickWaitFillMessage?.message && System.currentTimeMillis() - last.getTimeStamp() < 10000) {
                        Log.d(
                            TAG,
                            "findLastMessage: in close time, same message:" + last.message
                        )
                        //in close time, same message
                        return
                    }
                }

                if("群主" == doubleClickWaitFillMessage?.tag || "管理员" == doubleClickWaitFillMessage?.tag) {
                    doubleClickWaitFillMessage?.isEnableCommand = true
                }

                if (NekoChatService.getInstance() != null) {
                    NekoChatService.getInstance().addLogcat(
                        String.format(
                            Locale.US,
                            "✨findLastMessage:%s",
                            doubleClickWaitFillMessage.toString(),
                        )
                    )
                }
                BotApp.getInstance().insert(doubleClickWaitFillMessage)
                messageList.add(doubleClickWaitFillMessage)
                messageCallback.onFind(doubleClickWaitFillMessage)
            }
        }
    }

    private fun doubleClickLastMessage(rootItemNode: AccessibilityNodeInfo): Message {
        val emptyMessage = Message(null, null, System.currentTimeMillis())
        for (j in 0..<rootItemNode.childCount) {
            val messageItemChild = rootItemNode.getChild(j) ?: continue
            if ("android.widget.TextView" == messageItemChild.className && j == 0) {
                //"text": "下午2:11"
            }
            if("android.widget.RelativeLayout" == messageItemChild.className && j ==1){
                if(messageItemChild.viewIdResourceName.contains(":id/mgo")){
                    val mgoChild = messageItemChild.getChild(0)
                    if(mgoChild!=null){
                        val nickName = mgoChild.contentDescription.toString()
                        emptyMessage.speaker = nickName.replace("的资料卡","")
                        mgoChild.recycle()
                    }
                }
            }
            if ("android.widget.FrameLayout" == messageItemChild.className && j == 2) {
                for (k in 0..<messageItemChild.getChildCount()) {
                    val nbtChild = messageItemChild.getChild(k)
                    if ("android.widget.TextView".contentEquals(nbtChild.getClassName()) && k == 0) {
                        val leveAndTag = nbtChild.text.toString()
                        if(leveAndTag.contains(" ")){
                            val sp = leveAndTag.split(" ")
                            if(sp.size == 2){
                                emptyMessage.tag = sp[1]
                            }
                        }else{
                            emptyMessage.tag = leveAndTag
                        }
                    }
                    nbtChild.recycle()
                }
            }
            if ("android.widget.LinearLayout" == messageItemChild.className) {
                if(NekoChatService.getInstance()!=null){
                    NekoChatService.getInstance().doDoubleClick(messageItemChild,{
                        NekoChatService.getInstance().performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    })
                }
            }
            messageItemChild.recycle()
        }
        FileLogger.d(TAG,emptyMessage.toString())
        return emptyMessage
    }

    override fun findLastMessage(nodeInfo: AccessibilityNodeInfo) {
        if (messageCallback == null) {
            return
        }
        val grayBar = id2FindGroupLastMessage(nodeInfo)
        grayBar?.let { grayBarMessage->
            BotApp.getInstance().insert(grayBarMessage)
            messageList.add(grayBarMessage)
            messageCallback.onFind(grayBarMessage)
        }
    }

    override fun id2FindGroupLastMessage(nodeInfo: AccessibilityNodeInfo): Message? {
        val layoutTree = LayoutTreeUtils.treeAndPrintLayout(nodeInfo,0,false)
        LocalFileCache.getInstance()
            .saveJSONObject(BotApp.getInstance().applicationContext, layoutTree, "qq_v13690.new_message.json")

        val vd6NodeList = nodeInfo.findAccessibilityNodeInfosByViewId(PACKAGE_NAME+":id/vd6")
        val rootNodeList = nodeInfo.findAccessibilityNodeInfosByViewId(chatMessageItemRootId)
        var isChatMessage = false

        if(vd6NodeList != null && vd6NodeList.isNotEmpty()){
            val rootLast = rootNodeList?.last()
            val vd6Last = vd6NodeList.last()
            if(rootLast == null){
                isChatMessage = false
            }else{
                val rLB = Rect()
                val vd6LB = Rect()
                rootLast.getBoundsInScreen(rLB)
                vd6Last.getBoundsInScreen(vd6LB)
                isChatMessage = rLB.top > vd6LB.top
            }
        }else{
            isChatMessage = true
        }
        if(isChatMessage){
            val lastNotFillMessage = doubleClickLastMessage(rootNodeList!!.last())
            lastNotFillMessage.let {
                doubleClickWaitFillMessage = it
            }
        }else{
            vd6NodeList?.let {
                if(it.isNotEmpty()){
                    val emptyMessage = Message(null, null, System.currentTimeMillis())
                    val textNode = it.last().getChild(0)
                    textNode?.let { text->
                        emptyMessage.message = "(${text.text})"
                        textNode.recycle()
                        return emptyMessage
                    }
                }
            }
        }
        return null
    }

    override fun checkWhatPage(root: AccessibilityNodeInfo?): String {
        if(root == null){
            return UNKNOWN_PAGE
        }
        if(hasAllId(root, chatMessageItemRootId,chatTitleId,":id/mgo")){
            return CHAT_GROUP
        }else if(hasAllId(root,":id/1ko",":id/root",":id/a46",":id/ec3",":id/y_h")){
            return MESSAGE_PAGE
        }else if(hasAllId(root,":id/20r",":id/j64",":id/pdl")){
            return CHAT_PERSON
        }
        return UNKNOWN_PAGE
    }

    override fun getChatMessageItemRootId(): String {
        return "$PACKAGE_NAME:id/root"
    }

    override fun getChatTitleId(): String {
        return "$PACKAGE_NAME:id/20r"
    }

    override fun getInputId(): String {
        return "$PACKAGE_NAME:id/input"
    }

    override fun getSendButtonId(): String {
        return "$PACKAGE_NAME:id/send_btn"
    }

    override fun getChatTitle(): String {
        return titleNode?.text.toString()
    }
}