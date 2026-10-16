package com.langfree.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.langfree.R;
import com.langfree.ai.QuizParser;
import com.langfree.databinding.ItemQuizTurnBinding;
import com.langfree.model.QuizTurn;

import java.util.ArrayList;
import java.util.List;

/** Détail d'un quiz : question, réponse, correction. */
public class QuizTurnAdapter extends RecyclerView.Adapter<QuizTurnAdapter.Holder> {
    private final List<QuizTurn> items = new ArrayList<>();

    public void submit(List<QuizTurn> turns) {
        items.clear();
        if (turns != null) {
            for (QuizTurn turn : turns) {
                if (turn.answer != null && !turn.answer.isEmpty()) {
                    items.add(turn);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemQuizTurnBinding.inflate(
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

    static final class Holder extends RecyclerView.ViewHolder {
        private final ItemQuizTurnBinding binding;

        Holder(ItemQuizTurnBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(QuizTurn turn) {
            String level = QuizParser.levelLabel(turn.levelEstimate);
            if (level.isEmpty()) {
                binding.level.setVisibility(View.GONE);
            } else {
                binding.level.setVisibility(View.VISIBLE);
                binding.level.setText(itemView.getContext().getString(R.string.quiz_level, level));
            }
            binding.question.setText(turn.question);
            binding.answer.setText(itemView.getContext().getString(R.string.quiz_you_said, turn.answer));
            binding.correction.setText(itemView.getContext().getString(
                    R.string.quiz_corrected, turn.correction == null ? "" : turn.correction));
            boolean hasExplanation = turn.explanation != null && !turn.explanation.isEmpty();
            binding.explanation.setVisibility(hasExplanation ? View.VISIBLE : View.GONE);
            binding.explanation.setText(turn.explanation);
        }
    }
}
