package com.langfree.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.langfree.LangFreeApp;
import com.langfree.R;
import com.langfree.ai.LlmManager;
import com.langfree.ai.PromptBuilder;
import com.langfree.ai.QuizParser;
import com.langfree.data.AppDatabase;
import com.langfree.data.QuizRepository;
import com.langfree.data.SettingsRepository;
import com.langfree.model.UiSignal;
import com.langfree.model.UserSettings;
import com.langfree.utils.LlmMessages;
import com.langfree.utils.ModelStorage;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Quiz oral : une question au hasard, puis la correction et le niveau estimé. */
public class QuizViewModel extends AndroidViewModel {
    public enum Phase {
        LOADING, ASKING, ANSWERING, GRADING, RESULT, FAILED
    }

    private static final String[] TOPICS = {
            "a normal weekday",
            "their job or studies",
            "a recent trip",
            "food they enjoy",
            "plans for next month",
            "a problem they solved",
            "their hometown",
            "shopping",
            "sport or health",
            "a film, book or song",
            "working with other people",
            "why they are learning English"
    };

    public final MutableLiveData<Phase> phase = new MutableLiveData<>(Phase.LOADING);
    public final MutableLiveData<String> question = new MutableLiveData<>();
    public final MutableLiveData<String> correction = new MutableLiveData<>();
    public final MutableLiveData<String> explanation = new MutableLiveData<>();
    public final MutableLiveData<String> levelLabel = new MutableLiveData<>();
    public final MutableLiveData<String> status = new MutableLiveData<>();
    public final MutableLiveData<UiSignal> error = new MutableLiveData<>();

    private final QuizRepository repository;
    private final SettingsRepository settingsRepository;
    private final LlmManager llmManager;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicInteger generation = new AtomicInteger();
    private final Random random = new Random();
    private long sessionId;
    private long turnId;
    private String lastTopic = "";
    private boolean started;

    public QuizViewModel(@NonNull Application application) {
        super(application);
        repository = new QuizRepository(AppDatabase.getInstance(application));
        settingsRepository = new SettingsRepository(application);
        llmManager = ((LangFreeApp) application).getLlmManager();
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        status.setValue(getApplication().getString(R.string.quiz_loading));
        io.execute(() -> {
            sessionId = repository.createSession();
            ensureModelLoaded();
        });
    }

    public void submit(String text) {
        if (phase.getValue() != Phase.ANSWERING || text == null || text.trim().isEmpty()) {
            return;
        }
        if (sessionId <= 0 || turnId <= 0) {
            return;
        }
        final String answer = text.trim();
        final String asked = question.getValue() == null ? "" : question.getValue();
        final int token = generation.incrementAndGet();
        phase.setValue(Phase.GRADING);
        status.setValue(getApplication().getString(R.string.quiz_grading));
        io.execute(() -> {
            UserSettings settings = settingsRepository.load();
            String prompt = PromptBuilder.quizGradePrompt(settings.level.promptValue, asked, answer);
            llmManager.generateRaw(prompt, new LlmManager.GenerateListener() {
                @Override
                public void onPartial(String accumulated) {
                    if (token == generation.get()) {
                        status.postValue(getApplication().getString(R.string.quiz_grading));
                    }
                }

                @Override
                public void onComplete(String fullText) {
                    if (token != generation.get()) {
                        return;
                    }
                    QuizParser.Grade grade = QuizParser.grade(fullText);
                    String correctionText = grade.correction.isEmpty()
                            ? getApplication().getString(R.string.quiz_no_correction)
                            : grade.correction;
                    repository.saveGrade(sessionId, turnId, answer, correctionText,
                            grade.explanation, grade.level);
                    correction.postValue(correctionText);
                    explanation.postValue(grade.explanation);
                    levelLabel.postValue(QuizParser.levelLabel(grade.level));
                    status.postValue("");
                    phase.postValue(Phase.RESULT);
                }

                @Override
                public void onError(LlmManager.LlmError errorKind, String detail) {
                    if (token != generation.get() || errorKind == LlmManager.LlmError.CANCELLED) {
                        return;
                    }
                    error.postValue(new UiSignal(LlmMessages.toFrench(getApplication(), errorKind)));
                    status.postValue("");
                    phase.postValue(Phase.ANSWERING);
                }
            });
        });
    }

    public void nextQuestion() {
        if (phase.getValue() == Phase.ASKING || phase.getValue() == Phase.GRADING) {
            return;
        }
        ask();
    }

    public void retry() {
        if (phase.getValue() == Phase.ASKING || phase.getValue() == Phase.GRADING) {
            return;
        }
        if (llmManager.isLoaded()) {
            ask();
            return;
        }
        ensureModelLoaded();
    }

    private void ensureModelLoaded() {
        if (llmManager.isLoaded()) {
            ask();
            return;
        }
        File model = ModelStorage.modelFile(getApplication());
        if (!ModelStorage.isReady(model)) {
            phase.postValue(Phase.FAILED);
            status.postValue(getApplication().getString(R.string.error_model_missing));
            return;
        }
        llmManager.load(getApplication(), model, new LlmManager.LoadListener() {
            @Override
            public void onSuccess() {
                ask();
            }

            @Override
            public void onError(LlmManager.LlmError errorKind, String detail) {
                phase.postValue(Phase.FAILED);
                status.postValue(LlmMessages.toFrench(getApplication(), errorKind));
            }
        });
    }

    private void ask() {
        if (sessionId <= 0) {
            return;
        }
        final int token = generation.incrementAndGet();
        phase.postValue(Phase.ASKING);
        question.postValue(null);
        correction.postValue(null);
        explanation.postValue(null);
        levelLabel.postValue(null);
        status.postValue(getApplication().getString(R.string.quiz_asking));
        io.execute(() -> {
            UserSettings settings = settingsRepository.load();
            List<String> previous = repository.recentQuestions(sessionId);
            String prompt = PromptBuilder.quizQuestionPrompt(
                    settings.level.promptValue, nextTopic(), previous);
            llmManager.generateRaw(prompt, new LlmManager.GenerateListener() {
                @Override
                public void onPartial(String accumulated) {
                    if (token == generation.get()) {
                        String partial = QuizParser.question(accumulated);
                        if (!partial.isEmpty()) {
                            question.postValue(partial);
                        }
                    }
                }

                @Override
                public void onComplete(String fullText) {
                    if (token != generation.get()) {
                        return;
                    }
                    String asked = QuizParser.question(fullText);
                    if (asked.isEmpty()) {
                        asked = getApplication().getString(R.string.quiz_question_failed);
                    }
                    turnId = repository.addQuestion(sessionId, asked);
                    question.postValue(asked);
                    status.postValue("");
                    phase.postValue(Phase.ANSWERING);
                }

                @Override
                public void onError(LlmManager.LlmError errorKind, String detail) {
                    if (token != generation.get() || errorKind == LlmManager.LlmError.CANCELLED) {
                        return;
                    }
                    error.postValue(new UiSignal(LlmMessages.toFrench(getApplication(), errorKind)));
                    phase.postValue(Phase.FAILED);
                    status.postValue(LlmMessages.toFrench(getApplication(), errorKind));
                }
            });
        });
    }

    private String nextTopic() {
        String topic = TOPICS[random.nextInt(TOPICS.length)];
        if (topic.equals(lastTopic)) {
            topic = TOPICS[(random.nextInt(TOPICS.length - 1) + indexOf(topic) + 1) % TOPICS.length];
        }
        lastTopic = topic;
        return topic;
    }

    private int indexOf(String topic) {
        for (int i = 0; i < TOPICS.length; i++) {
            if (TOPICS[i].equals(topic)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void onCleared() {
        generation.incrementAndGet();
        llmManager.cancel();
        io.shutdownNow();
        super.onCleared();
    }
}
