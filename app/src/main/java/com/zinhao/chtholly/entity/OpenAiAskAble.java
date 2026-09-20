package com.zinhao.chtholly.entity;

import android.util.Log;

import com.zinhao.chtholly.session.OpenAiSession;

import okhttp3.Call;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

public class OpenAiAskAble extends NetAiAskAble{
    private static final String TAG = "OpenAiAskAble";

    public OpenAiAskAble(String packageName, Message question, DelayReplyCallback delayReplyCallback) {
        super(packageName, question, delayReplyCallback);
    }

    @Override
    protected boolean handleAsk() {
        Log.i("Command","OpenAiAskAble -> handleAsk");
        return super.handleAsk();
    }

    @Override
    protected boolean throwToChild() {
        Log.i("Command","OpenAiAskAble throwToChild");
        try {
            return  OpenAiSession.getInstance().callApi(this,true);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        getAnswer().setMessage(String.format(Locale.CHINA,"\uD83D\uDE44发生错误了:%s %s",e.getMessage(),e.getCause()));
        if(delayReplyCallback !=null)
            delayReplyCallback.onReplySuccess(this);
    }


    //* 模型停止生成令牌的原因。如果模型达到自然停止点或提供的停止序列，则这将stop；
    //* 如果达到请求中指定的最大令牌数，则将length；
    //* 如果由于内容过滤器中的标志而省略内容，则为 content_filter；
    //* 如果模型达到 tool_calls，则为 tool_calls称为工具。
    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

    }

    @Override
    public void doToolCall(Choice nekoReply) {
        super.doToolCall(nekoReply);
        // Tool calls are now handled directly in OpenAiSession.defaultChatCompletions()
    }

    @Override
    public void doToolCallReply(JSONObject content, String callId) {
        super.doToolCallReply(content, callId);
        // Tool call results are now handled directly in OpenAiSession.defaultChatCompletions()
    }

    private Choice parseResponse(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray choices = jsonResponse.getJSONArray("choices");
        JSONObject choice = choices.getJSONObject(0);
        return Choice.fromJson(choice.toString());
    }
}
