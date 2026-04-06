package com.zinhao.chtholly.utils;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.media3.container.NalUnitUtil;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.entity.Command;
import com.zinhao.chtholly.entity.Message;

import java.util.List;
import java.util.Locale;
import java.util.Vector;

public class RBChatHandler extends BaseChatHandler{
    public static final String PACKAGE_NAME = "com.xingin.xhs";
    private final List<Message> messageList = new Vector<>();
    boolean firstInit = true;
    public RBChatHandler(MessageCallback messageCallback) {
        super(messageCallback);
    }

    @Override
    protected boolean isAtName(Message message, String name) {
        return true;
    }

    @Override
    public void handle(AccessibilityEvent event) {
        if(event == null)
            return;
        if(event.getSource() == null)
            return;
        Log.i(TAG,"handle");
        // message rec
        //getEventStringBuilder: EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 163384217; PackageName: com.xingin.xhs;
        // MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE];
        // WindowChangeTypes: [] [ ClassName: androidx.recyclerview.widget.RecyclerView;
        // Text: []; ContentDescription: null; ItemCount: 9; CurrentItemIndex: -1;
        // Enabled: true; Password: false; Checked: false; FullScreen: false;
        // Scrollable: true; BeforeText: null; FromIndex: 0;
        // ToIndex: 7; ScrollX: 0; ScrollY: 0; MaxScrollX: 0;
        // MaxScrollY: 0; ScrollDeltaX: -1; ScrollDeltaY: -1; AddedCount: -1; RemovedCount: -1; ParcelableData: null ]; recordCount: 0
        //2025-12-21 23:59:26.996 25687-25687 FindMessageHandler      com.zinhao.chtholly                  I  handle
        if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) {
            if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
                // chat 文本消息
//                if (QQChatHandler.CHAT_PAGE.equals(checkWhatPage(event.getSource()))) {
                    initChatPage(event.getSource());

                    if(firstInit&&etInput!= null){
                        firstInit = false;
                        Log.i(TAG,"firstInit 1");
                        Message initMessage = new Message("system","init",System.currentTimeMillis());
                        messageList.add(initMessage);
                        messageCallback.onFind(initMessage);
                    }
//                    findLastMessage(event.getSource());
//                }
            }
        } else {
            if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
                initChatPage(event.getSource());
                if(firstInit && etInput!= null){
                    firstInit = false;
                    Log.i(TAG,"firstInit 2");
                    Message initMessage = new Message("system","init",System.currentTimeMillis());
                    messageList.add(initMessage);
                    messageCallback.onFind(initMessage);
                }
            }
        }
    }

    private AccessibilityNodeInfo etInput;
    private AccessibilityNodeInfo btSend;
    public final ChatPageViewIds chatPageViewIds = new ChatPageViewIds();
    private void initChatPage(AccessibilityNodeInfo nodeInfo){
        if (nodeInfo == null) {
            Log.e(TAG, "initChatPage:nodeInfo null!");
            return;
        }
        //todo 目前只是适配qq聊天界面
        if (!PACKAGE_NAME.equals(nodeInfo.getPackageName().toString())) {
            return;
        }
        //输入文本框id
        AccessibilityNodeInfo input = findFirstNodeInfo(nodeInfo, PACKAGE_NAME+":id/chatInputContentView");
        chatPageViewIds.setInputViewId(":id/chatInputContentView");

        // 发送按钮id
        AccessibilityNodeInfo send;
        send = findFirstNodeInfo(nodeInfo, PACKAGE_NAME+":id/chatPushView");
        chatPageViewIds.setSendBtnViewId(":id/chatPushView");

        // 聊天标题id
        AccessibilityNodeInfo title = findFirstNodeInfo(nodeInfo, PACKAGE_NAME+":id/title");
        chatPageViewIds.setTitleViewId(":id/title");

        if (input == null) {
            Log.d(TAG, "initChatPage:非聊天界面");
            return;
        }
        etInput = input;
        btSend = send;
        if (title != null) {
            /***
             * 机器人信息和聊天信息
             */
            String chatTitle = title.getText().toString();
            Log.d(TAG, "initChatPage:聊天界面:" + chatTitle);

        }
    }

    private void findLastMessage(AccessibilityNodeInfo nodeInfo){
        Message last = id2FindPersonLastMessage(nodeInfo);
    }

    public Message id2FindPersonLastMessage(AccessibilityNodeInfo nodeInfo){
        List<AccessibilityNodeInfo> messageNodes = nodeInfo.findAccessibilityNodeInfosByViewId(PACKAGE_NAME +":id/chatContentView");
        if(messageNodes.isEmpty())
            return null;

        for (int i = 0; i < messageNodes.size(); i++) {
            AccessibilityNodeInfo m = messageNodes.get(i);
            Log.d(TAG, String.format(Locale.CHINA,"id2FindLastMessage: : %s",m.getText()));
        }
        int lastIndex = messageNodes.size()-1;
        AccessibilityNodeInfo lastNodeInfo = messageNodes.get(lastIndex);
        CharSequence text = lastNodeInfo.getText();
        if(text != null){
            Message emptyMessage = new Message(null,null,System.currentTimeMillis());
            emptyMessage.setSpeaker(BotApp.getInstance().getAdminName());
            emptyMessage.setMessage(text.toString());
            return emptyMessage;
        }else {
            return null;
        }
    }


    @Override
    public String getPackageName() {
        return "com.xingin.xhs";
    }

    @Override
    public boolean writeAndSend(Command command) {
        Log.e(TAG, "writeAndSend: "+( btSend == null));
        if (etInput != null ) {
            etInput.refresh();
            if (writeMessage(etInput, command)) {
                if(btSend!=null){
                    btSend.refresh();
                    boolean result = BaseChatHandler.clickButton(btSend, command);
                    if (!result) {
                        NekoChatService.getInstance().addLogcat("writeAndSend: id[" + btSend.getViewIdResourceName() + ']'+"点击发送按钮失败");
                    }
                    return result;
                }
            }
        }
        return false;
    }

    @Override
    public String beforeWriteMessage(Command command) {
        return command.getAnswer().getMessage();
    }
}
