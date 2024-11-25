package com.zinhao.chtholly.session;

import android.util.Log;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.LoggingInterceptor;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.entity.AIMethodTool;
import com.zinhao.chtholly.entity.Choice;
import com.zinhao.chtholly.entity.Message;
import com.zinhao.chtholly.entity.OpenAiAskAble;
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

public class OpenAiSession extends NekoSession{
    private static final String TAG = "OpenAiSession";

    private static final String ROLE = "role";
    private static final String CONTENT = "content";
    private static final String TOOL_CALL_ID = "tool_call_id";

    private static final String ROLE_SYSTEM = "system";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_USER = "user";
    private static final String ROLE_TOOL = "tool";

    public static final String MODEL_GPT_3_5_TURBO = "gpt-3.5-turbo";
    public static final String MODEL_GPT_4_TURBO = "gpt-4-turbo";
    public static final String MODEL_GPT_4O_MINI = "gpt-4o-mini";
    public static final String MODEL_GPT_4O = "gpt-4o";

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA);
    private final JSONObject data;
    private JSONArray chats;
    private final OkHttpClient okHttpClient;
    private static OpenAiSession instance;
    private final JSONObject firstSystemChat;

    private String chatUrl;


    private static final Tool TOOL_1 = new Tool("function",AIMethodTool.REMIND_TOOL);
    private static final Tool TOOL_2 = new Tool("function",AIMethodTool.HELP_TOOL);
    static class Tool{
        String type;
        AIMethodTool function;
        // 构造函数
        public Tool(String type, AIMethodTool function) {
            this.type = type;
            this.function = function;
        }

        public String getType() {
            return type;
        }

        public AIMethodTool getFunction() {
            return function;
        }

        // 将 Tool 对象转换为 JSON 对象
        public JSONObject toJson() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", this.type);
            if (this.function != null) {
                jsonObject.put("function", this.function.toJsonObject()); // 使用 AIMethodTool 的 toJsonObject 方法
            }
            return jsonObject;
        }
    }

    private OpenAiSession(String chatUrl) {
        this.chatUrl = chatUrl;
        okHttpClient = new OkHttpClient.Builder()
                .callTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
//                .sslSocketFactory()
                .addInterceptor(new LoggingInterceptor())
                .build();
        data = new JSONObject();
        chats = new JSONArray();
        firstSystemChat = new JSONObject();
        try {
            firstSystemChat.put(ROLE,ROLE_SYSTEM);
            firstSystemChat.put(CONTENT,
                    BotApp.getInstance().getCurrentCharacter().desc.replace("$name",BotApp.getInstance().getBotName()));
            chats.put(firstSystemChat);

            JSONArray tools = new JSONArray();
            tools.put(TOOL_1.toJson());
            tools.put(TOOL_2.toJson());

            data.put("model",MODEL_GPT_4O_MINI);
            data.put("temperature",1);
            data.put("max_completion_tokens", 16384);
            data.put("top_p",1);
            data.put("messages",chats);
            data.put("tools",tools);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void setModel(String model){
        try {
            data.put("model",model);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void setChara(String charaDesc){
        try {
            firstSystemChat.put(CONTENT, charaDesc.replace("$name",BotApp.getInstance().getBotName()));
            Log.d(TAG, "setChara: "+chats.get(0));
        } catch (JSONException e) {
            Log.d(TAG, "setChara: failed.");
        }
    }

    public String getChara() {
        try {
            return firstSystemChat.getString(CONTENT);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getContextChat(){
        return chats.toString();
    }

    public int summarize(){
        int len = 0;
        for (int i = 0; i < chats.length(); i++) {
            try {
                JSONObject chat = chats.getJSONObject(i);
                if(chat.getString(ROLE).equals(ROLE_USER)){
                    len++;
                }
            } catch (JSONException e) {

            }
        }
        chats = new JSONArray();
        chats.put(firstSystemChat);
        return len;
    }

    public int deleteChatContext(){
        int deleteCount = chats.length()/2;
        for (int i = 0; i < deleteCount; i++) {
            chats.remove(1);
        }
        data.remove("messages");
        try {
            data.put("messages",chats);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Log.d(TAG, "autoSummarize: " + deleteCount);
        return deleteCount;
    }

    public void addAssistantChat(String message){
        OpenAiSession.getInstance().addTextChat(ROLE_ASSISTANT,message);
    }

    public void addSystemChat(String message){
        OpenAiSession.getInstance().addTextChat(ROLE_SYSTEM,message);
    }

    public void addToolCalls(Choice.Message message){
        chats.put(message.getRawJsonString());
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
        try {
            newChat.put("role",role);
            newChat.put("content",text);
            Log.d(TAG, String.format(Locale.CHINA,"addChat: %s: %s",role,text));
        } catch (JSONException e) {
            Log.e(TAG, String.format(Locale.CHINA,"addChat: %s: %s",role,text));
        }
        chats.put(newChat);
    }

    public static OpenAiSession getInstance() {
        if(instance == null){
            instance = new OpenAiSession(BotApp.getInstance().getChatUrl());
        }
        return instance;
    }

    public boolean startAsk(OpenAiAskAble message) throws JSONException {
        addTextChat(ROLE_USER,message.getQuestion().getMessage());
        data.put("messages",chats);
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
        Message question = new Message("system","使用不超过50字总结对话",System.currentTimeMillis());
        OpenAiAskAble summarizeMessage = new OpenAiAskAble(BotApp.getInstance().getPackageName(), question, new OpenAiAskAble.DelayReplyCallback() {
            @Override
            public void onReply(OpenAiAskAble message) {
                chats = new JSONArray();
                chats.put(firstSystemChat);
                NekoChatService.getInstance().addLogcat("requestChatSummarize:"+message.getAnswer().getMessage());
                addTextChat(ROLE_SYSTEM,message.getAnswer().getMessage());
            }
        });
        summarizeMessage.ask();
    }

    public boolean requestChatCompletions(OpenAiAskAble message){
        RequestBody requestBody = RequestBody.Companion.create(data.toString(),MediaType.parse("application/json;charset=utf-8"));
        Log.d(TAG, "requestAsk: "+data);
        Request request = new Request.Builder().post(requestBody).url(chatUrl)
                .addHeader("Content-Type","application/json")
                .addHeader("Authorization","Bearer " + BotApp.getInstance().apiKey)
                .addHeader("User-Agent","Android Application <Chttolly>")
                .build();
        okHttpClient.newCall(request).enqueue(message);
        return true;
    }
}
