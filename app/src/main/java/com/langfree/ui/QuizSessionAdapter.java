package com.langfree.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.langfree.R;
import com.langfree.ai.QuizParser;
import com.langfree.databinding.ItemConversationBinding;
import com.langfree.model.QuizSession;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Ligne d'historique d'un quiz. */
public class QuizSessionAdapter extends RecyclerView.Adapter<QuizSessionAdapter.Holder> {
    public interface Listener {
        void onOpen(QuizSession session);

        void onDelete(QuizSession session);
    }

    private final List<QuizSession> items = new ArrayList<>();
    private final Listener listener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM, DateFormat.SHORT, Locale.FRANCE);

    public QuizSessionAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<QuizSession> sessions) {
        items.clear();
        if (sessions != null) {
            items.addAll(sessions);
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

        void bind(QuizSession session) {
            binding.title.setText(dateFormat.format(new Date(session.updatedAt)));
            String count = itemView.getContext().getResources().getQuantityString(
                    R.plurals.quiz_answer_count, session.questionCount, session.questionCount);
            String level = QuizParser.levelLabel(session.levelEstimate);
            binding.subtitle.setText(level.isEmpty() ? count : count + " · " + level);
            binding.content.setOnClickListener(v -> listener.onOpen(session));
            binding.delete.setOnClickListener(v -> listener.onDelete(session));
        }
    }
}
