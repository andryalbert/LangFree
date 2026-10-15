package com.langfree.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.langfree.R;
import com.langfree.databinding.ActivityMistakesBinding;
import com.langfree.model.Mistake;
import com.langfree.viewmodel.MistakesViewModel;

/** Carnet des erreurs fréquentes et accès à la révision. */
public class MistakesActivity extends AppCompatActivity implements MistakeAdapter.DeleteListener {
    private ActivityMistakesBinding binding;
    private MistakesViewModel viewModel;
    private MistakeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMistakesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(MistakesViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new MistakeAdapter(this);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        viewModel.mistakes.observe(this, mistakes -> {
            adapter.submit(mistakes);
            boolean empty = mistakes == null || mistakes.isEmpty();
            binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.review.setEnabled(!empty);
            binding.clear.setEnabled(!empty);
        });
        binding.clear.setOnClickListener(v -> confirmClear());
        binding.review.setOnClickListener(v -> {
            if (adapter.isEmpty()) {
                Snackbar.make(binding.getRoot(), R.string.review_empty, Snackbar.LENGTH_LONG).show();
                return;
            }
            startActivity(new Intent(this, ReviewActivity.class));
        });
    }

    @Override
    public void onDelete(Mistake mistake) {
        viewModel.delete(mistake);
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.clear_mistakes_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.clearAll())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
