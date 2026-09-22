package com.zinhao.chtholly.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.zinhao.chtholly.R;
import com.zinhao.chtholly.entity.Message;
import com.zinhao.chtholly.utils.MessageDiffCallback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppChatAdapter extends ListAdapter<Message, AppChatAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    public interface OnResendClickListener {
        void onResendClick(Message message);
    }

    private String currentUser;
    private OnResendClickListener resendClickListener;
    private boolean resendEnabled = true;

    public AppChatAdapter(String currentUser) {
        super(new MessageDiffCallback());
        this.currentUser = currentUser;
    }

    public AppChatAdapter(String currentUser, OnResendClickListener resendClickListener) {
        this(currentUser);
        this.resendClickListener = resendClickListener;
    }

    /**
     * 重发按钮是否可用（流式回复期间禁用）
     */
    public void setResendEnabled(boolean enabled) {
        if (this.resendEnabled == enabled) return;
        this.resendEnabled = enabled;
        int position = findLastSentPosition(getCurrentList());
        if (position != -1) {
            notifyItemChanged(position);
        }
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
        bindResend(holder, position);
    }

    private void bindResend(MessageViewHolder holder, int position) {
        if (holder.btnResend == null) return;
        boolean showResend = resendClickListener != null
                && position == findLastSentPosition(getCurrentList());
        holder.btnResend.setVisibility(showResend ? View.VISIBLE : View.GONE);
        holder.btnResend.setEnabled(resendEnabled);
        if (showResend) {
            holder.btnResend.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                Message current = getItem(pos);
                if (resendClickListener != null) {
                    resendClickListener.onResendClick(current);
                }
            });
        } else {
            holder.btnResend.setOnClickListener(null);
        }
    }

    /**
     * 列表变化后，“最后一条发送消息”的位置可能移动，需刷新旧位置收起按钮，
     * 否则 DiffUtil 认为旧条目内容未变不会重新绑定，会出现多个按钮同时显示。
     */
    @Override
    public void onCurrentListChanged(@NonNull List<Message> previousList, @NonNull List<Message> currentList) {
        super.onCurrentListChanged(previousList, currentList);
        int previousPosition = findLastSentPosition(previousList);
        int currentPosition = findLastSentPosition(currentList);
        if (previousPosition != -1 && previousPosition != currentPosition) {
            notifyItemChanged(previousPosition);
        }
    }

    /**
     * 从尾部往前找第一条自己发送的消息，显隐逻辑与 getItemViewType 保持一致
     */
    private int findLastSentPosition(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (isSent(message)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isSent(Message message) {
        if (message.speaker != null) {
            return message.speaker.equals(currentUser);
        }
        return true;
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
        private final ImageButton btnResend;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvSpeaker = itemView.findViewById(R.id.tv_speaker);
            btnResend = itemView.findViewById(R.id.btn_resend);
        }

        public void bind(Message message) {
            tvMessage.setText(message.message);
            tvSpeaker.setText(message.speaker);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tvTime.setText(sdf.format(new Date(message.timeStamp)));
        }
    }
}
