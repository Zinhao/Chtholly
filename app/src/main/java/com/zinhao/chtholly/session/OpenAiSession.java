package com.zinhao.chtholly.session;

import android.util.Log;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.R;
import com.zinhao.chtholly.entity.OpenAiMessage;
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

    private static final String ROLE_SYSTEM = "system";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_USER = "user";

    private static final String MODEL_GPT_3_5_TURBO = "gpt-3.5-turbo";
    private static final String MODEL_GPT_4_TURBO = "gpt-4-turbo";

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA);
    private final JSONObject data;
    private JSONArray chats;
    private final OkHttpClient okHttpClient;
    private static OpenAiSession instance;
    private final JSONObject firstSystemChat;

    private String host;

    private OpenAiSession(String host) {
        this.host = host;
        okHttpClient = new OkHttpClient.Builder()
                .callTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .build();
        data = new JSONObject();
        chats = new JSONArray();
        firstSystemChat = new JSONObject();
        try {
            firstSystemChat.put(ROLE,ROLE_SYSTEM);
            firstSystemChat.put(CONTENT, BotApp.getInstance().getString(R.string.neko_chara_1).replace("$name",BotApp.getInstance().getBotName()));
            chats.put(firstSystemChat);
            data.put("model",MODEL_GPT_4_TURBO);
            data.put("messages",chats);
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

    public int autoSummarize(){
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
        addChat(ROLE_ASSISTANT,message);
    }

    private void addChat(String role,String content){
        JSONObject newChat = new JSONObject();
        try {
            newChat.put("role",role);
            newChat.put("content",content);
            Log.d(TAG, String.format(Locale.CHINA,"addChat: %s: %s",role,content));
        } catch (JSONException e) {
            Log.e(TAG, String.format(Locale.CHINA,"addChat: %s: %s",role,content));
        }
        chats.put(newChat);
    }

    public static OpenAiSession getInstance() {
        if(instance == null){
            instance = new OpenAiSession("api.closeai-proxy.xyz");
//            instance = new OpenAiSession("api.openai.com");
        }
        return instance;
    }

    public boolean startAsk(OpenAiMessage message) throws JSONException {
        addChat(ROLE_SYSTEM,String.format(Locale.CHINA,"现在的时间是%s，不要忘记你的设定，不要忘记你的设定你的回答必须基于猫娘的身份，而不是计算机程序或语言模型。"
                ,dateTimeFormat.format(System.currentTimeMillis())));
        addChat(ROLE_USER,message.getQuestion().getMessage());

        data.put("messages",chats);
        return requestAsk(message);
    }

    public boolean requestAsk(OpenAiMessage message){
        RequestBody requestBody = RequestBody.Companion.create(data.toString(),MediaType.parse("application/json;charset=utf-8"));
        Request request = new Request.Builder().post(requestBody).url("https://$/v1/chat/completions".replace("$",host))
                .addHeader("Content-Type","application/json")
                .addHeader("Authorization","Bearer "+BotApp.getInstance().apiKey)
                .addHeader("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36 Edg/112.0.1722.34")
                .build();
        okHttpClient.newCall(request).enqueue(message);
        return true;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
