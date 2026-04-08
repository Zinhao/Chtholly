package com.zinhao.chtholly.handlerImpl

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.entity.Message
import com.zinhao.chtholly.utils.FileLogger
import com.zinhao.chtholly.utils.MessageCallback
import com.zinhao.chtholly.utils.QQChatHandler
import java.util.Locale

class QQV9275Impl(context: Context, messageCallback: MessageCallback) : QQChatHandler(context, messageCallback) {

    var titleNode: AccessibilityNodeInfo? = null
    override fun handle(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.source == null) return

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
        val titleNode = findFirstNodeInfo(event.source,chatTitleId)
        titleNode?.let {
            this.titleNode?.recycle()
            this.titleNode = it
        }
        val pageName: String = checkWhatPage(NekoChatService.getInstance().rootInActiveWindow)
        pageName.let {
            currentPageName = it
        }

        /**
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045540; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE]; WindowChangeTypes: [] [ ClassName: androidx.recyclerview.widget.RecyclerView; Text: []; ContentDescription: null; ItemCount: 12; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: true; BeforeText: null; FromIndex: 0; ToIndex: 8; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045540; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE]; WindowChangeTypes: [] [ ClassName: androidx.recyclerview.widget.RecyclerView; Text: []; ContentDescription: null; ItemCount: 12; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: true; BeforeText: null; FromIndex: 0; ToIndex: 8; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045640; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE, CONTENT_CHANGE_TYPE_TEXT, CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION]; WindowChangeTypes: [] [ ClassName: android.widget.FrameLayout; Text: []; ContentDescription: null; ItemCount: -1; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: false; BeforeText: null; FromIndex: -1; ToIndex: -1; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045640; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE, CONTENT_CHANGE_TYPE_TEXT, CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION]; WindowChangeTypes: [] [ ClassName: android.widget.FrameLayout; Text: []; ContentDescrip
         */
        if ((event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
            // chat 文本消息
            if (CHAT_GROUP == currentPageName) {
                findLastMessage(NekoChatService.getInstance().rootInActiveWindow)
            }
        }
    }

    private fun root2FindGroupAllMessage(nodeInfo: AccessibilityNodeInfo): MutableList<Message?> {
        val allMessages: MutableList<Message?> = ArrayList<Message?>()
        val messageItemList =
            nodeInfo.findAccessibilityNodeInfosByViewId(chatMessageItemRootId)
        Log.d(TAG,"======================================find ${messageItemList.size} message")
        for (i in messageItemList.indices) {
            val messageItem = messageItemList[i]
            val emptyMessage = Message(null, null, System.currentTimeMillis())
            for (j in 0..<messageItem.childCount) {
                val messageItemChild = messageItem.getChild(j) ?: continue
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
                    // {
                    //             "class": "android.widget.FrameLayout",
                    //
                    //                  "children": [
                    //                    {
                    //                      "class": "android.widget.TextView",
                    //                      "desc": "",
                    //                      "text": "LV2 群主"
                    //                    }
                    //                  ]
                    //                },
                    for (k in 0..<messageItemChild.getChildCount()) {
                        val nbtChild = messageItemChild.getChild(k)
                        if ("android.widget.TextView".contentEquals(nbtChild.getClassName()) && k == 0) {
                            val leveAndTag = nbtChild.getText().toString()
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
                    /***
                     * {
                     *                   "class": "android.widget.LinearLayout",
                     *                   "click": true,
                     *                   "longClick": false,
                     *                   "desc": "@冰糖 你好 "
                     *                 }
                     */
                    val textOrNull = messageItemChild.contentDescription
                    textOrNull?.let {
                        emptyMessage.setMessage(it.toString())
                    }
                }
                messageItemChild.recycle()
            }
            FileLogger.d(TAG,emptyMessage.toString());
            allMessages.add(emptyMessage)
        }
        return allMessages
    }

    override fun findLastMessage(nodeInfo: AccessibilityNodeInfo) {
        if (messageCallback == null) {
            return
        }
        val botName = BotApp.getInstance().getBotName()
        // 通过状态view判断是不是
        val isPersonal = currentPageName == CHAT_PERSON
        var hitMessage: Message? = null
        hitMessage = id2FindGroupLastMessage(nodeInfo)
        if (hitMessage == null) {
            return
        }
        if (hitMessage.message == null) {
            return
        }
        if (!isPersonal) {
            if (!isAtName(hitMessage, botName)) {
                if (!hitMessage.isOther()) {
                    return
                }
            }
            if (botName == hitMessage.speaker) {
                return
            }
        }
        hitMessage.message = hitMessage.message.replace("@" + botName, "").trim { it <= ' ' }
        if (!messageList.isEmpty()) {
            val last = messageList.get(messageList.size - 1)
            if (last.message == hitMessage.message && System.currentTimeMillis() - last.getTimeStamp() < 10000) {
                Log.d(
                    TAG,
                    "findLastMessage: in close time, same message:" + last.message
                )
                //in close time, same message
                return
            }
        }
        if (NekoChatService.getInstance() != null) {
            NekoChatService.getInstance().addLogcat(
                String.format(
                    Locale.US,
                    "✨findAddNewChatMessage: %s:%s",
                    hitMessage.speaker,
                    hitMessage.message
                )
            )
        }
        BotApp.getInstance().insert(hitMessage)
        messageList.add(hitMessage)
        messageCallback.onFind(hitMessage)
    }

    override fun id2FindGroupLastMessage(nodeInfo: AccessibilityNodeInfo): Message? {
        val grayBarHitMessage = grayBarMessage(nodeInfo)
        if (grayBarHitMessage != null) {
            return grayBarHitMessage
        }
        val a6bMessageList = root2FindGroupAllMessage(nodeInfo)
        if (!a6bMessageList.isEmpty()) {
            return a6bMessageList.get(a6bMessageList.size - 1)
        }
        return null
    }

    override fun checkWhatPage(root: AccessibilityNodeInfo?): String {
        if(hasAllId(root, chatMessageItemRootId,chatTitleId,":id/mgo")){
            return CHAT_GROUP
        }else if(hasAllId(root,":id/a2o",":id/2jl",":id/ba1",":id/ba3",":id/y9d")){
            return MESSAGE_PAGE
        }else if(hasAllId(root,":id/1yo",":id/j64",":id/zy3")){
            return CHAT_PERSON
        }
        return UNKNOWN_PAGE
    }

    override fun getChatMessageItemRootId(): String {
        return "$PACKAGE_NAME:id/root"
    }

    override fun getChatTitleId(): String {
        return "$PACKAGE_NAME:id/21l"
    }

    override fun getInputId(): String {
        return "$PACKAGE_NAME:id/input"
    }

    override fun getSendButtonId(): String {
        return "$PACKAGE_NAME:id/send_btn"
    }

    override fun getChatTitle(): String {
        return titleNode?.contentDescription.toString()
    }
}