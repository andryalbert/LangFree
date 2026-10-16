package com.langfree.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.langfree.R;
import com.langfree.databinding.ActivityQuizBinding;
import com.langfree.viewmodel.QuizViewModel;

/** L'IA pose une question en anglais, puis corrige la réponse. */
public class QuizActivity extends AppCompatActivity {
    private ActivityQuizBinding binding;
    private QuizViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel.phase.observe(this, this::showPhase);
        viewModel.question.observe(this, text -> {
            binding.question.setText(text == null ? "" : text);
            boolean visible = text != null && !text.isEmpty();
            binding.questionLabel.setVisibility(visible ? View.VISIBLE : View.GONE);
        });
        viewModel.status.observe(this, text -> {
            boolean visible = text != null && !text.isEmpty();
            binding.status.setVisibility(visible ? View.VISIBLE : View.GONE);
            binding.status.setText(text);
        });
        viewModel.correction.observe(this, text -> binding.correction.setText(text == null ? "" : text));
        viewModel.explanation.observe(this, text -> {
            boolean visible = text != null && !text.isEmpty();
            binding.explanation.setText(visible ? text : "");
            binding.explanation.setVisibility(visible ? View.VISIBLE : View.GONE);
            binding.explanationLabel.setVisibility(visible ? View.VISIBLE : View.GONE);
        });
        viewModel.levelLabel.observe(this, text -> {
            if (text == null || text.isEmpty()) {
                binding.level.setText(R.string.quiz_level_unknown);
            } else {
                binding.level.setText(getString(R.string.quiz_level, text));
            }
        });
        viewModel.error.observe(this, signal -> {
            if (signal != null && signal.message != null && !signal.message.isEmpty()) {
                Snackbar.make(binding.getRoot(), signal.message, Snackbar.LENGTH_LONG).show();
            }
        });

        binding.send.setOnClickListener(v -> submit());
        binding.answer.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submit();
                return true;
            }
            return false;
        });
        binding.next.setOnClickListener(v -> {
            binding.answer.setText("");
            viewModel.nextQuestion();
        });
        binding.retry.setOnClickListener(v -> viewModel.retry());
        viewModel.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_quiz, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_quiz_history) {
            startActivity(new Intent(this, QuizHistoryActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void submit() {
        String text = binding.answer.getText() == null ? "" : binding.answer.getText().toString();
        if (text.trim().isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.quiz_empty_answer, Snackbar.LENGTH_SHORT).show();
            return;
        }
        viewModel.submit(text);
    }

    private void showPhase(QuizViewModel.Phase phase) {
        if (phase == null) {
            return;
        }
        boolean answering = phase == QuizViewModel.Phase.ANSWERING;
        boolean result = phase == QuizViewModel.Phase.RESULT;
        boolean failed = phase == QuizViewModel.Phase.FAILED;
        binding.feedback.setVisibility(result ? View.VISIBLE : View.GONE);
        binding.answerBar.setVisibility(answering ? View.VISIBLE : View.GONE);
        binding.next.setVisibility(result ? View.VISIBLE : View.GONE);
        binding.retry.setVisibility(failed ? View.VISIBLE : View.GONE);
        binding.send.setEnabled(answering);
    }
}
