package com.zinhao.chtholly.utils;

import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.Nullable;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.entity.Command;
import com.zinhao.chtholly.entity.Message;

import java.util.List;

public abstract class BaseChatHandler {
    public final String TAG = "FindMessageHandler";
    protected abstract boolean isAtName(Message message,String name);
    protected MessageCallback messageCallback;
    public abstract void handle(AccessibilityEvent event);
    public abstract String getPackageName();
    public BaseChatHandler(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    public void setMessageCallback(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    public abstract boolean writeAndSend(Command command);
    public abstract String beforeWriteMessage(Command command);

    public boolean writeMessage(AccessibilityNodeInfo inputEditText, Command qaMessage) {
        if (!qaMessage.isWrite()) {
            if(inputEditText.isEditable()){
                Bundle arg = new Bundle();
                String sendMessage = beforeWriteMessage(qaMessage);
                arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sendMessage);
                boolean result = inputEditText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg);
                if(!result){
//                    Log.e("MyAccessibilityService", "Failed to set text directly.");
                    // 如果直接设置文本失败，可以逐个字符发送输入事件
                    for (char c : sendMessage.toString().toCharArray()) {
                        sendCharacter(c,inputEditText);
                    }
                }
                qaMessage.setWrite(result);
            }else {
                NekoChatService.getInstance().addLogcat("isEditable false");
            }
        }
        return qaMessage.isWrite();
    }

    private void sendCharacter(char c,AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            // 发送字符的逻辑可以通过模拟按键实现
            // 这里我们需要使用 AccessibilityService 模拟输入
            // 但是 Android 的无障碍 API 本身不支持直接模拟按键
            // 所以我们可以通过在输入框中添加字符的方式间接实现

            // 这里可以使用 performAction 添加字符
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, String.valueOf(c));
            boolean success = nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
//            if (!success) {
//                Log.e("MyAccessibilityService", "Failed to send character: " + c);
//            }
        }
    }

    public boolean pasteMessage(AccessibilityNodeInfo inputEditText, Command qaMessage){
        if(inputEditText.isEditable()){
            boolean result= inputEditText.performAction(AccessibilityNodeInfo.ACTION_PASTE);
//            Bundle arg = new Bundle();
//            String sendMessage = beforeWriteMessage(qaMessage);
//            arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sendMessage);
//            boolean result = inputEditText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg);
            qaMessage.setWrite(result);
        }else {
            NekoChatService.getInstance().addLogcat("isEditable false");
        }
        return qaMessage.isWrite();
    }

    public static boolean clickButton(AccessibilityNodeInfo sendButton, Command commandMessage) {
        boolean result = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        commandMessage.setSend(result);
        return commandMessage.isSend();
    }

    public static @Nullable AccessibilityNodeInfo findFirstNodeInfo(AccessibilityNodeInfo source, String viewId) {
        return findIndexNodeInfo(source,viewId,0);
    }

    public static @Nullable AccessibilityNodeInfo findIndexNodeInfo(AccessibilityNodeInfo source, String viewId,int position) {
        List<AccessibilityNodeInfo> targets = source.findAccessibilityNodeInfosByViewId(viewId);
        AccessibilityNodeInfo target = null;
        if (!targets.isEmpty()) {
            if(position<targets.size()){
                target = targets.get(position);
            }
        }
        return target;
    }

    public static boolean hasAllId(AccessibilityNodeInfo nodeInfo,String... ids){
        for (String s : ids)
        {
            if(!s.startsWith(":")){
                s= ":"+s;
            }
            List<AccessibilityNodeInfo> nodeInfoList = nodeInfo
                    .findAccessibilityNodeInfosByViewId(nodeInfo.getPackageName() + s);
            if(nodeInfoList.isEmpty()){
                return false;
            }
        }
        return true;
    }
}

