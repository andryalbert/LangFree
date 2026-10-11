package com.langfree.ui;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.langfree.R;
import com.langfree.model.Message;
import com.langfree.speech.AnaSpeaker;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Message> messages = new ArrayList<>();
    private OnKeepListener onKeepListener;
    private AnaSpeaker anaSpeaker;
    
    public interface OnKeepListener {
        void onKeep(Message message);
    }
    
    public MessageAdapter(OnKeepListener onKeepListener) {
        this.onKeepListener = onKeepListener;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }
    
    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    public void clear() {
        messages.clear();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageViewHolder vh = (MessageViewHolder) holder;
        Message message = messages.get(position);
        
        if (message.isUserMessage()) {
            // Message utilisateur
            vh.userMessageLayout.setVisibility(View.VISIBLE);
            vh.anaMessageLayout.setVisibility(View.GONE);
            
            vh.userMessageText.setText(message.getText());
            
            // Afficher la correction si présente
            if (message.getCorrection() != null && message.getCorrection().hasErrors()) {
                vh.correctionCard.setVisibility(View.VISIBLE);
                
                // Texte original (simplifié - on pourrait surligner les erreurs)
                vh.originalText.setText(message.getText());
                
                // Texte corrigé
                vh.correctedText.setText(message.getCorrection().getCorrectedText());
                
                // Explication
                vh.explanationText.setText(message.getCorrection().getExplanation());
                
                // Bouton garder
                vh.keepButton.setOnClickListener(v -> {
                    if (onKeepListener != null) {
                        onKeepListener.onKeep(message);
                    }
                });
            } else if (message.getCorrection() != null) {
                // Pas d'erreurs
                vh.correctionCard.setVisibility(View.VISIBLE);
                vh.originalText.setText(message.getText());
                vh.correctedText.setText("✓ " + message.getCorrection().getExplanation());
                vh.explanationText.setText("");
                vh.keepButton.setVisibility(View.GONE);
            } else {
                vh.correctionCard.setVisibility(View.GONE);
            }
            
        } else {
            // Message d'Ana
            vh.userMessageLayout.setVisibility(View.GONE);
            vh.anaMessageLayout.setVisibility(View.VISIBLE);
            
            vh.anaMessageText.setText(message.getText());
            
            // Bouton pour écouter
            vh.speakButton.setOnClickListener(v -> {
                if (anaSpeaker == null) {
                    anaSpeaker = AnaSpeaker.getInstance(v.getContext());
                }
                anaSpeaker.speakSimple(message.getText());
            });
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout userMessageLayout;
        LinearLayout anaMessageLayout;
        TextView userMessageText;
        TextView anaMessageText;
        CardView correctionCard;
        TextView originalText;
        TextView correctedText;
        TextView explanationText;
        Button keepButton;
        ImageButton speakButton;
        
        public MessageViewHolder(View itemView) {
            super(itemView);
            
            userMessageLayout = itemView.findViewById(R.id.userMessageLayout);
            anaMessageLayout = itemView.findViewById(R.id.anaMessageLayout);
            userMessageText = itemView.findViewById(R.id.userMessageText);
            anaMessageText = itemView.findViewById(R.id.anaMessageText);
            correctionCard = itemView.findViewById(R.id.correctionCard);
            originalText = itemView.findViewById(R.id.originalText);
            correctedText = itemView.findViewById(R.id.correctedText);
            explanationText = itemView.findViewById(R.id.explanationText);
            keepButton = itemView.findViewById(R.id.keepButton);
            speakButton = itemView.findViewById(R.id.speakButton);
        }
    }
}
