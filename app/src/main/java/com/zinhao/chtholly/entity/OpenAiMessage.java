package com.zinhao.chtholly.entity;

import android.util.Log;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAiMessage extends NekoMessage implements Callback{
    private static final String TAG = "OpenAiMessage";
    private DelayReplyListener delayReplyListener;

    private final Pattern remind = Pattern.compile("\\[remind \\d{1,12} .*?]");

    public OpenAiMessage(String packageName, Message question, DelayReplyListener delayReplyListener) {
        super(packageName, question);
        this.delayReplyListener = delayReplyListener;
    }

    public OpenAiMessage(String packageName, Message question) {
        super(packageName, question);
    }

    public DelayReplyListener getDelayReplyListener() {
        return delayReplyListener;
    }

    public void setDelayReplyListener(DelayReplyListener delayReplyListener) {
        this.delayReplyListener = delayReplyListener;
    }

    @Override
    public boolean ask() {
        if(super.ask()){
            return true;
        }
        return beforeAskCheck();
    }

    private boolean beforeAskCheck(){
        if(BotApp.getInstance().apiKey.isEmpty()){
            return NekoSession.getInstance().startAsk(this);
        }else{
            if(NekoChatService.mode != OpenAiSession.class){
                return NekoSession.getInstance().startAsk(this);
            }
            try {
                return OpenAiSession.getInstance().startAsk(this);
            } catch (JSONException e) {
                getAnswer().setMessage("JSONException:" + e.getMessage());
                return true;
            }
        }
    }

    @Override
    public boolean throwQuestion() {
        return beforeAskCheck();
    }

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        getAnswer().setMessage(String.format(Locale.CHINA,"\uD83D\uDE44发生错误了:%s %s",e.getMessage(),e.getCause()));
        if(delayReplyListener!=null)
            delayReplyListener.onReply(this);
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
        if(response.code() == 200){
            ResponseBody body = response.body();
            if(body!=null){
                try {
                    if(getAnswer() != null){
                        try {
                            Choice nekoReply = parseResponse(body.string());
                            String content = nekoReply.getMessage().getContent();
                            if(content != null && !content.trim().equals("null")){
                                doTextReply(content);
                                doTTSReply(content);
                            }
                            doToolCall(nekoReply);
                        }catch (IllegalStateException e){
                            Log.e(TAG, "onResponse: ", e);
                        }
                    }
                } catch (JSONException e) {
                    getAnswer().setMessage(e.getMessage());
                }
            }
        }
//        else if(response.code() == 400){
//            int deleteCount = OpenAiSession.getInstance().autoSummarize();
//            if(deleteCount>=2){
//                OpenAiSession.getInstance().requestAsk(this);
//            }else{
//                getAnswer().setMessage("我有点累了，需要休息!("+response.code()+")");
//            }
//        }else {
//            int deleteCount = OpenAiSession.getInstance().autoSummarize();
//            if(deleteCount>=2){
//                OpenAiSession.getInstance().requestAsk(this);
//            }else{
//                getAnswer().setMessage("我有点累了，需要休息!("+response.code()+")");
//            }
//        }
        if(delayReplyListener!=null)
            delayReplyListener.onReply(this);
        response.close();
    }

    public void doTextReply(String content){
        getAnswer().setMessage(content);
        BotApp.getInstance().insert(getAnswer());
        OpenAiSession.getInstance().addAssistantChat(content);
    }

    public void doTTSReply(String text){
        NekoChatService.getInstance().speakMessage(text);
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
        if(nekoReply.getFinishReason().equals("length")){
            OpenAiSession.getInstance().requestChatSummarize();
        }
        nekoReply.getMessage().getToolCalls().forEach(new Consumer<Choice.ToolCall>() {
            @Override
            public void accept(Choice.ToolCall toolCall) {
                String methodName = toolCall.getFunction().getName();
                try {
                    AIMethodTool aiMethodTool = AIMethodTool.TOTAL_TOOL.get(methodName);
                    assert aiMethodTool!=null;
                    CallAble callAble = aiMethodTool.getCallAble();
                    if(callAble!=null){
                        Map<String, Object> argsMap = toolCall.getArgsMap();
                        argsMap.put(OpenAiMessage.this.getClass().getName(),OpenAiMessage.this);
                        callAble.call(argsMap);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private Choice parseResponse(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray choices = jsonResponse.getJSONArray("choices");
        JSONObject choice = choices.getJSONObject(0);
        return Choice.fromJson(choice.toString());
    }

    public interface DelayReplyListener{
        void onReply(OpenAiMessage message);
    }
}
