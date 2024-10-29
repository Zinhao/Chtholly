package com.zinhao.chtholly;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import com.zinhao.chtholly.databinding.ActivityMainBinding;
import com.zinhao.chtholly.entity.Message;
import com.zinhao.chtholly.session.OpenAiSession;

import java.util.ArrayList;
import java.util.List;

import per.goweii.layer.core.anim.AnimStyle;
import per.goweii.layer.core.widget.SwipeLayout;
import per.goweii.layer.dialog.DialogLayer;


public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private List<Message> messages;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        EditText apiKeyEditText = binding.textInputLayout.getEditText();
        if(apiKeyEditText!=null) {
            apiKeyEditText.setText(BotApp.getInstance().apiKey);
            apiKeyEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    BotApp.getInstance().setApiKey(s.toString());
                }
            });
        }

        EditText botNameEditText = binding.textInputLayout2.getEditText();
        if(botNameEditText!=null) {
            botNameEditText.setText(BotApp.getInstance().getBotName());
            botNameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    BotApp.getInstance().setBotName(s.toString());
                }
            });
        }

        EditText adminNameEditText = binding.textInputLayout3.getEditText();
        if(adminNameEditText!=null){
            adminNameEditText.setText(BotApp.getInstance().getAdminName());
            adminNameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    BotApp.getInstance().setAdminName(s.toString());
                }
            });
        }

        binding.toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        initRoleDialog();
        binding.button2.setEnabled(false);
        BotApp.getInstance().select(new MessageDao.MessageGetAllListener() {
            @Override
            public void onSuccess(List<Message> result) {
                messages = result;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initMessageDialog();
                    }
                });
            }
        });
    }

    private void initMessageDialog(){
        View dialogContent = getLayoutInflater().inflate(R.layout.bottom_dialog,null,false);
        ListView listView = dialogContent.findViewById(R.id.list);
        listView.setAdapter(new MessageAdapter(this,android.R.layout.simple_list_item_2,messages));
        binding.button2.setEnabled(true);
        binding.button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DialogLayer(MainActivity.this)
                        .setContentView(dialogContent)
                        .setGravity(Gravity.BOTTOM)
                        .setSwipeDismiss(SwipeLayout.Direction.BOTTOM)
                        .setBackgroundDimDefault()
                        .setContentAnimator(AnimStyle.BOTTOM)
                        .show();
            }
        });
    }
    ArrayList<Role> roles = new ArrayList<>();
    private void initRoleDialog(){
        roles.clear();
        roles.add(new Role("健忘的猫娘-3.5",getString(R.string.neko_chara_1)));
        roles.add(new Role("允许H的猫娘-3.5",getString(R.string.neko_chara_2)));
        roles.add(new Role("正常的猫娘-3.5",getString(R.string.neko_chara_3)));

        roles.add(new Role("健忘的猫娘-4",getString(R.string.neko_chara_1)));
        roles.add(new Role("允许H的猫娘-4",getString(R.string.neko_chara_2)));
        roles.add(new Role("正常的猫娘-4",getString(R.string.neko_chara_3)));
        View dialogContent = getLayoutInflater().inflate(R.layout.bottom_dialog,null,false);
        ListView listView = dialogContent.findViewById(R.id.list);
        listView.setAdapter(new RoleAdapter(this,android.R.layout.simple_list_item_2,roles));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Role role = roles.get(position);
                binding.textView.setText(role.desc);
                OpenAiSession.getInstance().setChara(role.desc);
                if(position<3){
                    OpenAiSession.getInstance().setModel(OpenAiSession.MODEL_GPT_3_5_TURBO);
                }else {
                    OpenAiSession.getInstance().setModel(OpenAiSession.MODEL_GPT_4_TURBO);
                }
            }
        });

        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DialogLayer(MainActivity.this)
                        .setContentView(dialogContent)
                        .setGravity(Gravity.BOTTOM)
                        .setSwipeDismiss(SwipeLayout.Direction.BOTTOM)
                        .setBackgroundDimDefault()
                        .setContentAnimator(AnimStyle.BOTTOM)
                        .show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences.Editor editor = BotApp.getInstance().getSharedPreferences().edit();
        editor.putString(BotApp.CONFIG_BOT_NAME,BotApp.getInstance().getBotName());
        editor.putString(BotApp.CONFIG_ADMIN_NAME,BotApp.getInstance().getAdminName());
        editor.putString(BotApp.CONFIG_API_KEY,BotApp.getInstance().apiKey);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.toggleButton.setChecked(isAccessibilitySettingsOn(this));
    }

    private boolean isAccessibilitySettingsOn(Context mContext){
        int accessibilityEnabled = 0;
        String service = mContext.getPackageName() + "/" + NekoChatService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            mStringColonSplitter.setString(settingValue);
            while (mStringColonSplitter.hasNext()) {
                String accessibilityService = mStringColonSplitter.next();
                if (accessibilityService.equalsIgnoreCase(service)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class Role{
        private final String name;
        private final String desc;

        public Role(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }
    }

    public static class RoleAdapter extends ArrayAdapter<Role>{

        public RoleAdapter(@NonNull Context context, int resource, @NonNull List<Role> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if(convertView == null)
                convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);

            if(convertView!=null){
                TextView tvTitle = convertView.findViewById(android.R.id.text1);
                tvTitle.setText(getItem(position).name);

                TextView tvResult = convertView.findViewById(android.R.id.text2);
                tvResult.setSingleLine();
                tvResult.setText(getItem(position).desc);
            }
            assert convertView != null;
            return convertView;
        }
    }

    public static class MessageAdapter extends ArrayAdapter<Message>{

        public MessageAdapter(@NonNull Context context, int resource, @NonNull List<Message> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if(convertView == null)
                convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);

            if(convertView!=null){
                TextView tvTitle = convertView.findViewById(android.R.id.text1);
                tvTitle.setText(getItem(position).getSpeaker());

                TextView tvResult = convertView.findViewById(android.R.id.text2);
                tvResult.setText(getItem(position).getMessage());
            }
            assert convertView != null;
            return convertView;
        }
    }
}