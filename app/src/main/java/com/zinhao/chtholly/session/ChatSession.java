package com.zinhao.chtholly.session;

import com.zinhao.chtholly.entity.AskAble;

public interface ChatSession {
    boolean requestChatCompletions(AskAble message);
    void requestChatSummarize();
    boolean startAsk(AskAble message);

    void setChatUrl(String chatUrl);
    String getChatUrl();
}
