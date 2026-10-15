package com.langfree.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.langfree.R;
import com.langfree.databinding.ItemMessageAssistantBinding;
import com.langfree.databinding.ItemMessageStreamingBinding;
import com.langfree.databinding.ItemMessageUserBinding;
import com.langfree.model.ChatMessage;
import com.langfree.model.TutorResponse;
import com.langfree.utils.TextDiff;

import java.util.ArrayList;
import java.util.List;

/**
 * Bulles utilisateur, bulles tuteur en trois sections, et bulle de streaming.
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface ListenListener {
        void onListen(String englishText);
    }

    private static final int TYPE_USER = 1;
    private static final int TYPE_ASSISTANT = 2;
    private static final int TYPE_STREAM = 3;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final ListenListener listenListener;
    private String streaming;
    private int textSizeSp = 16;

    public MessageAdapter(ListenListener listenListener) {
        this.listenListener = listenListener;
    }

    public void setMessages(List<ChatMessage> items) {
        messages.clear();
        if (items != null) {
            messages.addAll(items);
        }
        notifyDataSetChanged();
    }

    public void setStreaming(String text) {
        streaming = (text == null || text.isEmpty()) ? null : text;
        notifyDataSetChanged();
    }

    public void setTextSizeSp(int size) {
        textSizeSp = size;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return messages.size() + (streaming == null ? 0 : 1);
    }

    @Override
    public int getItemViewType(int position) {
        if (streaming != null && position == messages.size()) {
            return TYPE_STREAM;
        }
        return messages.get(position).isUser() ? TYPE_USER : TYPE_ASSISTANT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            return new UserHolder(ItemMessageUserBinding.inflate(inflater, parent, false));
        }
        if (viewType == TYPE_STREAM) {
            return new StreamHolder(ItemMessageStreamingBinding.inflate(inflater, parent, false));
        }
        return new AssistantHolder(ItemMessageAssistantBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof UserHolder) {
            ((UserHolder) holder).bind(messages.get(position), textSizeSp);
        } else if (holder instanceof StreamHolder) {
            ((StreamHolder) holder).bind(streaming, textSizeSp);
        } else if (holder instanceof AssistantHolder) {
            String previousUser = null;
            if (position > 0 && messages.get(position - 1).isUser()) {
                previousUser = messages.get(position - 1).rawText;
            }
            ((AssistantHolder) holder).bind(messages.get(position), previousUser, textSizeSp, listenListener);
        }
    }

    static final class UserHolder extends RecyclerView.ViewHolder {
        private final ItemMessageUserBinding binding;

        UserHolder(ItemMessageUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message, int textSizeSp) {
            binding.messageText.setText(message.rawText);
            binding.messageText.setTextSize(textSizeSp);
        }
    }

    static final class StreamHolder extends RecyclerView.ViewHolder {
        private final ItemMessageStreamingBinding binding;

        StreamHolder(ItemMessageStreamingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String text, int textSizeSp) {
            binding.streamingText.setText(text);
            binding.streamingText.setTextSize(textSizeSp);
        }
    }

    static final class AssistantHolder extends RecyclerView.ViewHolder {
        private final ItemMessageAssistantBinding binding;

        AssistantHolder(ItemMessageAssistantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message, String previousUser, int textSizeSp, ListenListener listener) {
            binding.correctionText.setTextSize(textSizeSp);
            binding.explanationText.setTextSize(textSizeSp);
            binding.replyText.setTextSize(textSizeSp);
            binding.fallbackText.setTextSize(textSizeSp);
            String spoken;
            if (message.fallback) {
                binding.sections.setVisibility(View.GONE);
                binding.fallbackText.setVisibility(View.VISIBLE);
                binding.fallbackText.setText(message.rawText);
                spoken = message.rawText;
            } else {
                binding.sections.setVisibility(View.VISIBLE);
                binding.fallbackText.setVisibility(View.GONE);
                int highlight = ContextCompat.getColor(binding.getRoot().getContext(), R.color.highlight_error);
                if (TutorResponse.looksPerfect(message.correction)) {
                    binding.correctionText.setText(message.correction);
                } else {
                    binding.correctionText.setText(TextDiff.highlight(message.correction, previousUser, highlight));
                }
                binding.explanationText.setText(message.explanation);
                binding.replyText.setText(message.reply);
                spoken = message.reply == null || message.reply.isEmpty() ? message.rawText : message.reply;
            }
            binding.listen.setOnClickListener(v -> {
                if (spoken != null && !spoken.isEmpty()) {
                    listener.onListen(spoken);
                }
            });
        }
    }
}
