package com.zinhao.chtholly.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.zinhao.chtholly.db.AICharacterDao;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.view.adapter.CharacterAdapter;
import com.zinhao.chtholly.databinding.ActivityCharacterBinding;
import com.zinhao.chtholly.entity.AICharacter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.List;

public class CharacterActivity extends AppCompatActivity implements CharacterAdapter.ItemClickListener {
    ActivityCharacterBinding binding;
    List<AICharacter> listData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCharacterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("Character Choose");
        binding.floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
            }
        });
        binding.floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addIntent = new Intent(v.getContext(), AddCharacterActivity.class);
                startActivityForResult(addIntent,2);
            }
        });
        BotApp.getInstance().loadAICharacter(new AICharacterDao.AICharacterGetAllListener() {
            @Override
            public void onSuccess(List<AICharacter> result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listData = result;
                        CharacterAdapter adapter = new CharacterAdapter(listData);
                        adapter.setItemClickListener(CharacterActivity.this);
                        binding.recyclerView.setAdapter(adapter);
                        binding.recyclerView.addItemDecoration(new DividerItemDecoration(CharacterActivity.this,DividerItemDecoration.VERTICAL));
                        binding.recyclerView.setLayoutManager(new LinearLayoutManager(CharacterActivity.this));

                    }
                });
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == RESULT_OK){
            if (data != null) {
                String jsonArrayStr = readTextFormIntent(data);
                if(jsonArrayStr.isEmpty()){
                    throw new RuntimeException("text is empty");
                }
                try {
                    JSONArray array = new JSONArray(jsonArrayStr);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject jsonObject = array.getJSONObject(i);
                        String desc = jsonObject.getString("desc");
                        String name = jsonObject.getString("name");
                        AICharacter character = new AICharacter(name,desc);
                        BotApp.getInstance().insert(character);
                        listData.add(character);
                    }
                    binding.recyclerView.getAdapter().notifyDataSetChanged();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            }
        }
        if(requestCode == 2 && resultCode == RESULT_OK){
            if(data!=null) {
                String name = data.getStringExtra("name");
                String desc = data.getStringExtra("desc");
                AICharacter character = new AICharacter(name, desc);
                BotApp.getInstance().insert(character);
                listData.add(character);
                binding.recyclerView.getAdapter().notifyItemInserted(listData.size());
            }
        }
    }

    private String readTextFormIntent(Intent data) {
        Uri uri = data.getData();
        if(uri!=null){
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line = null;
                while ((line = reader.readLine())!=null){
                    stringBuilder.append(line);
                }
                inputStream.close();
                return stringBuilder.toString();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        return "[]";
    }

    @Override
    public void onItemClick(AICharacter character) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(character.getName());
        builder.setMessage(character.getDesc());
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BotApp.getInstance().switchAISoul(character);
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
}