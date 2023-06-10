package com.zinhao.chtholly.entity;

import android.util.Log;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.NekoChatService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAiMessage extends Command implements Callback{
    private static final String TAG = "OpenAiMessage";
    private DelayReplyListener delayReplyListener;

    private static String[] textHappy = new String[]{"(⌯︎¤̴̶̷̀ω¤̴̶̷́)✧","❛˓◞˂̵✧","( ˉ͈̀꒳ˉ͈́ )✧"};
    private static String[] textNoWords = new String[]{" ୧⍢⃝୨","←_←","┐(´-｀)┌","(*￣rǒ￣)"};
    private static String[] textSad = new String[]{"˃ ˄ ˂̥̥ "};

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
        try {
            return OpenAiSession.getInstance().ask(this);
        } catch (JSONException e) {
            getAnswer().setMessage("JSONException:" + e.getMessage());
        }
        return true;
    }

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        String speaker = getQuestion().getSpeaker();
        getAnswer().setMessage(String.format(Locale.CHINA,"刚刚%s走神了！能重复一遍吗，喵？",BotApp.getInstance().getBotName()));
        OpenAiSession.getInstance().addAssistantChat(getAnswer().message);
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
                        String nekoReply = parseResponse(body.string());
                        Matcher m = remind.matcher(nekoReply);
                        if(m.find()){
                            String remindCommand = m.group(0);
                            if(remindCommand!=null && !remindCommand.isEmpty()){
                                parseRemindMessage(remindCommand);
                                nekoReply = nekoReply.replace(remindCommand,textNoWords[0]);
                            }
                        }
                        getAnswer().setMessage(nekoReply);
                        BotApp.getInstance().insert(getAnswer());
                        OpenAiSession.getInstance().addAssistantChat(getAnswer().message);
                    }
                } catch (JSONException e) {
                    getAnswer().setMessage(e.getMessage());
                }
            }
        }else if(response.code() == 400){
            int deleteCount = OpenAiSession.getInstance().autoSummarize();
            if(deleteCount>=2){
                OpenAiSession.getInstance().requestAsk(this);
            }else{
                getAnswer().setMessage("我有点累了，需要休息!("+response.code()+")");
            }
        }else {
            int deleteCount = OpenAiSession.getInstance().autoSummarize();
            if(deleteCount>=2){
                OpenAiSession.getInstance().requestAsk(this);
            }else{
                getAnswer().setMessage("我有点累了，需要休息!("+response.code()+")");
            }
        }
        if(delayReplyListener!=null)
            delayReplyListener.onReply(this);
        response.close();
    }

    private void parseRemindMessage(String remindCommand){
        Log.d(TAG, "parseRemindMessage: "+remindCommand);
        remindCommand = remindCommand.replace('[',' ').trim();
        remindCommand = remindCommand.replace(']',' ').trim();
        String[] sp = remindCommand.split(" ");
        if(sp.length == 3){
            long secondL = Long.parseLong(sp[1]);
            NekoChatService.getInstance().addRemind(secondL,sp[2],getQuestion().getSpeaker());
        }
    }

    private String parseResponse(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray choices = jsonResponse.getJSONArray("choices");
        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice.getJSONObject("message");
        String content = message.getString("content");
        return content.trim();
    }

    public interface DelayReplyListener{
        void onReply(OpenAiMessage message);
    }
}
