package com.zinhao.chtholly.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.zinhao.chtholly.entity.Message;

public class MessageDiffCallback extends DiffUtil.ItemCallback<Message> {
    @Override
    public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
        return oldItem.id == newItem.id;
    }

    @Override
    public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
        if(oldItem.speaker == null || newItem.message == null){
            return false;
        }
        return oldItem.message.equals(newItem.message)
                && oldItem.speaker.equals(newItem.speaker)
                && oldItem.timeStamp == newItem.timeStamp;
    }
}