package com.zinhao.chtholly.utils;

import android.view.accessibility.AccessibilityEvent;
import com.zinhao.chtholly.entity.Command;
import com.zinhao.chtholly.entity.Message;

public class LockScreenHandler extends BaseChatHandler{
    public LockScreenHandler(MessageCallback messageCallback) {
        super(messageCallback);
    }

    @Override
    protected boolean isAtName(Message message, String name) {
        return false;
    }

    @Override
    public void handle(AccessibilityEvent event) {

    }

    @Override
    public String getPackageName() {
        return null;
    }

    @Override
    public boolean writeAndSend(Command command) {
        return false;
    }

    @Override
    public String beforeWriteMessage(Command command) {
        return null;
    }
}
