package com.zinhao.chtholly;

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

public class VoiceServerSettingActivity extends AppCompatActivity {
    ActivityVoiceServerSettingBinding binding;
    List<String> speakerList = new ArrayList<>();
    List<ServerInfo> serverList = new ArrayList<>();
    private Handler mHandler;
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
                    binding.speakerSpinner.setAdapter(new ArrayAdapter<String>(VoiceServerSettingActivity.this,
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
        binding.speakerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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
        serverList.add(new ServerInfo("本地代理","http://192.168.31.253:8180"));
        binding.aiServerSpinner.setAdapter(new ArrayAdapter<>(VoiceServerSettingActivity.this,
                android.R.layout.simple_list_item_1, serverList));
        binding.aiServerSpinner.setPrompt("choose server");
        binding.aiServerSpinner.setSelection(0);
        binding.aiServerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                OpenAiSession.getInstance().setChatUrl(serverList.get(position).url);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        voiceServerEdit = binding.voiceServer.getEditText();
        if(voiceServerEdit !=null){
            voiceServerEdit.setText(BotApp.getInstance().getVoiceServerHost());
            voiceServerEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    mHandler.removeCallbacks(updateVoiceHost);
                    mHandler.postDelayed(updateVoiceHost,3000);

                }
            });
        }
    }

    private Runnable updateVoiceHost = new Runnable() {
        @Override
        public void run() {
            if(isDestroyed()){
                return;
            }
            String host = voiceServerEdit.toString().trim();
            if(host.isEmpty()){
                return;
            }
            if(!host.startsWith("http://") || !host.startsWith("https://")){
                return;
            }
            BotApp.getInstance().setVoiceServerHost(host);
            Toast.makeText(VoiceServerSettingActivity.this,"set voice host success!",Toast.LENGTH_SHORT).show();
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