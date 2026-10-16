package com.langfree.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.langfree.databinding.ActivityQuizDetailBinding;
import com.langfree.viewmodel.QuizHistoryViewModel;

/** Questions, réponses et corrections d'un quiz passé. */
public class QuizDetailActivity extends AppCompatActivity {
    public static final String EXTRA_SESSION_ID = "quiz_session_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityQuizDetailBinding binding = ActivityQuizDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        QuizHistoryViewModel viewModel = new ViewModelProvider(this).get(QuizHistoryViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        QuizTurnAdapter adapter = new QuizTurnAdapter();
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        viewModel.turns.observe(this, adapter::submit);
        viewModel.open(getIntent().getLongExtra(EXTRA_SESSION_ID, 0));
    }
}
