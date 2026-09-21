package com.zinhao.chtholly.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.zinhao.chtholly.R;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.view.adapter.CharacterAdapter;
import com.zinhao.chtholly.databinding.ActivityCharacterBinding;
import com.zinhao.chtholly.entity.AICharacter;
import com.zinhao.chtholly.viewmodel.CharacterViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CharacterActivity extends AppCompatActivity implements CharacterAdapter.ItemClickListener {
    private ActivityCharacterBinding binding;
    private List<AICharacter> listData;
    private CharacterAdapter adapter;
    private CharacterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCharacterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("Character Choose");

        viewModel = new ViewModelProvider(this).get(CharacterViewModel.class);

        binding.floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
            }
        });
        binding.floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addIntent = new Intent(v.getContext(), AddCharacterActivity.class);
                startActivityForResult(addIntent, 2);
            }
        });

        listData = new ArrayList<>();
        adapter = new CharacterAdapter(listData);
        adapter.setItemClickListener(this);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        viewModel.getCharacterList().observe(this, characters -> {
            if (characters != null) {
                listData.clear();
                listData.addAll(characters);
                adapter.notifyDataSetChanged();
            }
        });

        viewModel.loadCharacters();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null) {
                String rawText = readTextFormIntent(data);
                if (rawText.isEmpty()) {
                    throw new RuntimeException("text is empty");
                }
                try {
                    JSONArray array = new JSONArray(rawText);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject jsonObject = array.getJSONObject(i);
                        String desc = jsonObject.getString("desc");
                        String name = jsonObject.getString("name");
                        AICharacter character = new AICharacter(name, desc);
                        viewModel.addCharacter(character);
                    }
                } catch (JSONException e) {
                    AICharacter character = new AICharacter("未命名", rawText);
                    viewModel.addCharacter(character);
                }
            }
        }
        if (requestCode == 2 && resultCode == RESULT_OK) {
            if (data != null) {
                String name = data.getStringExtra("name");
                String desc = data.getStringExtra("desc");
                long editId = data.getLongExtra("edit_id", -1);
                if (editId != -1) {
                    AICharacter character = new AICharacter(name, desc);
                    character.setId(editId);
                    viewModel.updateCharacter(character);
                } else {
                    AICharacter character = new AICharacter(name, desc);
                    viewModel.addCharacter(character);
                }
            }
        }
    }

    private String readTextFormIntent(Intent data) {
        Uri uri = data.getData();
        if (uri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\r\n");
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
        if (!character.isBuiltin()) {
            builder.setNeutralButton("edit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent editIntent = new Intent(CharacterActivity.this, AddCharacterActivity.class);
                    editIntent.putExtra("edit_id", character.getId());
                    editIntent.putExtra("edit_name", character.getName());
                    editIntent.putExtra("edit_desc", character.getDesc());
                    startActivityForResult(editIntent, 2);
                    dialog.dismiss();
                }
            });
        }
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                viewModel.switchCharacter(character);
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    @Override
    public void onLongClick(AICharacter character) {
        if (character.isBuiltin()) {
            new AlertDialog.Builder(this)
                .setTitle(character.getName())
                .setMessage("内置角色不可删除")
                .setPositiveButton("ok", null)
                .create().show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(character.getName());
        builder.setMessage("确定删除" + character.getName() + "吗?");
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                viewModel.deleteCharacter(character);
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
}
