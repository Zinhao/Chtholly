package com.zinhao.chtholly.entity;

import android.util.Log;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.CallAble;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.session.NekoSession;
import com.zinhao.chtholly.session.OpenAiSession;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class NetAiAskAble extends NekoAskAble implements Callback{
    private static final String TAG = "NetAiAskAble";
    protected DelayReplyCallback delayReplyCallback;

    private final Pattern remind = Pattern.compile("\\[remind \\d{1,12} .*?]");

    public NetAiAskAble(String packageName, Message question, DelayReplyCallback delayReplyCallback) {
        super(packageName, question);
        this.delayReplyCallback = delayReplyCallback;
    }

    public NetAiAskAble(String packageName, Message question) {
        super(packageName, question);
    }

    public DelayReplyCallback getDelayReplyCallback() {
        return delayReplyCallback;
    }

    public void setDelayReplyCallback(DelayReplyCallback delayReplyCallback) {
        this.delayReplyCallback = delayReplyCallback;
    }

    @Override
    protected boolean handleAsk() {
        Log.i("Command","NetAiAskAble handleAsk");
        return super.handleAsk();
    }


    public void doTextReply(String content){
        getAnswer().setMessage(content);
        getAnswer().setSpeaker(BotApp.getInstance().getBotName());
        BotApp.getInstance().insert(getAnswer());
    }

    public void doTTSReply(String text){
        if(NekoChatService.getInstance()!=null){
            NekoChatService.getInstance().playTTSVoiceFromNetWork(text);
        }
    }

    /***
     *
     */
    public void doToolCallReply(JSONObject content,String callId){
    }

    /**
     * getFinishReason
     * 模型停止生成令牌的原因。如果模型达到自然停止点或提供的停止序列，则这将stop；
     * 如果达到请求中指定的最大令牌数，则将length；
     * 如果由于内容过滤器中的标志而省略内容，则为 content_filter；
     * 如果模型达到 tool_calls，则为 tool_calls称为工具。
     * @param nekoReply
     */
    public void doToolCall(Choice nekoReply){

    }

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {

    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

    }

    public interface DelayReplyCallback {
        void onReply(NetAiAskAble message);
    }
}
