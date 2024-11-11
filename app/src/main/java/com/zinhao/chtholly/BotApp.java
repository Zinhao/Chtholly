package com.zinhao.chtholly;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.room.Room;
import com.zinhao.chtholly.entity.AICharacter;
import com.zinhao.chtholly.entity.Message;
import com.zinhao.chtholly.utils.LocalFileCache;

import java.util.List;

public class BotApp extends Application {
    public String apiKey;
    public static final String CONFIG_API_KEY = "api_kye_config";
    public static final String CONFIG_BOT_NAME = "bot_name";
    public static final String CONFIG_CURRENT_CHARACTER_ID = "current_character_id";
    public static final String CONFIG_ADMIN_NAME = "admin_name";
    public static final String CONFIG_CHAT_URL = "chat_url";
    public static final String CONFIG_TTS_URL = "tts_url";
    private String botName;
    private String adminName;
    private long characterId;
    private String chatUrl;
    private AICharacter currentCharacter;
    private String ttsUrl;

    private static BotApp instance;
    private SharedPreferences sharedPreferences;
    private AppDatabase database;
    private MessageDao messageDao;
    private AICharacterDao aiCharacterDao;

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

    public static Context context() {
        return instance.getApplicationContext();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        sharedPreferences = getSharedPreferences("app_data", MODE_PRIVATE);
        apiKey = sharedPreferences.getString(CONFIG_API_KEY,"");
        botName = sharedPreferences.getString(CONFIG_BOT_NAME,"bot name");
        adminName = sharedPreferences.getString(CONFIG_ADMIN_NAME,"");
        characterId = sharedPreferences.getLong(CONFIG_CURRENT_CHARACTER_ID,0);
        chatUrl = sharedPreferences.getString(CONFIG_CHAT_URL,"https://api.openai.com/v1/chat/completions");
        ttsUrl = sharedPreferences.getString(CONFIG_TTS_URL,"http://localhost");

        database = Room.databaseBuilder(this,AppDatabase.class,"app_data")
                .build();
        messageDao = database.messageDao();
        aiCharacterDao = database.characterDao();
        LocalFileCache.getInstance().doSomething(()->{
            currentCharacter = aiCharacterDao.getAICharacterById(characterId);
            if(currentCharacter == null){
                currentCharacter = new AICharacter("人工智能",getApplicationContext().getString(R.string.chara_default));
            }
        });


    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getBotName() {
        return botName;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setCharacterId(long characterId) {
        this.characterId = characterId;
    }

    public long getCharacterId() {
        return characterId;
    }

    public AICharacter getCurrentCharacter() {
        return currentCharacter;
    }

    public void setCurrentCharacter(AICharacter currentCharacter) {
        this.currentCharacter = currentCharacter;
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
        LocalFileCache.getInstance().doSomething(new Runnable() {
            @Override
            public void run() {
                messageDao.insert(message);
            }
        });
    }

    public void select(MessageDao.MessageGetAllListener listener){
        LocalFileCache.getInstance().doSomething(new Runnable() {
            @Override
            public void run() {
                List<Message> result = messageDao.getAll();
                listener.onSuccess(result);
            }
        });
    }

    public void insert(AICharacter character){
        LocalFileCache.getInstance().doSomething(new Runnable() {
            @Override
            public void run() {
                long id = aiCharacterDao.insert(character);
                character.setId(id);
            }
        });
    }

    public void select(AICharacterDao.AICharacterGetAllListener listener){
        LocalFileCache.getInstance().doSomething(new Runnable() {
            @Override
            public void run() {
                List<AICharacter> result = aiCharacterDao.getAll();
                listener.onSuccess(result);
            }
        });
    }
}
