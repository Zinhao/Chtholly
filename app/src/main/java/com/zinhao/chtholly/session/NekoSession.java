package com.zinhao.chtholly.session;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.entity.Command;
import com.zinhao.chtholly.entity.NekoMessage;
import com.zinhao.chtholly.utils.LocalFileCache;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.zinhao.chtholly.entity.NekoMessage.*;

public class NekoSession {
    private JSONObject keywordAnswerMap;
    public static final int MODE_ID = 481;
    private static NekoSession instance;
    public static NekoSession getInstance() {
        if(instance == null){
            instance = new NekoSession();
        }
        return instance;
    }
    public NekoSession() {
        keywordAnswerMap = new JSONObject();
        LocalFileCache.getInstance().readJSONObject(BotApp.context(), "keyword_map.json", new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
                if(e==null || jsonObject == null){
                    return;
                }
                keywordAnswerMap = jsonObject;
            }
        });
    }

    public void study(String key,String word) throws JSONException {
        JSONArray replies;
        if(keywordAnswerMap.has(key)){
            replies = keywordAnswerMap.getJSONArray(key);
        }else{
            replies = new JSONArray();
        }
        JSONObject ask = new JSONObject();
        ask.put("message",word);
        replies.put(ask);
        keywordAnswerMap.put(key,replies);
        write();
    }

    public String find(String key) throws JSONException {
        JSONArray replies;
        if(keywordAnswerMap.has(key)){
            replies = keywordAnswerMap.getJSONArray(key);
            int random = (int) (Math.random()*replies.length());
            return replies.getJSONObject(random).getString("message");
        }
        return null;
    }

    public void write(){
        LocalFileCache.getInstance().saveJSONObject(BotApp.getInstance().getApplicationContext(), keywordAnswerMap,"keyword_map.json");
    }

    public String show(){
        return keywordAnswerMap.toString();
    }
    
    public boolean ask(NekoMessage nekoMessage){
        if(nekoMessage.getQuestion().message.startsWith(LOOK_NODE)){
            nekoMessage.getAnswer().setMessage(BotApp.getInstance().getNekoSession().show());
            return true;
        }
        if(nekoMessage.getQuestion().message.startsWith(STUDY)){
            if(nekoMessage.getQuestion().message.indexOf(STUDY) != nekoMessage.getQuestion().message.lastIndexOf(STUDY)){
                nekoMessage.getAnswer().setMessage(HARD);
                return true;
            }
            if(nekoMessage.getQuestion().message.indexOf(SAY) != nekoMessage.getQuestion().message.lastIndexOf(SAY)){
                nekoMessage.getAnswer().setMessage(HARD);
                return true;
            }
            if(nekoMessage.getQuestion().message.contains(SAY)){
                //记录到数据库
                String replaceStr = nekoMessage.getQuestion().message.replace(STUDY, File.separator).replace(SAY, File.separator);
                if(replaceStr.contains(File.separator)){
                    String[] sp = replaceStr.split(File.separator);
                    if(sp.length == 3){
                        try {
                            BotApp.getInstance().getNekoSession().study(sp[1].trim(),sp[2].trim());
                            nekoMessage.getAnswer().setMessage(OK);
                            return true;
                        } catch (JSONException e) {

                        }
                    }
                }
                nekoMessage.getAnswer().setMessage(HARD);
            }else{
                nekoMessage.getAnswer().setMessage(THINK);
            }
            return true;
        }

        if(nekoMessage.getQuestion().message.contains("忘记")){
            //写入json
//            BotApp.getInstance().getNekoSession().write();\ --D./
            nekoMessage.getAnswer().setMessage(NOT_FORGET);
            return true;
        }

        // json
        try {
            String an = BotApp.getInstance().getNekoSession().find(nekoMessage.getQuestion().message);
            if(an!=null){
                nekoMessage.getAnswer().setMessage(an);
                return true;
            }
        } catch (JSONException e) {

        }

        if(nekoMessage.getQuestion().message.contains("早上好") || nekoMessage.getQuestion().message.contains("早安")){
            nekoMessage.getAnswer().setMessage(ASK_NORMAL_TEMP.replace("$","早"));
        }else if(nekoMessage.getQuestion().message.contains("中午好") || nekoMessage.getQuestion().message.contains("午安")){
            nekoMessage.getAnswer().setMessage(ASK_NORMAL_TEMP.replace("$","午"));
        }else if(nekoMessage.getQuestion().message.contains("晚安")){
            nekoMessage.getAnswer().setMessage(ASK_NORMAL_TEMP.replace("$","晚"));
        }else if(nekoMessage.getQuestion().message.contains("晚上好")){
            nekoMessage.getAnswer().setMessage(nekoMessage.getQuestion().message);
        }else{
            miaomiaojiao(nekoMessage);
        }
        return true;
    }

    private static void miaomiaojiao(Command qaMessage){
        StringBuilder builder = new StringBuilder();
        int random1 = (int) (Math.random()*2+1);
        int random2 = (int) (Math.random()*3);
        int random3 = (int) (Math.random()*3);
        for (int i = 0; i < random1; i++) {
            builder.append("喵");
        }
        builder.append("~");
        for (int i = 0; i < random2; i++) {
            builder.append("喵");
        }
        builder.append("~");
        for (int i = 0; i < random3; i++) {
            builder.append("喵");
        }
        builder.append("~~");
        qaMessage.getAnswer().setMessage(builder.toString());
        qaMessage.setWrite(false);
        qaMessage.setSend(false);
    }
}
