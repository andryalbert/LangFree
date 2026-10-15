package com.langfree.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.langfree.databinding.ItemConversationBinding;
import com.langfree.model.Conversation;
import com.langfree.model.Level;
import com.langfree.model.Topic;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Ligne d'historique : titre, niveau, thème, date, suppression. */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.Holder> {
    public interface Listener {
        void onOpen(Conversation conversation);

        void onDelete(Conversation conversation);
    }

    private final List<Conversation> items = new ArrayList<>();
    private final Listener listener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM, DateFormat.SHORT, Locale.FRANCE);

    public ConversationAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Conversation> conversations) {
        items.clear();
        if (conversations != null) {
            items.addAll(conversations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding binding;

        Holder(ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Conversation conversation) {
            binding.title.setText(conversation.title);
            String subtitle = Level.labelOf(conversation.level)
                    + " · " + Topic.labelOf(conversation.topic)
                    + " · " + dateFormat.format(new Date(conversation.updatedAt));
            binding.subtitle.setText(subtitle);
            binding.content.setOnClickListener(v -> listener.onOpen(conversation));
            binding.delete.setOnClickListener(v -> listener.onDelete(conversation));
        }
    }
}
