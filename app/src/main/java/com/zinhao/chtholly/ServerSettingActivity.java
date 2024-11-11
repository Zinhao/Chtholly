package com.zinhao.chtholly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.zinhao.chtholly.databinding.ActivityVoiceServerSettingBinding;
import com.zinhao.chtholly.session.OpenAiSession;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ServerSettingActivity extends AppCompatActivity {
    private static final String TAG = "ServerSettingActivity";
    ActivityVoiceServerSettingBinding binding;
    List<String> speakerList = new ArrayList<>();
    List<ServerInfo> serverList = new ArrayList<>();
    private Handler mHandler;
    ServerInfo localNetWork;
    boolean isVoiceServerSpinnerInit = true;
    boolean isSpeakerSpinnerInit = true;
    private final AsyncHttpClient.JSONArrayCallback modelInfo = new AsyncHttpClient.JSONArrayCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONArray jsonArray) {
            speakerList.clear();
            if(e!=null){
                e.printStackTrace();
                return;
            }

            if(jsonArray.length()!=0){

                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    JSONArray speakers  = jsonObject.getJSONArray("speakers");
                    for (int j = 0; j < speakers.length(); j++) {
                        speakerList.add(speakers.getString(j));
                    }
                    Log.d("VoiceServerSettingActivity", "onCompleted: "+speakers.getClass().getName());
                } catch (JSONException ex) {
                    throw new RuntimeException(ex);
                }
                runOnUiThread(()->{
                    binding.speakerSpinner.setAdapter(new ArrayAdapter<String>(ServerSettingActivity.this,
                            android.R.layout.simple_list_item_1, speakerList));
                    binding.speakerSpinner.setPrompt("choose model");
                    int selectIndex = Math.max(Math.min(BotApp.getInstance().getSpeakerId(),speakerList.size()-1),0);
                    binding.speakerSpinner.setSelection(selectIndex);
                });
            }


        }
    };
    private EditText voiceServerEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoiceServerSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        VoiceHttpApi.getModelInfo(modelInfo);
        mHandler = new Handler(getMainLooper());
        localNetWork = new ServerInfo("本地代理",BotApp.getInstance().getChatUrl());
        binding.scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent starter = new Intent(v.getContext(), ScanNetActivity.class);
                starter.putExtra("start_port",80);
                starter.putExtra("end_port",434);
                startActivityForResult(starter,38);
            }
        });
        binding.speakerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(isSpeakerSpinnerInit){
                    isSpeakerSpinnerInit = false;
                    return;
                }
                BotApp.getInstance().setSpeakerId(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

//        https://chat.closechat.org/api/assistants/v2/chat
        /**
         * {
         *     "text": "推荐点纯音乐",
         *     "sender": "User",
         *     "isCreatedByUser": true,
         *     "parentMessageId": "e82d5ac6-0467-498c-948f-89753f61114f",
         *     "conversationId": "97731ff5-73a4-46d9-aa31-cc1a3a8f44ac",
         *     "messageId": "",
         *     "error": false,
         *     "model": "gpt-4o-mini",
         *     "tools": [],
         *     "systemPrompt": "你是丛雨，你通常以本座自称，你说话有着浓厚的古人腔调。性格有小孩子的一面，也有大人的一面。不过基本上都是小孩子的一面居多，平时是一个很有元气开朗的女孩子。其实很胆小，非常怕幽灵鬼怪。没有身体，无法进食和洗澡，但是能感受灵力的存在，所以对供奉给神的酒、有灵力的温泉有舒服的感受。\n神刀“丛雨丸”的管理者。\n丛雨作为献祭品成为“丛雨丸”的管理者，守护着“穗织”这片土地，认拔出刀的有地将臣为主人，别人对你得称呼是丛雨酱、小雨。",
         *     "temperature": 0.8,
         *     "maxChatHistories": 10,
         *     "maxIterations": 10,
         *     "artifactsPrompt": false,
         *     "endpoint": "openai",
         *     "key": null,
         *     "isContinued": false
         * }
         */
        serverList.add(new ServerInfo("close ai proxy 1","https://api.openai-proxy.org/v1/chat/completions"));
        serverList.add(new ServerInfo("close ai proxy 2","https://api.closeai-proxy.xyz/v1/chat/completions"));
        serverList.add(new ServerInfo("close ai proxy 3","https://api.openai-proxy.live/v1/chat/completions"));
        serverList.add(new ServerInfo("open ai","https://api.openai.com/v1/chat/completions"));
        serverList.add(new ServerInfo("通意千问","https://dashscope.aliyuncs.com/compatible-mode/v1"));
        serverList.add(localNetWork);

        int selectIndex = serverList.size()-1;
        for (int i = 0; i < serverList.size(); i++) {
            ServerInfo serverInfo = serverList.get(i);
            if(serverInfo.url.equals(OpenAiSession.getInstance().getChatUrl())){
                selectIndex = i;
                break;
            }
        }
        binding.aiServerSpinner.setAdapter(new ArrayAdapter<>(ServerSettingActivity.this,
                android.R.layout.simple_list_item_1, serverList));
        binding.aiServerSpinner.setPrompt("choose server");
        binding.aiServerSpinner.setSelection(selectIndex);
        binding.aiServerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(isVoiceServerSpinnerInit){
                    isVoiceServerSpinnerInit = false;
                    return;
                }
                if(serverList.get(position).name.equals(localNetWork.name)){
                    Intent starter = new Intent(view.getContext(), ScanNetActivity.class);
                    starter.putExtra("start_port",8175);
                    starter.putExtra("end_port",8185);
                    startActivityForResult(starter,39);
                }else {
                    binding.textView9.setText(serverList.get(position).url);
                    BotApp.getInstance().setChatUrl(serverList.get(position).url);
                    OpenAiSession.getInstance().setChatUrl(serverList.get(position).url);
                    saveChatUrl(serverList.get(position).url);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        binding.textView9.setText(OpenAiSession.getInstance().getChatUrl());

        voiceServerEdit = binding.voiceServer.getEditText();
        if(voiceServerEdit !=null){
            voiceServerEdit.setText(BotApp.getInstance().getTtsUrl());
            voiceServerEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    mHandler.removeCallbacks(updateVoiceHost);
                    mHandler.postDelayed(updateVoiceHost,500);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 38 && resultCode == RESULT_OK){
            if(data!=null){
                String host = data.getStringExtra("host");
                binding.voiceServer.getEditText().setText(host);
                updateVoiceHost.run();
            }
        }

        if(requestCode == 39 && resultCode == RESULT_OK){
            if(data!=null){
                String host = data.getStringExtra("host");
                localNetWork.url = host;
                binding.textView9.setText(host);
                BotApp.getInstance().setChatUrl(host);
                OpenAiSession.getInstance().setChatUrl(host);
                saveChatUrl(host);
            }
        }
    }

    private void saveChatUrl(String chatUrl){
        Log.d(TAG, "saveChatUrl: "+chatUrl);
        SharedPreferences.Editor editor = BotApp.getInstance().getSharedPreferences().edit();
        editor.putString(BotApp.CONFIG_CHAT_URL,chatUrl);
        editor.apply();
    }

    private void saveTtsUrl(String ttsUrl){
        Log.d(TAG, "saveTtsUrl: "+ttsUrl);
        SharedPreferences.Editor editor = BotApp.getInstance().getSharedPreferences().edit();
        editor.putString(BotApp.CONFIG_TTS_URL,ttsUrl);
        editor.apply();
    }

    private Runnable updateVoiceHost = new Runnable() {
        @Override
        public void run() {
            if(isDestroyed()){
                return;
            }
            String host = voiceServerEdit.getText().toString().trim();
            if(host.isEmpty()){
                return;
            }
            if(!host.startsWith("http://") && !host.startsWith("https://")){
                return;
            }
            BotApp.getInstance().setTtsUrl(host);
            saveTtsUrl(host);
            Toast.makeText(ServerSettingActivity.this,"set voice host success!",Toast.LENGTH_SHORT).show();

        }
    };

    static class ServerInfo{
        String name;
        String url;

        public ServerInfo(String name, String url) {
            this.name = name;
            this.url = url;
        }

        @NonNull
        @NotNull
        @Override
        public String toString() {
            return name;
        }
    }
}