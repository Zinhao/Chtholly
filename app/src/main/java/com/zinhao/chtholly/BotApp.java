package com.zinhao.chtholly;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.room.Room;

import com.zinhao.chtholly.db.AICharacterDao;
import com.zinhao.chtholly.db.AppDatabase;
import com.zinhao.chtholly.db.ChatSessionDao;
import com.zinhao.chtholly.db.MessageDao;
import com.zinhao.chtholly.entity.AICharacter;
import com.zinhao.chtholly.entity.ChatSession;
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
    public static final String CONFIG_ROLEPLAY = "roleplay";

    public static final String CONFIG_WITH_SPEAKER = "with_speaker";

    public static final String CONFIG_FEISHU_APP_ID = "feishu_app_id";
    public static final String CONFIG_FEISHU_APP_SECRET = "feishu_app_secret";
    public static final String CONFIG_FEISHU_RECEIVE_OPENID = "feishu_receive_openid";
    public static final String CONFIG_CURRENT_CHARACTER_ID = "current_character_id";


    private boolean isFirstRun;
    private String apiKey;
    private String botName;
    private String adminName;
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
    private ChatSessionDao chatSessionDao;
    private long currentSessionId;

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
    public NekoSession getApiSession() {
        if(mode == OpenAiSession.class){
            OpenAiSession openAiSession = OpenAiSession.getInstance();
            if(openAiSession!=null){
                openAiSession.setRoleplayMode(isRoleplay());
            }
            return openAiSession;
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
        //大概一两个小时，我的key暴露在开场合,危险！！！
        //A couple of hours ago, my key was exposed in public. Danger!!!
        apiKey = sharedPreferences.getString(CONFIG_API_KEY,"sk-123456789abcdefg!@#$%^&");
        setApiKey(apiKey);
        botName = sharedPreferences.getString(CONFIG_BOT_NAME,"");
        String aiSoul = sharedPreferences.getString(CONFIG_SOUL_DESC,"");

        adminName = sharedPreferences.getString(CONFIG_ADMIN_NAME,"");
        chatUrl = sharedPreferences.getString(CONFIG_CHAT_URL,HostConsts.LOCAL_HOST);
        ttsUrl = sharedPreferences.getString(CONFIG_TTS_URL, HostConsts.LOCAL_HOST);
        withSpeaker = sharedPreferences.getBoolean(CONFIG_WITH_SPEAKER, false);
        isFirstRun = sharedPreferences.getBoolean(CONFIG_IS_FIRST_RUN,true);
        //飞书配置
        feishuAppId = sharedPreferences.getString(CONFIG_FEISHU_APP_ID,"");
        feishuAppSecret = sharedPreferences.getString(CONFIG_FEISHU_APP_SECRET,"");
        feishuReceiveOpenid = sharedPreferences.getString(CONFIG_FEISHU_RECEIVE_OPENID,"");
        replyGateWayAgentDesc = getString(R.string.reply_gateway);
        summarizeChatAgentDesc = getString(R.string.summarize);


        AppDatabase database = Room.databaseBuilder(this, AppDatabase.class, "app_data")
                .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.createMigration3_4(this), AppDatabase.MIGRATION_4_5)
                .build();
        messageDao = database.messageDao();
        aiCharacterDao = database.characterDao();
        chatSessionDao = database.chatSessionDao();

        // Load current character from database
        loadCurrentCharacter(aiSoul);
    }

    public String getReplyGateWayAgentDesc() {
        return replyGateWayAgentDesc;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        //Default
        if(apiKey.isEmpty()){
            mode = NekoSession.class;
        }else{
            mode = OpenAiSession.class;
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
        if (currentCharacter == null) {
            return sharedPreferences.getString(CONFIG_SOUL_DESC, "");
        }
        return currentCharacter.getDesc();
    }

    public void setAiSoul(String aiSoul) {
        if (currentCharacter != null) {
            currentCharacter.setDesc(aiSoul);
        }
    }

    public AICharacter getCurrentCharacter() {
        return currentCharacter;
    }

    public void setCurrentCharacter(@NotNull AICharacter currentCharacter) {
        this.currentCharacter = currentCharacter;
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

    public void setRoleplay(boolean roleplay) {
        if (currentCharacter != null) {
            currentCharacter.setRoleplay(roleplay);
            AsyncHelper.INSTANCE.doAsyncPart(() -> {
                aiCharacterDao.updateRoleplay(currentCharacter.getId(), roleplay);
            });
        }
        if (mode == OpenAiSession.class) {
            NekoSession session = getApiSession();
            if (session instanceof OpenAiSession) {
                ((OpenAiSession) session).setRoleplayMode(roleplay);
            }
            if (session instanceof RemoteChatApiSession) {
                if (currentCharacter != null) {
                    ((RemoteChatApiSession) session).setAgentPrompt(currentCharacter.getDesc());
                }
            }
        }
    }

    public boolean isRoleplay() {
        if (currentCharacter != null) {
            return currentCharacter.isRoleplay();
        }
        return true;
    }

    public long getCurrentSessionId() {
        return currentSessionId;
    }

    public void setCurrentSessionId(long currentSessionId) {
        this.currentSessionId = currentSessionId;
    }

    public ChatSessionDao getChatSessionDao() {
        return chatSessionDao;
    }

    public AICharacterDao getCharacterDao() {
        return aiCharacterDao;
    }

    private void loadCurrentCharacter(String defaultAiSoul) {
        FileLogger.INSTANCE.i(getClass().getSimpleName(),"loadCurrentCharacter");
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                // 确保内置助手角色存在
                ensureBuiltinCharacterExists();

                // Try to load from SP saved id
                long savedCharacterId = sharedPreferences.getLong(CONFIG_CURRENT_CHARACTER_ID, -1);
                AICharacter loaded = null;

                if (savedCharacterId > 0) {
                    loaded = aiCharacterDao.getAICharacterById(savedCharacterId);
                }

                if (loaded != null) {
                    currentCharacter = loaded;
                    FileLogger.INSTANCE.i("BotApp", "Loaded character from DB: " + loaded.getName() + " id=" + loaded.getId());
                } else {
                    // Try to get the first character from database
                    List<AICharacter> all = aiCharacterDao.getAll();
                    if (all != null && !all.isEmpty()) {
                        currentCharacter = all.get(0);
                        FileLogger.INSTANCE.i("BotApp", "Using first character from DB: " + currentCharacter.getName() + " id=" + currentCharacter.getId());
                    } else {
                        // First run, create default character
                        currentCharacter = new AICharacter(botName, defaultAiSoul);
                        long id = aiCharacterDao.insert(currentCharacter);
                        currentCharacter.setId(id);
                        FileLogger.INSTANCE.i("BotApp", "Created default character: " + currentCharacter.getName() + " id=" + id);
                    }
                    // Save to SP
                    sharedPreferences.edit().putLong(CONFIG_CURRENT_CHARACTER_ID, currentCharacter.getId()).apply();
                }

                // Now restore session after character is loaded
                restoreCurrentSession();
            }
        });
    }

    private void ensureBuiltinCharacterExists() {
        AICharacter existing = aiCharacterDao.getBuiltinCharacter();
        if (existing == null) {
            String prompt = readAssetFile("default_assistant_prompt.txt");
            if (prompt != null && !prompt.isEmpty()) {
                AICharacter builtin = new AICharacter("助手", prompt);
                builtin.setBuiltin(true);
                builtin.setRoleplay(false);
                long id = aiCharacterDao.insert(builtin);
                builtin.setId(id);
                FileLogger.INSTANCE.i("BotApp", "Created builtin assistant character: id=" + id);
            }
        }
    }

    private String readAssetFile(String fileName) {
        try {
            java.io.InputStream is = getAssets().open(fileName);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            is.close();
            return sb.toString().trim();
        } catch (java.io.IOException e) {
            FileLogger.INSTANCE.e("BotApp", "Failed to read asset: " + fileName, e);
            return null;
        }
    }

    private void restoreCurrentSession() {
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                ChatSession session = chatSessionDao.getLatestByCharacterId(currentCharacter.getId());
                if (session != null) {
                    currentSessionId = session.getId();
                    FileLogger.INSTANCE.i("BotApp", "Restored session: " + currentSessionId + " for character: " + currentCharacter.getName());
                } else {
                    // Create a new session for the default character
                    ChatSession newSession = new ChatSession(currentCharacter.getId(), currentCharacter.getName(), System.currentTimeMillis());
                    long id = chatSessionDao.insert(newSession);
                    currentSessionId = id;
                    FileLogger.INSTANCE.i("BotApp", "Created new session: " + currentSessionId + " for character: " + currentCharacter.getName());
                }
                NekoSession nekoSession = getApiSession();
                if(nekoSession instanceof OpenAiSession || nekoSession instanceof GeminiSession){
                    ((RemoteChatApiSession) nekoSession).setAgentPrompt(currentCharacter.desc);
                    ((RemoteChatApiSession) nekoSession).loadChatHistory();
                }
            }
        });
    }

    public void getOrCreateSessionForCharacter(long characterId, String characterName, SessionReadyCallback callback) {
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                ChatSession session = chatSessionDao.getLatestByCharacterId(characterId);
                if (session == null) {
                    session = new ChatSession(characterId, characterName, System.currentTimeMillis());
                    long id = chatSessionDao.insert(session);
                    session.setId(id);
                }
                currentSessionId = session.getId();
                callback.onSessionReady(session.getId());
            }
        });
    }

    public interface SessionReadyCallback {
        void onSessionReady(long sessionId);
    }

    public void insert(Message message){
        message.setSessionId(currentSessionId);
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                messageDao.insert(message);
                chatSessionDao.updateLastMessageTime(currentSessionId, message.getTimeStamp());
            }
        });
    }

    public MessageDao getMessageDao() {
        return messageDao;
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
        getLastTenMessagesBySession(currentSessionId, listener);
    }

    public void getLastTenMessagesBySession(long sessionId, MessageDao.MessageGetAllListener listener){
        FileLogger.INSTANCE.i(getClass().getSimpleName(),"getLastTenMessagesBySession:"+sessionId);
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                List<Message> result = messageDao.getLastTenMessagesBySessionId(sessionId);
                listener.onSuccess(result);
            }
        });
    }

    public void loadMessageBySession(long sessionId, MessageDao.MessageGetAllListener listener){
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                List<Message> result = messageDao.getBySessionId(sessionId);
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
        // Save to SharedPreferences
        SharedPreferences.Editor editor = BotApp.getInstance().getSharedPreferences().edit();
        editor.putString(BotApp.CONFIG_SOUL_DESC, character.getDesc());
        editor.putString(BotApp.CONFIG_BOT_NAME, character.getName());
        editor.putLong(BotApp.CONFIG_CURRENT_CHARACTER_ID, character.getId());
        editor.apply();
        // Switch to the session for this character
        getOrCreateSessionForCharacter(character.getId(), character.getName(), new SessionReadyCallback() {
            @Override
            public void onSessionReady(long sessionId) {
                FileLogger.INSTANCE.i("BotApp", "Switched to session: " + sessionId + " for character: " + character.getName());
                NekoSession nekoSession = getApiSession();
                if (nekoSession instanceof OpenAiSession) {
                    ((OpenAiSession) nekoSession).setRoleplayMode(character.isRoleplay());
                }
                if(nekoSession instanceof RemoteChatApiSession){
                    ((RemoteChatApiSession) nekoSession).setAgentPrompt(character.getDesc());
                    ((RemoteChatApiSession) nekoSession).loadChatHistory();
                }
            }
        });
    }
}
