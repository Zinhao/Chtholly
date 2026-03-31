package com.zinhao.chtholly.session;

import android.util.Log;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.LoggingInterceptor;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.entity.Message;
import com.zinhao.chtholly.entity.NetAiAskAble;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class GeminiSession extends NekoSession implements ChatSession{
    private static final String TAG = "GeminiSession";

    private static final String ROLE = "role";
    private static final String CONTENT = "content";
    private static final String TOOL_CALL_ID = "tool_call_id";

    private static final String ROLE_SYSTEM = "system_instruction";
    private static final String ROLE_MODEL = "model";
    private static final String ROLE_USER = "user";
    private static final String ROLE_TOOL = "tool";

    public static final String MODEL_GEMINI_3_FL_PRE = "gemini-3.1-flash-lite-preview";

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA);
    private final JSONObject data;
    private JSONArray chats;
    private final OkHttpClient okHttpClient;
    private static GeminiSession instance;
    private final JSONObject systemInstruction;

    private String chatUrl;

    private GeminiSession(String chatUrl) {
        this.chatUrl = chatUrl;
        okHttpClient = new OkHttpClient.Builder()
                .callTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .addInterceptor(new LoggingInterceptor())
                .build();
        data = new JSONObject();
        chats = new JSONArray();
        systemInstruction = new JSONObject();
        try {

            setChara(BotApp.getInstance().getCurrentCharacter().desc);

            data.put(ROLE_SYSTEM, systemInstruction);

            data.put("contents",chats);

            JSONObject t = new JSONObject();
            t.put("thinkingLevel","low");
            JSONObject generationConfigObj = new JSONObject();
            generationConfigObj.put("thinkingConfig",t);
            data.put("generationConfig",generationConfigObj);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void setModel(String model){

    }

    @Override
    public void setChara(String charaDesc){
        JSONArray partsArray = new JSONArray();
        JSONObject st = new JSONObject();
        String agentSys = charaDesc.replace("$name",BotApp.getInstance().getBotName());

        try {
            st.put("text",agentSys);
            partsArray.put(st);

            systemInstruction.put(CONTENT, partsArray);
            Log.d(TAG, "setChara: "+chats.get(0));
        } catch (JSONException e) {
            Log.d(TAG, "setChara: failed.");
        }
    }

    public String getChara() {
        try {
            return systemInstruction.getString(CONTENT);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getContextChat(){
        return chats.toString();
    }

    public int summarize(){
        return -1;
    }

    public void addAssistantChat(String message){

    }

    public void addSystemChat(String message){

    }

    public void addToolCallResult(JSONObject content,String callId){
        JSONObject function_call_result_message = new JSONObject();
        try {
            function_call_result_message.put(ROLE,ROLE_TOOL);
            function_call_result_message.put(CONTENT,content);
            function_call_result_message.put(TOOL_CALL_ID,callId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        chats.put(function_call_result_message);
    }

    private void addTextChat(String role, String text){
        JSONObject newChat = new JSONObject();
        JSONArray parts = new JSONArray();
        try {
            JSONObject textObj = new JSONObject();
            textObj.put("text",text);
            parts.put(textObj);

            newChat.put("role",role);
            newChat.put("parts",parts);
            Log.d(TAG, String.format(Locale.CHINA,"addChat: %s: %s",role,text));
        } catch (JSONException e) {
            Log.e(TAG, String.format(Locale.CHINA,"addChat: %s: %s",role,text));
        }
        chats.put(newChat);
    }

    public static GeminiSession getInstance() {
        if(instance == null){
            instance = new GeminiSession(BotApp.getInstance().getChatUrl());
        }
        return instance;
    }

    public boolean startAsk(NetAiAskAble message) throws JSONException {
        addTextChat(ROLE_USER,message.getQuestion().getMessage());
        data.put("contents",chats);
        return requestChatCompletions(message);
    }

    public void setChatUrl(String chatUrl) {
        this.chatUrl = chatUrl;
    }

    public String getChatUrl() {
        return chatUrl;
    }

    public void requestChatSummarize(){
        NekoChatService.getInstance().addLogcat("requestChatSummarize:length");
        Message question = new Message("SYSTEM","使用不超过50字总结对话",System.currentTimeMillis());
        NetAiAskAble summarizeMessage = new NetAiAskAble(BotApp.getInstance().getPackageName(), question, new NetAiAskAble.DelayReplyCallback() {
            @Override
            public void onReply(NetAiAskAble message) {
                chats = new JSONArray();
                chats.put(systemInstruction);
                NekoChatService.getInstance().addLogcat("requestChatSummarize:"+message.getAnswer().getMessage());
                addTextChat(ROLE_SYSTEM,message.getAnswer().getMessage());
            }
        });
        summarizeMessage.ask();
    }

    public boolean requestChatCompletions(NetAiAskAble message){
        RequestBody requestBody = RequestBody.Companion.create(data.toString(),MediaType.parse("application/json;charset=utf-8"));

        Log.d(TAG, "requestAsk: "+data);
        Request request = new Request.Builder().post(requestBody).url(chatUrl + "/models/"+MODEL_GEMINI_3_FL_PRE+":generateContent")
                .addHeader("Content-Type","application/json")
                .addHeader("x-goog-api-key", BotApp.getInstance().apiKey)
                .addHeader("User-Agent","Android Application <Chtholly>")
                .build();
        okHttpClient.newCall(request).enqueue(message);
        return true;
    }
}
