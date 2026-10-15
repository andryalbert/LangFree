package com.langfree.viewmodel;

import android.app.ActivityManager;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.langfree.LangFreeApp;
import com.langfree.R;
import com.langfree.ai.LlmManager;
import com.langfree.ai.PromptBuilder;
import com.langfree.data.AppDatabase;
import com.langfree.data.ChatRepository;
import com.langfree.model.Mistake;
import com.langfree.model.QuizItem;
import com.langfree.model.UiSignal;
import com.langfree.utils.LlmMessages;
import com.langfree.utils.ModelStorage;
import com.langfree.utils.TextNormalize;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Mini-exercice : quiz immédiat à partir des erreurs enregistrées,
 * plus un texte rédigé par le modèle local s'il peut être chargé.
 */
public class ReviewViewModel extends AndroidViewModel {
    public final MutableLiveData<List<QuizItem>> quiz = new MutableLiveData<>();
    public final MutableLiveData<Boolean> quizReady = new MutableLiveData<>(false);
    public final MutableLiveData<String> aiExercise = new MutableLiveData<>();
    public final MutableLiveData<Boolean> generating = new MutableLiveData<>(false);
    public final MutableLiveData<UiSignal> message = new MutableLiveData<>();

    private final ChatRepository repository;
    private final LlmManager llmManager;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean started;

    public ReviewViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(AppDatabase.getInstance(application));
        llmManager = ((LangFreeApp) application).getLlmManager();
    }

    public void load() {
        if (started) {
            return;
        }
        started = true;
        io.execute(() -> {
            List<Mistake> mistakes = repository.topMistakes(5);
            ArrayList<QuizItem> items = new ArrayList<>();
            for (Mistake mistake : mistakes) {
                items.add(new QuizItem(mistake.originalText, mistake.correctedText, mistake.explanation));
            }
            quiz.postValue(items);
            quizReady.postValue(true);
            if (!items.isEmpty()) {
                requestAiExercise(mistakes);
            }
        });
    }

    public boolean check(QuizItem item, String attempt) {
        item.attempt = attempt == null ? "" : attempt;
        boolean ok = TextNormalize.same(item.attempt, item.answer);
        item.correct = ok;
        if (ok) {
            item.feedback = getApplication().getString(R.string.quiz_correct);
        } else {
            item.feedback = getApplication().getString(R.string.quiz_expected, item.answer, item.explanation);
        }
        return ok;
    }

    private void requestAiExercise(List<Mistake> mistakes) {
        if (!ModelStorage.isReady(getApplication()) && !llmManager.isLoaded()) {
            message.postValue(new UiSignal(getApplication().getString(R.string.review_model_missing)));
            return;
        }
        ActivityManager manager = (ActivityManager) getApplication().getSystemService(Application.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        if (manager != null) {
            manager.getMemoryInfo(info);
        }
        if (!llmManager.isLoaded() && info.availMem > 0 && info.availMem < 1100L * 1024L * 1024L) {
            message.postValue(new UiSignal(getApplication().getString(R.string.review_low_memory)));
            return;
        }
        generating.postValue(true);
        aiExercise.postValue("");
        Runnable generate = () -> {
            StringBuilder block = new StringBuilder();
            for (Mistake mistake : mistakes) {
                block.append("- ").append(mistake.originalText)
                        .append(" -> ").append(mistake.correctedText)
                        .append(" (").append(mistake.explanation).append(")\n");
            }
            String prompt = PromptBuilder.exercisePrompt(block.toString());
            llmManager.generateRaw(prompt, new LlmManager.GenerateListener() {
                @Override
                public void onPartial(String accumulated) {
                    aiExercise.postValue(accumulated);
                }

                @Override
                public void onComplete(String fullText) {
                    aiExercise.postValue(fullText);
                    generating.postValue(false);
                }

                @Override
                public void onError(LlmManager.LlmError error, String detail) {
                    generating.postValue(false);
                    if (error != LlmManager.LlmError.CANCELLED) {
                        message.postValue(new UiSignal(LlmMessages.toFrench(getApplication(), error)));
                    }
                }
            });
        };
        if (llmManager.isLoaded()) {
            generate.run();
            return;
        }
        File model = ModelStorage.modelFile(getApplication());
        llmManager.load(getApplication(), model, new LlmManager.LoadListener() {
            @Override
            public void onSuccess() {
                io.execute(generate);
            }

            @Override
            public void onError(LlmManager.LlmError error, String detail) {
                generating.postValue(false);
                message.postValue(new UiSignal(LlmMessages.toFrench(getApplication(), error)));
            }
        });
    }

    @Override
    protected void onCleared() {
        llmManager.cancel();
        io.shutdownNow();
        super.onCleared();
    }
}
