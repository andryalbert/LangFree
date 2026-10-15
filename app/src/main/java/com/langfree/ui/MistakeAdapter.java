package com.langfree.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.langfree.R;
import com.langfree.databinding.ItemMistakeBinding;
import com.langfree.model.Mistake;

import java.util.ArrayList;
import java.util.List;

/** Carte d'erreur : fréquence, explication française et exemple. */
public class MistakeAdapter extends RecyclerView.Adapter<MistakeAdapter.Holder> {
    public interface DeleteListener {
        void onDelete(Mistake mistake);
    }

    private final List<Mistake> items = new ArrayList<>();
    private final DeleteListener listener;

    public MistakeAdapter(DeleteListener listener) {
        this.listener = listener;
    }

    public void submit(List<Mistake> mistakes) {
        items.clear();
        if (mistakes != null) {
            items.addAll(mistakes);
        }
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemMistakeBinding.inflate(
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
        private final ItemMistakeBinding binding;

        Holder(ItemMistakeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Mistake mistake) {
            binding.count.setText(binding.getRoot().getContext().getString(
                    R.string.mistake_count, mistake.hitCount));
            binding.explanation.setText(mistake.explanation);
            binding.original.setText(binding.getRoot().getContext().getString(
                    R.string.mistake_you, mistake.originalText));
            binding.corrected.setText(binding.getRoot().getContext().getString(
                    R.string.mistake_fixed, mistake.correctedText));
            binding.delete.setOnClickListener(v -> listener.onDelete(mistake));
        }
    }
}
