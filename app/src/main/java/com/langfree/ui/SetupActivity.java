package com.langfree.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.langfree.R;
import com.langfree.data.SettingsRepository;
import com.langfree.databinding.ActivitySetupBinding;
import com.langfree.utils.ModelStorage;
import com.langfree.viewmodel.SetupViewModel;

/**
 * Accueil : vérifie le modèle, guide l'import ou le téléchargement, puis ouvre le chat.
 */
public class SetupActivity extends AppCompatActivity {
    private ActivitySetupBinding binding;
    private SetupViewModel viewModel;
    private final ActivityResultLauncher<String[]> openDocument =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFilePicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(SetupViewModel.class);

        SettingsRepository settings = new SettingsRepository(this);
        binding.token.setText(settings.getHfToken());

        viewModel.modelReady.observe(this, ready ->
                binding.startChat.setEnabled(Boolean.TRUE.equals(ready) && !Boolean.TRUE.equals(viewModel.busy.getValue())));
        viewModel.busy.observe(this, busy -> {
            boolean working = Boolean.TRUE.equals(busy);
            binding.download.setEnabled(!working);
            binding.importModel.setEnabled(!working);
            binding.startChat.setEnabled(!working && Boolean.TRUE.equals(viewModel.modelReady.getValue()));
            binding.progress.setVisibility(working ? View.VISIBLE : View.GONE);
            binding.progressLabel.setVisibility(working ? View.VISIBLE : View.GONE);
        });
        viewModel.progress.observe(this, percent -> {
            if (percent == null) {
                return;
            }
            if (percent < 0) {
                binding.progress.setIndeterminate(true);
            } else {
                binding.progress.setIndeterminate(false);
                binding.progress.setProgress(percent);
            }
            binding.progressLabel.setText(viewModel.formatProgress(percent));
        });
        viewModel.status.observe(this, binding.status::setText);
        viewModel.memoryHint.observe(this, binding.memory::setText);
        viewModel.message.observe(this, signal -> {
            if (signal == null || signal.message.isEmpty()) {
                return;
            }
            Snackbar snackbar = Snackbar.make(binding.getRoot(), signal.message, Snackbar.LENGTH_LONG);
            if (signal.message.equals(getString(R.string.error_hf_auth))) {
                snackbar.setAction(R.string.open_license, v ->
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ModelStorage.MODEL_PAGE))));
            }
            snackbar.show();
        });

        binding.openModelPage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ModelStorage.MODEL_PAGE));
            startActivity(intent);
        });
        binding.download.setOnClickListener(v -> {
            String token = textOf(binding.token.getText());
            settings.setHfToken(token);
            viewModel.downloadModel(token);
        });
        binding.importModel.setOnClickListener(v -> openDocument.launch(new String[]{"*/*"}));
        binding.startChat.setOnClickListener(v ->
                startActivity(new Intent(this, ChatActivity.class)));
        binding.history.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        binding.mistakes.setOnClickListener(v ->
                startActivity(new Intent(this, MistakesActivity.class)));
        binding.settings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // L'accueil ne garde pas le modèle en mémoire : le chat le rechargera.
        viewModel.releaseModel();
        viewModel.refresh();
    }

    private void onFilePicked(Uri uri) {
        if (uri != null) {
            viewModel.importModel(uri);
        }
    }

    private static String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
