package com.langfree.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.langfree.LangFreeApp;
import com.langfree.R;
import com.langfree.ai.LlmManager;
import com.langfree.ai.ResponseParser;
import com.langfree.data.AppDatabase;
import com.langfree.data.ChatRepository;
import com.langfree.data.SettingsRepository;
import com.langfree.model.ChatMessage;
import com.langfree.model.Conversation;
import com.langfree.model.TutorResponse;
import com.langfree.model.UiSignal;
import com.langfree.model.UserSettings;
import com.langfree.utils.LlmMessages;
import com.langfree.utils.ModelStorage;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Conversation : envoi, streaming, sauvegarde et carnet d'erreurs.
 */
public class ChatViewModel extends AndroidViewModel {
    public enum ModelPhase {
        IDLE, LOADING, READY, FAILED
    }

    public final MutableLiveData<ModelPhase> modelPhase = new MutableLiveData<>(ModelPhase.IDLE);
    public final MutableLiveData<String> modelMessage = new MutableLiveData<>();
    public final MutableLiveData<Boolean> generating = new MutableLiveData<>(false);
    public final MutableLiveData<String> streamingText = new MutableLiveData<>();
    public final MutableLiveData<UiSignal> error = new MutableLiveData<>();
    public final MutableLiveData<Integer> textSizeSp = new MutableLiveData<>(16);
    public final LiveData<List<ChatMessage>> messages;
    public final LiveData<Conversation> conversation;

    private final MutableLiveData<Long> conversationId = new MutableLiveData<>();
    private final ChatRepository repository;
    private final SettingsRepository settingsRepository;
    private final LlmManager llmManager;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicInteger generation = new AtomicInteger();
    private boolean opened;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        repository = new ChatRepository(database);
        settingsRepository = new SettingsRepository(application);
        llmManager = ((LangFreeApp) application).getLlmManager();
        messages = Transformations.switchMap(conversationId, id -> {
            if (id == null || id <= 0) {
                MutableLiveData<List<ChatMessage>> empty = new MutableLiveData<>();
                empty.setValue(Collections.emptyList());
                return empty;
            }
            return database.messageDao().observe(id);
        });
        conversation = Transformations.switchMap(conversationId, id -> {
            if (id == null || id <= 0) {
                return new MutableLiveData<>();
            }
            return database.conversationDao().observeById(id);
        });
        textSizeSp.setValue(settingsRepository.load().textSizeSp);
    }

    public Long getConversationId() {
        return conversationId.getValue();
    }

    /** Ouvre une conversation existante ou en crée une. Sans effet au second appel (rotation). */
    public void open(long id) {
        if (opened) {
            return;
        }
        opened = true;
        if (id > 0) {
            conversationId.setValue(id);
            return;
        }
        io.execute(() -> {
            long created = repository.createConversation(settingsRepository.load());
            conversationId.postValue(created);
        });
    }

    public void refreshTextSize() {
        textSizeSp.setValue(settingsRepository.load().textSizeSp);
    }

    public void ensureModelLoaded() {
        if (llmManager.isLoaded()) {
            modelPhase.setValue(ModelPhase.READY);
            return;
        }
        File model = ModelStorage.modelFile(getApplication());
        if (!ModelStorage.isReady(model)) {
            modelPhase.setValue(ModelPhase.FAILED);
            modelMessage.setValue(getApplication().getString(R.string.error_model_missing));
            return;
        }
        if (modelPhase.getValue() == ModelPhase.LOADING) {
            return;
        }
        modelPhase.setValue(ModelPhase.LOADING);
        modelMessage.setValue(getApplication().getString(R.string.model_loading));
        llmManager.load(getApplication(), model, new LlmManager.LoadListener() {
            @Override
            public void onSuccess() {
                modelPhase.setValue(ModelPhase.READY);
            }

            @Override
            public void onError(LlmManager.LlmError errorKind, String detail) {
                modelPhase.setValue(ModelPhase.FAILED);
                modelMessage.setValue(LlmMessages.toFrench(getApplication(), errorKind));
            }
        });
    }

    public void send(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (Boolean.TRUE.equals(generating.getValue())) {
            return;
        }
        Long id = conversationId.getValue();
        if (id == null || id <= 0) {
            error.setValue(new UiSignal(getApplication().getString(R.string.error_conversation)));
            return;
        }
        if (!llmManager.isLoaded()) {
            error.setValue(new UiSignal(getApplication().getString(R.string.error_model_not_loaded)));
            ensureModelLoaded();
            return;
        }
        final String userText = text.trim();
        final int token = generation.incrementAndGet();
        final long conversation = id;
        generating.setValue(true);
        streamingText.setValue(null);
        io.execute(() -> {
            try {
                UserSettings settings = settingsRepository.load();
                repository.addUserMessage(conversation, userText, settings);
                List<ChatMessage> history = repository.latestChronological(conversation, 12);
                llmManager.generateTutor(settings, history, new LlmManager.GenerateListener() {
                    @Override
                    public void onPartial(String accumulated) {
                        if (token != generation.get()) {
                            return;
                        }
                        streamingText.postValue(accumulated);
                    }

                    @Override
                    public void onComplete(String fullText) {
                        if (token != generation.get()) {
                            return;
                        }
                        TutorResponse parsed = ResponseParser.parse(fullText);
                        if (parsed.rawText.isEmpty()) {
                            parsed = TutorResponse.fallback(
                                    getApplication().getString(R.string.error_empty_reply));
                        }
                        UserSettings current = settingsRepository.load();
                        repository.addAssistantMessage(conversation, parsed, current);
                        repository.recordMistake(conversation, userText, parsed);
                        streamingText.postValue(null);
                        generating.postValue(false);
                    }

                    @Override
                    public void onError(LlmManager.LlmError errorKind, String detail) {
                        if (token != generation.get()) {
                            return;
                        }
                        if (errorKind != LlmManager.LlmError.CANCELLED) {
                            error.postValue(new UiSignal(LlmMessages.toFrench(getApplication(), errorKind)));
                        }
                        streamingText.postValue(null);
                        generating.postValue(false);
                        if (errorKind == LlmManager.LlmError.OUT_OF_MEMORY) {
                            modelPhase.postValue(ModelPhase.FAILED);
                            modelMessage.postValue(LlmMessages.toFrench(getApplication(), errorKind));
                        }
                    }
                });
            } catch (Exception exception) {
                generation.incrementAndGet();
                error.postValue(new UiSignal(getApplication().getString(R.string.error_send)));
                streamingText.postValue(null);
                generating.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        generation.incrementAndGet();
        llmManager.cancel();
        io.shutdownNow();
        super.onCleared();
    }
}
