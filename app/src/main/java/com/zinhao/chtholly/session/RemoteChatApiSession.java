package com.zinhao.chtholly.session;

import com.zinhao.chtholly.entity.NetAiAskAble;
import org.json.JSONException;

import java.util.List;

public interface RemoteChatApiSession {
    boolean requestChatCompletions(NetAiAskAble message);
    void requestChatSummarize();

    boolean callApi(NetAiAskAble message,boolean add) throws JSONException;

    String getAgentPrompt();
    void setAgentPrompt(String desc);

    String getContextChat();
    int clearContext();
    void loadChatHistory();
    int summarize();

    void setModelIndex(int modelIndex);
    RemoteModel getCurrentModel();
    List<RemoteModel> getModelList();

    class RemoteModel {
        private final String str;

        public String getStr() {
            return str;
        }

        public RemoteModel(String str) {
            this.str = str;
        }
    }

}
