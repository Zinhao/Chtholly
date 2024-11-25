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

public class OpenAiAskAble extends NekoAskAble implements Callback{
    private static final String TAG = "OpenAiMessage";
    private DelayReplyCallback delayReplyCallback;

    private final Pattern remind = Pattern.compile("\\[remind \\d{1,12} .*?]");

    public OpenAiAskAble(String packageName, Message question, DelayReplyCallback delayReplyCallback) {
        super(packageName, question);
        this.delayReplyCallback = delayReplyCallback;
    }

    public OpenAiAskAble(String packageName, Message question) {
        super(packageName, question);
    }

    public DelayReplyCallback getDelayReplyCallback() {
        return delayReplyCallback;
    }

    public void setDelayReplyCallback(DelayReplyCallback delayReplyCallback) {
        this.delayReplyCallback = delayReplyCallback;
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
        if(delayReplyCallback !=null)
            delayReplyCallback.onReply(this);
    }


    //* 模型停止生成令牌的原因。如果模型达到自然停止点或提供的停止序列，则这将stop；
    //* 如果达到请求中指定的最大令牌数，则将length；
    //* 如果由于内容过滤器中的标志而省略内容，则为 content_filter；
    //* 如果模型达到 tool_calls，则为 tool_calls称为工具。
    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
        if(response.code() == 200){
            ResponseBody body = response.body();
            if(body!=null){
                try {
                    if(getAnswer() != null){
                        try {
                            Choice nekoReply = parseResponse(body.string());
                            if(nekoReply.getFinishReason().equals("length")){
                                OpenAiSession.getInstance().requestChatSummarize();
                            }else if(nekoReply.getFinishReason().equals("tool_calls")){
                                doToolCall(nekoReply);
                            }else if(nekoReply.getFinishReason().equals("stop")){
                                String content = nekoReply.getMessage().getContent();
                                if(content != null && !content.trim().equals("null")){
                                    doTextReply(content);
                                    doTTSReply(content);
                                    OpenAiSession.getInstance().addAssistantChat(content);
                                }
                            }
                        }catch (IllegalStateException e){
                            NekoChatService.getInstance().addLogcat("onResponse: "+e.getMessage());
                            Log.e(TAG, "onResponse: ", e);
                        }
                    }
                } catch (JSONException e) {
                    getAnswer().setMessage(e.getMessage());
                }
            }
        }
        if(delayReplyCallback !=null)
            delayReplyCallback.onReply(this);
        response.close();
    }

    public void doTextReply(String content){
        getAnswer().setMessage(content);
        BotApp.getInstance().insert(getAnswer());
    }

    public void doTTSReply(String text){
        NekoChatService.getInstance().playTTSVoiceFromNetWork(text);
    }

    /***
     *
     */
    public void doToolCallReply(JSONObject content,String callId){
        //function_call_result_message = {
        //    "role": "tool",
        //    "content": json.dumps({
        //        "order_id": order_id,
        //        "delivery_date": delivery_date.strftime('%Y-%m-%d %H:%M:%S')
        //    }),
        //    "tool_call_id": response['choices'][0]['message']['tool_calls'][0]['id']
        //}
        OpenAiSession.getInstance().addToolCallResult(content,callId);
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
        OpenAiSession.getInstance().addToolCalls(nekoReply.getMessage());
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
                        argsMap.put(OpenAiAskAble.this.getClass().getName(), OpenAiAskAble.this);
                        callAble.call(argsMap,toolCall.getId());
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

    public interface DelayReplyCallback {
        void onReply(OpenAiAskAble message);
    }
}
