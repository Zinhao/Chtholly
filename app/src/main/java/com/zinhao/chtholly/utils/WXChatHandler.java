package com.zinhao.chtholly.utils;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.media3.common.C;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.entity.Command;
import com.zinhao.chtholly.entity.Message;

import java.util.List;
import java.util.Locale;
import java.util.Vector;

public class WXChatHandler extends BaseChatHandler {
    public static final String WX_PACKAGE_NAME = "com.tencent.mm";
    // :id/c2s 晚上10：00
    // "id/by8" 聊天对话内容父布局，包含头像
    // "id/bv9" 聊天对话内容
    // id/buo 聊天对话内容头像
    private final List<Message> messageList = new Vector<>();
    private AccessibilityNodeInfo etInput;
    private AccessibilityNodeInfo btSend;
    public final ChatPageViewIds chatPageViewIds = new ChatPageViewIds();

    private static final String[] CHAT_PAGE_ID = new String[]{"id/buz","id/qhu","id/c2i","id/bwy","id/qhs",
            "id/pwi","id/q9o","id/actionbar_up_indicator","id/bvw","id/llp","id/c13","id/bv8"/*输入*/,"id/a97","id/qhq"/*聊天标题*/,
            "id/g7","id/bum","id/c0n"};

    public void initChatPage(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            Log.e(TAG, "initChatPage:nodeInfo null!");
            return;
        }
        //输入文本框id
        AccessibilityNodeInfo input = findFirstNodeInfo(nodeInfo, getPackageName()+":id/bv8");
        chatPageViewIds.setInputViewId(getPackageName()+":id/bv8");

        // 发送按钮id
        AccessibilityNodeInfo send;
        send = findFirstNodeInfo(nodeInfo, getPackageName()+":id/c2c");
        chatPageViewIds.setSendBtnViewId(getPackageName()+":id/c2c");

        // 聊天标题id
        AccessibilityNodeInfo title = findFirstNodeInfo(nodeInfo,getPackageName()+":id/qhq");
        chatPageViewIds.setTitleViewId(getPackageName()+":id/qhq");

        if(input!=null){
//            NekoChatService.getInstance().addLogcat("input edit find!");
            etInput = input;
        }
        if(send!=null){
//            NekoChatService.getInstance().addLogcat("send btn find!");
            btSend = send;
        }
        if (title != null) {
            String chatTitle = title.getText().toString();
            Log.d(TAG, "initChatPage:聊天界面:" + chatTitle);
        }
    }

    @Override
    public boolean writeAndSend(Command qa) {
        if (etInput != null) {
            etInput.refresh();
//            Bundle arguments = new Bundle();
//            arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 1);
//            arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 2);
//            etInput.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments);

            etInput.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            Log.d(TAG, "writeAndSend: 焦点"+etInput.isFocused());
            if (writeMessage(etInput, qa)) {
                NekoChatService.getInstance().addLogcat("write: id[" + etInput.getViewIdResourceName() + ']' + qa.getAnswer().getMessage());
            }
//            if (pasteMessage(etInput, qa)) {
//                NekoChatService.getInstance().addLogcat("paste: id[" + etInput.getViewIdResourceName() + ']' + qa.getAnswer().getMessage());
//            }
        }
        if(btSend!=null){
            if(qa.isWrite()){
                btSend.refresh();
                boolean result = BaseChatHandler.clickButton(btSend, qa);
                if (!result) {
                    NekoChatService.getInstance().addLogcat("send: id[" + btSend.getViewIdResourceName() + ']'+"点击发送按钮失败");
                }
                return result;
            }
        }
        return false;
    }

    @Override
    public String beforeWriteMessage(Command command) {
        return command.getAnswer().getMessage();
    }

    public WXChatHandler(MessageCallback messageCallback) {
        super(messageCallback);
    }

    @Override
    protected boolean isAtName(Message message, String name) {
        return false;
    }

    @Override
    public void handle(AccessibilityEvent event) {
        if(event == null)
            return;
        if(event.getSource() == null)
            return;
        initChatPage(event.getSource());
        //EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 99375618; PackageName: com.tencent.mm; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_TEXT]; WindowChangeTypes: [] [ ClassName: android.widget.TextVie
        //EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 99375717; PackageName: com.tencent.mm; MovementGranularity: 0; Action: 0; ContentChangeTypes: [CONTENT_CHANGE_TYPE_SUBTREE, CONTENT_CHANGE_TYPE_TEXT];
        if (hasAllId(event.getSource(),CHAT_PAGE_ID)) {
            // chat 文本消息
            findLastMessage(event.getSource());
        }
    }

    private void findLastMessage(AccessibilityNodeInfo source) {
        if(messageCallback == null){
            return;
        }
        Message hitMessage = null;
//        com.tencent.mm:id/by8
        List<AccessibilityNodeInfo> messageParent = source.findAccessibilityNodeInfosByViewId(getPackageName()+":id/by8");
        if(!messageParent.isEmpty()){
            AccessibilityNodeInfo last = messageParent.get(messageParent.size()-1);
            if(last.getChildCount()>=2){
                AccessibilityNodeInfo first = last.getChild(0);
                AccessibilityNodeInfo second = last.getChild(1);
                if(last.getChildCount() == 3 && first.getViewIdResourceName().equals(getPackageName()+":id/c2s")){
                    first = second;
                    second = last.getChild(2);
                }
                if(first.getViewIdResourceName().equals(getPackageName()+":id/bv9") && second.getViewIdResourceName().equals(getPackageName()+":id/buo")){
                    // 内容在先，是bot发出的消息。忽略
                }else if(second.getViewIdResourceName().equals(getPackageName()+":id/bv9") && first.getViewIdResourceName().equals(getPackageName()+":id/buo")){
                    hitMessage = new Message(BotApp.getInstance().getAdminName(),second.getText().toString(),System.currentTimeMillis());
                }else{
                    NekoChatService.getInstance().addLogcat("findLastMessage:消息结构例外");
                }
            }
        }
        if(hitMessage == null)
            return;
        if (!messageList.isEmpty()) {
            Message last = messageList.get(messageList.size() - 1);
            if (last.message.equals(hitMessage.message) && System.currentTimeMillis() - last.getTimeStamp() < 10000) {
                Log.d(TAG, "findLastMessage: in close time, same message:"+last.message);
                //in close time, same message
                return;
            }
        }

        NekoChatService.getInstance().addLogcat(String.format(Locale.US, "✨findAddNewChatMessage: %s:%s", hitMessage.speaker, hitMessage.message));
        BotApp.getInstance().insert(hitMessage);
        messageList.add(hitMessage);
        messageCallback.onFind(hitMessage);
    }


    @Override
    public String getPackageName() {
        return WX_PACKAGE_NAME;
    }
}
