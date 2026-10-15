package com.langfree.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.langfree.LangFreeApp;
import com.langfree.R;
import com.langfree.ai.LlmManager;
import com.langfree.data.SettingsRepository;
import com.langfree.model.UiSignal;
import com.langfree.model.UserSettings;
import com.langfree.utils.ModelStorage;

/**
 * Lecture et écriture des réglages. Le thème nuit est appliqué par l'activité.
 */
public class SettingsViewModel extends AndroidViewModel {
    public final MutableLiveData<UserSettings> settings = new MutableLiveData<>();
    public final MutableLiveData<UiSignal> message = new MutableLiveData<>();

    private final SettingsRepository repository;
    private final LlmManager llmManager;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        repository = new SettingsRepository(application);
        llmManager = ((LangFreeApp) application).getLlmManager();
        settings.setValue(repository.load());
    }

    public void save(UserSettings value) {
        repository.save(value);
        settings.setValue(value.copy());
    }

    public UserSettings current() {
        UserSettings value = settings.getValue();
        return value == null ? repository.load() : value.copy();
    }

    public void deleteModel() {
        llmManager.releaseThen(() -> {
            boolean deleted = ModelStorage.delete(getApplication());
            message.setValue(new UiSignal(getApplication().getString(
                    deleted ? R.string.model_deleted : R.string.error_delete_model)));
        });
    }

    public void releaseMemory() {
        llmManager.releaseThen(() ->
                message.setValue(new UiSignal(getApplication().getString(R.string.memory_released))));
    }
}
