package com.langfree.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.langfree.R;
import com.langfree.databinding.ActivitySettingsBinding;
import com.langfree.model.CorrectionMode;
import com.langfree.model.Level;
import com.langfree.model.Topic;
import com.langfree.model.UserSettings;
import com.langfree.viewmodel.SettingsViewModel;


/** Niveau, thème, correction, voix, taille du texte et apparence. */
public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;
    private boolean bindingViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        String[] topics = new String[Topic.values().length];
        for (int i = 0; i < Topic.values().length; i++) {
            topics[i] = Topic.values()[i].labelFr;
        }
        binding.topic.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, topics));

        UserSettings settings = viewModel.current();
        show(settings);
        wireListeners();

        viewModel.message.observe(this, signal -> {
            if (signal != null && !signal.message.isEmpty()) {
                Snackbar.make(binding.getRoot(), signal.message, Snackbar.LENGTH_LONG).show();
            }
        });
        binding.releaseMemory.setOnClickListener(v -> viewModel.releaseMemory());
        binding.deleteModel.setOnClickListener(v -> confirmDelete());
    }

    private void show(UserSettings settings) {
        bindingViews = true;
        switch (settings.level) {
            case B1_B2:
                binding.levelGroup.check(R.id.levelIntermediate);
                break;
            case C1_C2:
                binding.levelGroup.check(R.id.levelAdvanced);
                break;
            case A1_A2:
            default:
                binding.levelGroup.check(R.id.levelBeginner);
                break;
        }
        binding.modeGroup.check(settings.correctionMode == CorrectionMode.STRICT
                ? R.id.modeStrict : R.id.modeGentle);
        binding.topic.setSelection(settings.topic.ordinal());
        binding.speech.setValue(clamp(settings.speechRate, 0.7f, 1.4f));
        binding.textSize.setValue(clamp(settings.textSizeSp, 14, 22));
        if (settings.nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            binding.themeGroup.check(R.id.themeDark);
        } else if (settings.nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            binding.themeGroup.check(R.id.themeLight);
        } else {
            binding.themeGroup.check(R.id.themeSystem);
        }
        binding.speechValue.setText(getString(R.string.speech_value, settings.speechRate));
        binding.textValue.setText(getString(R.string.text_value, settings.textSizeSp));
        bindingViews = false;
    }

    private void wireListeners() {
        binding.levelGroup.setOnCheckedChangeListener((group, checkedId) -> saveIfReady());
        binding.modeGroup.setOnCheckedChangeListener((group, checkedId) -> saveIfReady());
        binding.themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (bindingViews) {
                return;
            }
            UserSettings settings = collect();
            viewModel.save(settings);
            AppCompatDelegate.setDefaultNightMode(settings.nightMode);
        });
        binding.topic.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                saveIfReady();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        binding.speech.addOnChangeListener((slider, value, fromUser) -> {
            float snapped = Math.round(value * 10f) / 10f;
            binding.speechValue.setText(getString(R.string.speech_value, snapped));
            if (fromUser) {
                saveIfReady();
            }
        });
        binding.textSize.addOnChangeListener((slider, value, fromUser) -> {
            binding.textValue.setText(getString(R.string.text_value, Math.round(value)));
            if (fromUser) {
                saveIfReady();
            }
        });
    }

    private void saveIfReady() {
        if (bindingViews) {
            return;
        }
        viewModel.save(collect());
    }

    private UserSettings collect() {
        UserSettings settings = viewModel.current();
        int levelId = binding.levelGroup.getCheckedRadioButtonId();
        if (levelId == R.id.levelIntermediate) {
            settings.level = Level.B1_B2;
        } else if (levelId == R.id.levelAdvanced) {
            settings.level = Level.C1_C2;
        } else {
            settings.level = Level.A1_A2;
        }
        settings.correctionMode = binding.modeGroup.getCheckedRadioButtonId() == R.id.modeStrict
                ? CorrectionMode.STRICT : CorrectionMode.GENTLE;
        int topicIndex = binding.topic.getSelectedItemPosition();
        Topic[] topics = Topic.values();
        if (topicIndex >= 0 && topicIndex < topics.length) {
            settings.topic = topics[topicIndex];
        }
        settings.speechRate = binding.speech.getValue();
        settings.textSizeSp = Math.round(binding.textSize.getValue());
        int themeId = binding.themeGroup.getCheckedRadioButtonId();
        if (themeId == R.id.themeDark) {
            settings.nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if (themeId == R.id.themeLight) {
            settings.nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            settings.nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        return settings;
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_model_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.deleteModel())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
