package com.zinhao.chtholly.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.zinhao.chtholly.R;
import com.zinhao.chtholly.entity.Message;
import com.zinhao.chtholly.utils.MessageDiffCallback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppChatAdapter extends ListAdapter<Message, AppChatAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private String currentUser;

    public AppChatAdapter(String currentUser) {
        super(new MessageDiffCallback());
        this.currentUser = currentUser;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = getItem(position);
        holder.bind(message);
    }

    @Override
    public int getItemViewType(int position) {
        Message message = getItem(position);
        if(message.speaker!=null){
            return message.speaker.equals(currentUser) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
        }else {
            return  VIEW_TYPE_SENT;
        }
    }

    public void updateStreamingMessage(int position, String partialText) {
        if (position >= 0 && position < getItemCount()) {
            Message message = getItem(position);
            message.setMessage(partialText);
            notifyItemChanged(position);
        }
    }

    public void updateStreamingText(RecyclerView recyclerView, int position, String text) {
        if (position >= 0 && position < getItemCount()) {
            Message message = getItem(position);
            message.setMessage(text);
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
            if (holder instanceof MessageViewHolder) {
                ((MessageViewHolder) holder).tvMessage.setText(text);
            }
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final TextView tvTime;
        private final TextView tvSpeaker;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvSpeaker = itemView.findViewById(R.id.tv_speaker);
        }

        public void bind(Message message) {
            tvMessage.setText(message.message);
            tvSpeaker.setText(message.speaker);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tvTime.setText(sdf.format(new Date(message.timeStamp)));
        }
    }
}