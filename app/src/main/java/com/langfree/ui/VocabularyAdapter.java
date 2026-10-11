package com.langfree.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.langfree.R;
import com.langfree.model.VocabularyItem;
import com.langfree.speech.AnaSpeaker;

import java.util.ArrayList;
import java.util.List;

public class VocabularyAdapter extends RecyclerView.Adapter<VocabularyAdapter.ViewHolder> {
    private List<VocabularyItem> items = new ArrayList<>();
    private OnDeleteListener onDeleteListener;
    private AnaSpeaker anaSpeaker;
    
    public interface OnDeleteListener {
        void onDelete(VocabularyItem item);
    }
    
    public VocabularyAdapter(OnDeleteListener onDeleteListener) {
        this.onDeleteListener = onDeleteListener;
    }
    
    public void setItems(List<VocabularyItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }
    
    public void addItem(VocabularyItem item) {
        items.add(0, item);
        notifyItemInserted(0);
    }
    
    public void removeItem(VocabularyItem item) {
        int position = items.indexOf(item);
        if (position >= 0) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vocabulary, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VocabularyItem item = items.get(position);
        
        holder.englishText.setText(item.getEnglish());
        holder.frenchText.setText(item.getFrench());
        
        // Bouton pour écouter
        holder.speakButton.setOnClickListener(v -> {
            if (anaSpeaker == null) {
                anaSpeaker = AnaSpeaker.getInstance(v.getContext());
            }
            anaSpeaker.speakSimple(item.getEnglish());
        });
        
        // Bouton pour supprimer
        holder.deleteButton.setOnClickListener(v -> {
            if (onDeleteListener != null) {
                onDeleteListener.onDelete(item);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView englishText;
        TextView frenchText;
        ImageButton speakButton;
        ImageButton deleteButton;
        
        public ViewHolder(View itemView) {
            super(itemView);
            englishText = itemView.findViewById(R.id.vocabularyEnglish);
            frenchText = itemView.findViewById(R.id.vocabularyFrench);
            speakButton = itemView.findViewById(R.id.speakVocabButton);
            deleteButton = itemView.findViewById(R.id.deleteVocabButton);
        }
    }
}
