package com.zinhao.chtholly.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.R;
import com.zinhao.chtholly.databinding.CharacterItemBinding;
import com.zinhao.chtholly.entity.AICharacter;
import com.zinhao.chtholly.utils.AsyncHelper;
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
            CharacterViewHolder holder = (CharacterViewHolder) viewHolder;
            holder.tvDesc.setText(aiCharacter.getDesc());
            holder.tvTitle.setText(aiCharacter.getName());
            holder.cbRoleplay.setChecked(aiCharacter.isRoleplay());
            holder.cbRoleplay.setOnClickListener(v -> {
                boolean isChecked = holder.cbRoleplay.isChecked();
                aiCharacter.setRoleplay(isChecked);
                long charId = aiCharacter.getId();
                AsyncHelper.INSTANCE.doAsyncPart(() -> {
                    BotApp.getInstance().getCharacterDao().updateRoleplay(charId, isChecked);
                });
                AICharacter current = BotApp.getInstance().getCurrentCharacter();
                if (current != null && current.getId() == aiCharacter.getId()) {
                    BotApp.getInstance().setRoleplay(isChecked);
                }
            });
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemClickListener.onItemClick(aiCharacter);
                }
            });
            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    itemClickListener.onLongClick(aiCharacter);
                    return true;
                }
            });

        }
    }

    public void removeItem(AICharacter aiCharacter){
        int index = data.indexOf(aiCharacter);
        notifyItemRemoved(index);
        data.remove(aiCharacter);
        notifyItemRangeChanged(index,data.size()-index);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class CharacterViewHolder extends RecyclerView.ViewHolder{
        TextView tvTitle;
        TextView tvDesc;
        CheckBox cbRoleplay;
        public CharacterViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.textView2);
            tvDesc = itemView.findViewById(R.id.textView3);
            cbRoleplay = itemView.findViewById(R.id.cbRoleplay);
        }
    }

    public interface ItemClickListener {
        void onItemClick(AICharacter character);
        void onLongClick(AICharacter character);
    }
}
