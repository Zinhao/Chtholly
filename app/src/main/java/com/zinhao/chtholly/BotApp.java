package com.zinhao.chtholly;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.room.Room;

import com.zinhao.chtholly.entity.Message;
import com.zinhao.chtholly.session.NekoSession;
import com.zinhao.chtholly.utils.LocalFileCache;

import java.util.List;

public class BotApp extends Application {
    public String apiKey;
    public static final String CONFIG_API_KEY = "api_kye_config";
    public static final String CONFIG_BOT_NAME = "bot name";
    public static final String CONFIG_ADMIN_NAME = "";
    private String botName;
    private String adminName;
    private static BotApp instance;
    private SharedPreferences sharedPreferences;
    private AppDatabase database;
    private MessageDao messageDao;
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
        database = Room.databaseBuilder(this,AppDatabase.class,"app_data").build();
        messageDao = database.messageDao();
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

    public void setAdminName(String adminName) {
        this.adminName = adminName;
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
}
