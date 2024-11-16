package com.zinhao.chtholly.utils;

import android.view.accessibility.AccessibilityEvent;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.entity.Message;

public class WXChatHandler extends BaseChatHandler {
    public WXChatHandler(MessageCallback messageCallback) {
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
}
