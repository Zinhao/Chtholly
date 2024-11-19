package com.zinhao.chtholly.utils;

import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.Nullable;
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

    public boolean writeMessage(AccessibilityNodeInfo inputEditText, Command qaMessage) {
        if (!qaMessage.isWrite()) {
            Bundle arg = new Bundle();
            String atMessage = String.format("@%s %s", qaMessage.getQuestion().getSpeaker(), qaMessage.getAnswer().getMessage());
            arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, atMessage);
            boolean result = inputEditText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg);
            qaMessage.setWrite(result);
        }
        return qaMessage.isWrite();
    }

    public static boolean clickButton(AccessibilityNodeInfo sendButton, Command commandMessage) {
        if (!commandMessage.isSend() && commandMessage.isWrite()) {
            boolean result = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            commandMessage.setSend(result);
        }
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
}

