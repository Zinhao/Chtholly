package com.zinhao.chtholly.session;

import com.zinhao.chtholly.entity.AskAble;
import com.zinhao.chtholly.entity.NetAiAskAble;
import org.json.JSONException;

public interface ChatSession {
    boolean requestChatCompletions(NetAiAskAble message);
    void requestChatSummarize();
    boolean startAsk(NetAiAskAble message) throws JSONException;

    void setChatUrl(String chatUrl);
    String getChatUrl();
    String getChara();
    void setChara(String desc);

    String getContextChat();
    int summarize();

}
