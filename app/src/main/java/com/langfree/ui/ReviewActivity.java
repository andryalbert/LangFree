package com.langfree.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.langfree.R;
import com.langfree.databinding.ActivityReviewBinding;
import com.langfree.databinding.ItemQuizBinding;
import com.langfree.model.QuizItem;
import com.langfree.viewmodel.ReviewViewModel;

import java.util.List;

/**
 * Quiz local immédiat, plus un texte d'exercice produit par le modèle s'il est disponible.
 */
public class ReviewActivity extends AppCompatActivity {
    private ActivityReviewBinding binding;
    private ReviewViewModel viewModel;
    private boolean quizBuilt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel.quizReady.observe(this, ready -> {
            if (!Boolean.TRUE.equals(ready) || quizBuilt) {
                return;
            }
            quizBuilt = true;
            List<QuizItem> items = viewModel.quiz.getValue();
            boolean empty = items == null || items.isEmpty();
            binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (!empty) {
                buildQuiz(items);
            }
        });
        viewModel.generating.observe(this, generating -> {
            boolean busy = Boolean.TRUE.equals(generating);
            binding.aiBox.setVisibility(busy || hasAiText() ? View.VISIBLE : View.GONE);
            binding.aiProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        });
        viewModel.aiExercise.observe(this, text -> {
            binding.aiExercise.setText(text == null ? "" : text);
            if (text != null && !text.isEmpty()) {
                binding.aiBox.setVisibility(View.VISIBLE);
            }
        });
        viewModel.message.observe(this, signal -> {
            if (signal != null && !signal.message.isEmpty()) {
                Snackbar.make(binding.getRoot(), signal.message, Snackbar.LENGTH_LONG).show();
            }
        });
        viewModel.load();
    }

    private boolean hasAiText() {
        CharSequence text = binding.aiExercise.getText();
        return text != null && text.length() > 0;
    }

    private void buildQuiz(List<QuizItem> items) {
        binding.quiz.removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            QuizItem item = items.get(i);
            ItemQuizBinding row = ItemQuizBinding.inflate(getLayoutInflater(), binding.quiz, false);
            row.prompt.setText(getString(R.string.mistake_you, item.prompt));
            row.attempt.setText(item.attempt);
            row.feedback.setText(item.feedback);
            applyFeedbackColor(row, item);
            row.attempt.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.attempt = s == null ? "" : s.toString();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            row.check.setOnClickListener(v -> {
                String attempt = row.attempt.getText() == null ? "" : row.attempt.getText().toString();
                if (attempt.trim().isEmpty()) {
                    Snackbar.make(binding.getRoot(), R.string.quiz_empty_attempt, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                viewModel.check(item, attempt);
                row.feedback.setText(item.feedback);
                applyFeedbackColor(row, item);
            });
            binding.quiz.addView(row.getRoot(), new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void applyFeedbackColor(ItemQuizBinding row, QuizItem item) {
        if (item.feedback == null || item.feedback.isEmpty()) {
            return;
        }
        int color = ContextCompat.getColor(this, item.correct ? R.color.success : R.color.error);
        row.feedback.setTextColor(color);
    }
}
