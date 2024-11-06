package com.zinhao.chtholly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.zinhao.chtholly.databinding.CharacterItemBinding;
import com.zinhao.chtholly.entity.AICharacter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CharacterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<AICharacter> data;
    private ItemClickListener itemClickListener;

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public CharacterAdapter(List<AICharacter> data) {
        this.data = data;
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup viewGroup, int i) {
        return new CharacterViewHolder(CharacterItemBinding.inflate(LayoutInflater.from(viewGroup.getContext())).getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder viewHolder, int i) {
        final AICharacter aiCharacter = data.get(i);
        if(viewHolder instanceof CharacterViewHolder){
            ((CharacterViewHolder) viewHolder).tvDesc.setText(aiCharacter.getDesc());
            ((CharacterViewHolder) viewHolder).tvTitle.setText(aiCharacter.getName());
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemClickListener.onItemClick(aiCharacter);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class CharacterViewHolder extends RecyclerView.ViewHolder{
        TextView tvTitle;
        TextView tvDesc;
        public CharacterViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.textView2);
            tvDesc = itemView.findViewById(R.id.textView3);
        }
    }

    interface ItemClickListener {
        void onItemClick(AICharacter character);
    }
}
