package com.zinhao.chtholly.view;

import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.zinhao.chtholly.databinding.ActivityAddCharacterBinding;

public class AddCharacterActivity extends AppCompatActivity {
    ActivityAddCharacterBinding binding;
    private long editId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddCharacterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        editId = getIntent().getLongExtra("edit_id", -1);
        boolean isEditMode = editId != -1;

        if (isEditMode) {
            setTitle("Edit Character");
            binding.button3.setText("Save");
            String name = getIntent().getStringExtra("edit_name");
            String desc = getIntent().getStringExtra("edit_desc");
            if (name != null) {
                binding.textInputLayout4.getEditText().setText(name);
            }
            if (desc != null) {
                binding.textInputDesc.getEditText().setText(desc);
            }
        } else {
            setTitle("Add Character");
        }

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
                dataIntent.putExtra("name", name);
                dataIntent.putExtra("desc", desc);
                if (isEditMode) {
                    dataIntent.putExtra("edit_id", editId);
                }
                setResult(RESULT_OK, dataIntent);
                finish();
            }
        });
    }
}
