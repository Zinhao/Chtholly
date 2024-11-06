package com.zinhao.chtholly;

import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.zinhao.chtholly.databinding.ActivityAddCharacterBinding;

public class AddCharacterActivity extends AppCompatActivity {
    ActivityAddCharacterBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddCharacterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("Add Character");
        binding.button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.textInputLayout4.getEditText().getText().toString().trim();
                String desc = binding.textInputDesc.getEditText().getText().toString().trim();
                if(name.isEmpty() || desc.isEmpty()){
                    Toast.makeText(AddCharacterActivity.this, "input is empty!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent dataIntent = new Intent();
                dataIntent.putExtra("name",name);
                dataIntent.putExtra("desc",desc);
                setResult(RESULT_OK,dataIntent);
                finish();
            }
        });
    }
}