package com.zinhao.chtholly.handlerImpl

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zinhao.chtholly.entity.Message
import com.zinhao.chtholly.utils.MessageCallback
import com.zinhao.chtholly.utils.QQChatHandler

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
            this.btSendNode.recycle()
            this.btSendNode = it
        }
        val titleNode = findFirstNodeInfo(event.source,chatTitleId)
        titleNode?.let {
            this.titleNode?.recycle()
            this.titleNode = it
        }
        /**
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045540; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE]; WindowChangeTypes: [] [ ClassName: androidx.recyclerview.widget.RecyclerView; Text: []; ContentDescription: null; ItemCount: 12; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: true; BeforeText: null; FromIndex: 0; ToIndex: 8; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045540; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE]; WindowChangeTypes: [] [ ClassName: androidx.recyclerview.widget.RecyclerView; Text: []; ContentDescription: null; ItemCount: 12; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: true; BeforeText: null; FromIndex: 0; ToIndex: 8; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045640; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE, CONTENT_CHANGE_TYPE_TEXT, CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION]; WindowChangeTypes: [] [ ClassName: android.widget.FrameLayout; Text: []; ContentDescription: null; ItemCount: -1; CurrentItemIndex: -1; Enabled: true; Password: false; Checked: false; FullScreen: false; Scrollable: false; BeforeText: null; FromIndex: -1; ToIndex: -1; ScrollX: 0; ScrollY: 0; MaxScrollX: 0; MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
         * debug: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 22045640; PackageName: com.tencent.mobileqq; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE, CONTENT_CHANGE_TYPE_TEXT, CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION]; WindowChangeTypes: [] [ ClassName: android.widget.FrameLayout; Text: []; ContentDescrip
         */
        if ((event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) {
            if ((event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
                // chat 文本消息
                if (CHAT_PAGE == checkWhatPage(event.source)) {
                    findLastMessage(event.source)
                }
            }
        }
    }

    private fun root2FindGroupAllMessage(nodeInfo: AccessibilityNodeInfo): MutableList<Message?> {
        val allMessages: MutableList<Message?> = ArrayList<Message?>()
        val messageItemList =
            nodeInfo.findAccessibilityNodeInfosByViewId(chatMessageItemRootId)
        for (i in messageItemList.indices) {
            val messageItem = messageItemList.get(i)
            val emptyMessage = Message(null, null, System.currentTimeMillis())
            for (j in 0..<messageItem.childCount) {
                val messageItemChild = messageItem.getChild(j) ?: continue
                if ("android.widget.TextView" == messageItemChild.className && j == 0) {
                    //"text": "下午2:11"
                }
                if("android.widget.RelativeLayout" == messageItemChild.className && j ==1){

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
                if (getChatNickId() == messageItemChild.getViewIdResourceName()) {
                    // ab6[3] = chat_item_nick_name[text] = 发言人
                    val textOrNull = messageItemChild.getText()
                    if (textOrNull != null) {
                        emptyMessage.setSpeaker(textOrNull.toString())
                    }
                }
                if (getChatTextId() == messageItemChild.getViewIdResourceName()) {
                    // ab6[4] = chat_item_content_layout[text] = 消息正文
                    val textOrNull = messageItemChild.getText()
                    if (textOrNull != null) {
                        emptyMessage.setMessage(textOrNull.toString())
                    }
                }
                messageItemChild.recycle()
            }
            //            FileLogger.INSTANCE.d(TAG,emptyMessage.toString());
            allMessages.add(emptyMessage)
        }
        return allMessages
    }

    override fun checkWhatPage(root: AccessibilityNodeInfo?): String? {
        if(hasAllId(root, chatMessageItemRootId,":id/mgo")){
            return CHAT_PAGE
        }
        return UNKNOWN_PAGE
    }

    override fun getChatMessageItemRootId(): String {
        return "$PACKAGE_NAME:id/root"
    }

    override fun findLastMessage(nodeInfo: AccessibilityNodeInfo?) {

        super.findLastMessage(nodeInfo)
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