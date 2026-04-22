package com.zinhao.chtholly;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.room.Room;

import com.zinhao.chtholly.db.AICharacterDao;
import com.zinhao.chtholly.db.AppDatabase;
import com.zinhao.chtholly.db.MessageDao;
import com.zinhao.chtholly.entity.AICharacter;
import com.zinhao.chtholly.entity.Message;
import com.zinhao.chtholly.session.NekoSession;
import com.zinhao.chtholly.session.RemoteChatApiSession;
import com.zinhao.chtholly.session.GeminiSession;
import com.zinhao.chtholly.session.OpenAiSession;
import com.zinhao.chtholly.utils.AsyncHelper;
import com.zinhao.chtholly.utils.FileLogger;
import com.zinhao.chtholly.utils.HostConsts;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BotApp extends Application {
    public static final String CONFIG_ADMIN_NAME = "admin_name";

    public static final String CONFIG_CHAT_URL = "chat_url";
    public static final String CONFIG_API_KEY = "api_kye_config";
    public static final String CONFIG_BOT_NAME = "bot_name";

    public static final String CONFIG_SOUL_DESC = "soul_description";

    public static final String CONFIG_TTS_URL = "tts_url";
    public static final String CONFIG_IS_FIRST_RUN = "is_first_run";

    public static final String CONFIG_WITH_SPEAKER = "with_speaker";

    public static final String CONFIG_FEISHU_APP_ID = "feishu_app_id";
    public static final String CONFIG_FEISHU_APP_SECRET = "feishu_app_secret";
    public static final String CONFIG_FEISHU_RECEIVE_OPENID = "feishu_receive_openid";


    private boolean isFirstRun;
    private String apiKey;
    private String botName;
    private String adminName;
    private String aiSoul;
    // 说话人前缀，用于群聊区分说话人
    private boolean withSpeaker = true;

    private String chatUrl;
    private AICharacter currentCharacter;
    private String ttsUrl;

    private String feishuAppId;
    private String feishuAppSecret;
    private String feishuReceiveOpenid;

    private static BotApp instance;
    private SharedPreferences sharedPreferences;
    private MessageDao messageDao;
    private AICharacterDao aiCharacterDao;

    private String replyGateWayAgentDesc;
    private String summarizeChatAgentDesc;

    private int speakerId = 0;

    public void setTtsUrl(String ttsUrl) {
        this.ttsUrl = ttsUrl;
    }

    public String getTtsUrl() {
        return ttsUrl;
    }

    public void setSpeakerId(int speakerId) {
        this.speakerId = speakerId;
    }

    public int getSpeakerId() {
        return speakerId;
    }
    public static BotApp getInstance() {
        return instance;
    }
    private Class<?> mode = NekoSession.class;
    public NekoSession getSession() {
        if(mode == OpenAiSession.class){
            return OpenAiSession.getInstance();
        }else if(mode == GeminiSession.class){
            return  GeminiSession.getInstance();
        }
        return NekoSession.getInstance();
    }

    public static Context context() {
        return instance.getApplicationContext();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        FileLogger.INSTANCE.init(getInstance());
        sharedPreferences = getSharedPreferences("app_data", MODE_PRIVATE);
        apiKey = sharedPreferences.getString(CONFIG_API_KEY,"");
        botName = sharedPreferences.getString(CONFIG_BOT_NAME,"");
        aiSoul = sharedPreferences.getString(CONFIG_SOUL_DESC,"");
        adminName = sharedPreferences.getString(CONFIG_ADMIN_NAME,"");
        chatUrl = sharedPreferences.getString(CONFIG_CHAT_URL,HostConsts.GEMINI_PROXY_API_HOST);
        ttsUrl = sharedPreferences.getString(CONFIG_TTS_URL, HostConsts.LOCAL_HOST);
        withSpeaker = sharedPreferences.getBoolean(CONFIG_WITH_SPEAKER, true);
        isFirstRun = sharedPreferences.getBoolean(CONFIG_IS_FIRST_RUN,true);
        //飞书配置
        feishuAppId = sharedPreferences.getString(CONFIG_FEISHU_APP_ID,"");
        feishuAppSecret = sharedPreferences.getString(CONFIG_FEISHU_APP_SECRET,"");
        feishuReceiveOpenid = sharedPreferences.getString(CONFIG_FEISHU_RECEIVE_OPENID,"");
        replyGateWayAgentDesc = getString(R.string.reply_gateway);
        summarizeChatAgentDesc = getString(R.string.summarize);
        if(apiKey.isEmpty()){
            mode = NekoSession.class;
        }else{
            mode = GeminiSession.class;
        }
        currentCharacter = new AICharacter(botName,aiSoul);
        AppDatabase database = Room.databaseBuilder(this, AppDatabase.class, "app_data")
                .build();
        messageDao = database.messageDao();
        aiCharacterDao = database.characterDao();
    }

    public String getReplyGateWayAgentDesc() {
        return replyGateWayAgentDesc;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        if(apiKey.isEmpty()){
            mode = NekoSession.class;
        }else{
            mode = GeminiSession.class;
        }
    }

    public String getSummarizeChatAgentDesc() {
        return summarizeChatAgentDesc;
    }

    public void setWithSpeaker(boolean withSpeaker) {
        this.withSpeaker = withSpeaker;
        sharedPreferences.edit().putBoolean(CONFIG_WITH_SPEAKER, withSpeaker).apply();
    }

    public boolean isWithSpeaker() {
        return withSpeaker;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getBotName() {
        return botName;
    }

    public Class<?> getMode() {
        return mode;
    }

    public void setMode(Class<?> mode) {
        this.mode = mode;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getAiSoul() {
        return aiSoul;
    }

    public void setAiSoul(String aiSoul) {
        this.aiSoul = aiSoul;
    }

    public AICharacter getCurrentCharacter() {
        return currentCharacter;
    }

    public void setCurrentCharacter(@NotNull AICharacter currentCharacter) {
        this.currentCharacter = currentCharacter;
        this.aiSoul = currentCharacter.desc;
    }

    public String getFeishuAppId() {
        return feishuAppId;
    }

    public String getFeishuAppSecret() {
        return feishuAppSecret;
    }

    public void setFeishuAppId(String feishuAppId) {
        this.feishuAppId = feishuAppId;
    }

    public void setFeishuAppSecret(String feishuAppSecret) {
        this.feishuAppSecret = feishuAppSecret;
    }

    public String getFeishuReceiveOpenid() {
        return feishuReceiveOpenid;
    }

    public void setFeishuReceiveOpenid(String feishuReceiveOpenid) {
        this.feishuReceiveOpenid = feishuReceiveOpenid;
        sharedPreferences.edit().putString(CONFIG_FEISHU_RECEIVE_OPENID, feishuReceiveOpenid).apply();
    }

    public boolean isFirstRun() {
        return isFirstRun;
    }

    public void setFirstRun(boolean firstRun) {
        isFirstRun = firstRun;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getChatUrl() {
        return chatUrl;
    }

    public void setChatUrl(String chatUrl) {
        this.chatUrl = chatUrl;
    }

    public void insert(Message message){
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                messageDao.insert(message);
            }
        });
    }

    public void loadMessage(MessageDao.MessageGetAllListener listener){
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                List<Message> result = messageDao.getAll();
                listener.onSuccess(result);
            }
        });
    }

    public void getLastTenMessages(MessageDao.MessageGetAllListener listener){
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                List<Message> result = messageDao.getLastTenMessages();
                listener.onSuccess(result);
            }
        });
    }

    public void insert(AICharacter character){
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                long id = aiCharacterDao.insert(character);
                character.setId(id);
            }
        });
    }

    public void delete(AICharacter aiCharacter){
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                aiCharacterDao.delete(aiCharacter);
            }
        });
    }
    public void insert(AICharacter character,Runnable callback){
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                long id = aiCharacterDao.insert(character);
                character.setId(id);
                callback.run();
            }
        });
    }

    public void loadAICharacter(AICharacterDao.AICharacterGetAllListener listener){
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                List<AICharacter> result = aiCharacterDao.getAll();
                listener.onSuccess(result);
            }
        });
    }

    public void switchAISoul(AICharacter character){
        setCurrentCharacter(character);
        NekoSession nekoSession = getSession();
        if(nekoSession instanceof RemoteChatApiSession){
            ((RemoteChatApiSession) nekoSession).setAgentPrompt(character.getDesc());
        }
        SharedPreferences.Editor editor = BotApp.getInstance().getSharedPreferences().edit();
        editor.putString(BotApp.CONFIG_SOUL_DESC,character.getDesc());
        editor.apply();
    }
}
