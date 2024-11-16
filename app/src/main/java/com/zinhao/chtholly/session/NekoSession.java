package com.zinhao.chtholly.session;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.entity.Command;
import com.zinhao.chtholly.entity.NekoAskAble;
import com.zinhao.chtholly.utils.LocalFileCache;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.zinhao.chtholly.entity.NekoAskAble.*;

public class NekoSession {
    private JSONObject keywordAnswerMap;
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

    private static String[] LOVE_MOE = new String[]{"୧⍢⃝୨",
            "٩(๛ ˘ ³˘)۶❤",
            "✧(≖ ◡ ≖✿)",
            "(・ω< )★",
            "Σ(ﾟдﾟ;)" ,
            "Σ( ￣□￣||)<" ,
            "(´；ω；`)" ,
            "（/TДT)/" ,
            "(^・ω・^)" ,
            "(｡･ω･｡)" ,
            "(●￣(ｴ)￣●)" ,
            "ε=ε=(ノ≧∇≦)ノ" ,
            "(´･_･`)" ,
            "(-_-#)" ,
            "（￣へ￣）" ,
            "(￣ε(#￣) Σ" ,
            "(╯°口°)╯(┴—┴" ,
            "ヽ(`Д´)ﾉ" ,
            "(\"▔□▔)/" ,
            "(º﹃º )" ,
            "(๑>\u0602<๑）" ,
            "｡ﾟ(ﾟ´Д｀)ﾟ｡" ,
            "(∂ω∂)" ,
            "(┯_┯)" ,
            "( ๑ˊ•̥▵•)੭₎₎" ,
            "¥ㄟ(´･ᴗ･`)ノ¥" ,
            "Σ_(꒪ཀ꒪」∠)_" ,
            "(๑‾᷅^‾᷅๑)"};

    public String randomFaceEmo(){
        int r = (int) (Math.random() * (LOVE_MOE.length-1));
        r = Math.min(LOVE_MOE.length-1,r);
        return LOVE_MOE[r];
    }

    private void study(String key,String word) throws JSONException {
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

    private void write(){
        LocalFileCache.getInstance().saveJSONObject(BotApp.getInstance().getApplicationContext(), keywordAnswerMap,"keyword_map.json");
    }

    private String show(){
        return keywordAnswerMap.toString();
    }
    
    public boolean startAsk(NekoAskAble nekoAskAble){
        if(nekoAskAble.getQuestion().message.startsWith(LOOK_NODE)){
            nekoAskAble.getAnswer().setMessage(instance.show());
            return true;
        }
        if(nekoAskAble.getQuestion().message.startsWith(STUDY)){
            if(nekoAskAble.getQuestion().message.indexOf(STUDY) != nekoAskAble.getQuestion().message.lastIndexOf(STUDY)){
                nekoAskAble.getAnswer().setMessage(HARD + randomFaceEmo());
                return true;
            }
            if(nekoAskAble.getQuestion().message.indexOf(SAY) != nekoAskAble.getQuestion().message.lastIndexOf(SAY)){
                nekoAskAble.getAnswer().setMessage(HARD + randomFaceEmo());
                return true;
            }
            if(nekoAskAble.getQuestion().message.contains(SAY)){
                //记录到数据库
                String replaceStr = nekoAskAble.getQuestion().message.replace(STUDY, File.separator).replace(SAY, File.separator);
                if(replaceStr.contains(File.separator)){
                    String[] sp = replaceStr.split(File.separator);
                    if(sp.length == 3){
                        try {
                           instance.study(sp[1].trim(),sp[2].trim());
                            nekoAskAble.getAnswer().setMessage(OK + randomFaceEmo());
                            return true;
                        } catch (JSONException e) {

                        }
                    }
                }
                nekoAskAble.getAnswer().setMessage(HARD+ randomFaceEmo());
            }else{
                nekoAskAble.getAnswer().setMessage(THINK+ randomFaceEmo());
            }
            return true;
        }

        if(nekoAskAble.getQuestion().message.contains("忘记")){
            //写入json
//            BotApp.getInstance().getNekoSession().write();\ --D./
            nekoAskAble.getAnswer().setMessage(NOT_FORGET + randomFaceEmo());
            return true;
        }

        // json
        try {
            String an = instance.find(nekoAskAble.getQuestion().message);
            if(an!=null){
                nekoAskAble.getAnswer().setMessage(an);
                return true;
            }
        } catch (JSONException e) {

        }

        if(nekoAskAble.getQuestion().message.contains("早上好") || nekoAskAble.getQuestion().message.contains("早安")){
            nekoAskAble.getAnswer().setMessage(ASK_NORMAL_TEMP.replace("$","早")+ randomFaceEmo());
        }else if(nekoAskAble.getQuestion().message.contains("中午好") || nekoAskAble.getQuestion().message.contains("午安")){
            nekoAskAble.getAnswer().setMessage(ASK_NORMAL_TEMP.replace("$","午")+ randomFaceEmo());
        }else if(nekoAskAble.getQuestion().message.contains("晚安")){
            nekoAskAble.getAnswer().setMessage(ASK_NORMAL_TEMP.replace("$","晚") + randomFaceEmo());
        }else if(nekoAskAble.getQuestion().message.contains("晚上好")){
            nekoAskAble.getAnswer().setMessage(nekoAskAble.getQuestion().message + randomFaceEmo());
        }else {
            miaomiaojiao(nekoAskAble);
        }
        return true;
    }

    public void miaomiaojiao(Command qaMessage){
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
        builder.append(randomFaceEmo());
        qaMessage.getAnswer().setMessage(builder.toString());
        qaMessage.setWrite(false);
        qaMessage.setSend(false);
    }
}
