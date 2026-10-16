package com.langfree.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.langfree.R;
import com.langfree.databinding.ActivityQuizHistoryBinding;
import com.langfree.model.QuizSession;
import com.langfree.viewmodel.QuizHistoryViewModel;

/** Liste des quiz déjà faits. */
public class QuizHistoryActivity extends AppCompatActivity implements QuizSessionAdapter.Listener {
    private ActivityQuizHistoryBinding binding;
    private QuizHistoryViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(QuizHistoryViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        QuizSessionAdapter adapter = new QuizSessionAdapter(this);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        viewModel.sessions.observe(this, sessions -> {
            adapter.submit(sessions);
            boolean empty = sessions == null || sessions.isEmpty();
            binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onOpen(QuizSession session) {
        startActivity(new Intent(this, QuizDetailActivity.class)
                .putExtra(QuizDetailActivity.EXTRA_SESSION_ID, session.id));
    }

    @Override
    public void onDelete(QuizSession session) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.quiz_delete_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.delete(session))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
