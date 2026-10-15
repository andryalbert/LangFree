package com.langfree.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.mediapipe.tasks.genai.llminference.LlmInference;
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession;
import com.google.mediapipe.tasks.genai.llminference.ProgressListener;
import com.langfree.model.ChatMessage;
import com.langfree.model.UserSettings;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Chargement du modèle Gemma, génération en streaming et libération de la mémoire.
 * Tout le travail natif passe par un seul thread : jamais le thread UI.
 *
 * Les callbacks {@link GenerateListener#onPartial} arrivent sur le thread natif de MediaPipe.
 * {@link GenerateListener#onComplete} et {@link GenerateListener#onError} arrivent sur le thread du moteur.
 * {@link LoadListener} est rappelé sur le thread UI.
 */
@SuppressWarnings("deprecation")
public final class LlmManager {
    private static final String TAG = "LlmManager";
    /** Taille du cache clé-valeur : entrée + sortie. Valeur utilisée par l'exemple officiel Gemma 3. */
    private static final int MAX_TOKENS = 1024;
    private static final int MAX_OUTPUT_CHARS = 1800;
    private static final long GENERATION_TIMEOUT_SECONDS = 120L;

    public enum LlmError {
        NOT_LOADED,
        OUT_OF_MEMORY,
        MISSING_MODEL,
        UNSUPPORTED_DEVICE,
        TIMEOUT,
        CANCELLED,
        FAILED
    }

    public interface LoadListener {
        void onSuccess();

        void onError(LlmError error, String detail);
    }

    public interface GenerateListener {
        void onPartial(String accumulated);

        void onComplete(String fullText);

        void onError(LlmError error, String detail);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "langfree-llm");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private LlmInference llm;
    private volatile boolean loaded;
    private volatile boolean loading;
    private volatile LlmInferenceSession activeSession;
    private volatile CountDownLatch activeLatch;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public boolean isLoaded() {
        return loaded && llm != null;
    }

    public void load(Context context, File modelFile, LoadListener listener) {
        Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            if (loaded && llm != null) {
                mainHandler.post(listener::onSuccess);
                return;
            }
            if (modelFile == null || !modelFile.isFile() || modelFile.length() < 1024) {
                mainHandler.post(() -> listener.onError(LlmError.MISSING_MODEL, "fichier absent"));
                return;
            }
            loading = true;
            try {
                releaseOnWorker();
                Log.i(TAG, "Chargement du modèle (" + (modelFile.length() / (1024 * 1024)) + " Mo)");
                LlmInference.LlmInferenceOptions options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelFile.getAbsolutePath())
                        .setMaxTokens(MAX_TOKENS)
                        .setMaxTopK(64)
                        .setPreferredBackend(LlmInference.Backend.CPU)
                        .build();
                llm = LlmInference.createFromOptions(appContext, options);
                loaded = true;
                mainHandler.post(listener::onSuccess);
            } catch (OutOfMemoryError error) {
                Log.e(TAG, "Mémoire insuffisante", error);
                releaseOnWorker();
                mainHandler.post(() -> listener.onError(LlmError.OUT_OF_MEMORY, error.getMessage()));
            } catch (UnsatisfiedLinkError error) {
                Log.e(TAG, "Bibliothèque native absente", error);
                releaseOnWorker();
                mainHandler.post(() -> listener.onError(LlmError.UNSUPPORTED_DEVICE, error.getMessage()));
            } catch (Throwable error) {
                Log.e(TAG, "Échec du chargement", error);
                releaseOnWorker();
                LlmError kind = classify(error);
                mainHandler.post(() -> listener.onError(kind, safeMessage(error)));
            } finally {
                loading = false;
            }
        });
    }

    /**
     * Génère une réponse de tuteur. Le prompt est construit ici pour compter les tokens
     * sur le même thread que le moteur.
     */
    public void generateTutor(UserSettings settings, List<ChatMessage> history, GenerateListener listener) {
        executor.execute(() -> generateOnWorker(settings, history, null, listener));
    }

    /** Génère un texte libre (exercice de révision) à partir d'un prompt déjà prêt. */
    public void generateRaw(String prompt, GenerateListener listener) {
        executor.execute(() -> generateOnWorker(null, null, prompt, listener));
    }

    public void cancel() {
        cancelRequested.set(true);
        LlmInferenceSession session = activeSession;
        if (session != null) {
            try {
                session.cancelGenerateResponseAsync();
            } catch (Throwable ignored) {
                // L'annulation peut échouer si la génération vient de se terminer.
            }
        }
        CountDownLatch latch = activeLatch;
        if (latch != null) {
            latch.countDown();
        }
    }

    /** Ferme le modèle et rend la RAM. Peut être appelé depuis n'importe quel thread. */
    public void release() {
        // Débloque une génération en cours avant de fermer, sinon le thread unique attendrait le délai.
        cancel();
        executor.execute(this::releaseOnWorker);
    }

    /** Libère le modèle puis exécute {@code after} sur le thread UI. */
    public void releaseThen(Runnable after) {
        cancel();
        executor.execute(() -> {
            releaseOnWorker();
            if (after != null) {
                mainHandler.post(after);
            }
        });
    }

    private void generateOnWorker(UserSettings settings, List<ChatMessage> history,
                                  String readyPrompt, GenerateListener listener) {
        if (llm == null || !loaded) {
            listener.onError(LlmError.NOT_LOADED, "modèle non chargé");
            return;
        }
        cancelRequested.set(false);
        LlmInferenceSession session = null;
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder accumulated = new StringBuilder();
        AtomicBoolean finished = new AtomicBoolean(false);
        try {
            String prompt = readyPrompt != null
                    ? readyPrompt
                    : PromptBuilder.build(settings, history, this::countTokens);
            LlmInferenceSession.LlmInferenceSessionOptions sessionOptions =
                    LlmInferenceSession.LlmInferenceSessionOptions.builder()
                            .setTemperature(0.6f)
                            .setTopK(40)
                            .setTopP(0.95f)
                            .setRandomSeed(randomSeed())
                            .build();
            session = LlmInferenceSession.createFromOptions(llm, sessionOptions);
            activeSession = session;
            activeLatch = latch;
            session.addQueryChunk(prompt);
            final LlmInferenceSession sessionRef = session;
            session.generateResponseAsync(new ProgressListener<String>() {
                @Override
                public void run(String partialResult, boolean done) {
                    if (finished.get()) {
                        return;
                    }
                    if (partialResult != null && !partialResult.isEmpty()) {
                        accumulated.append(partialResult);
                    }
                    String snapshot = accumulated.toString();
                    try {
                        listener.onPartial(snapshot);
                    } catch (Throwable callbackError) {
                        Log.w(TAG, "Callback partiel ignoré", callbackError);
                    }
                    if (!done && snapshot.length() > MAX_OUTPUT_CHARS) {
                        try {
                            sessionRef.cancelGenerateResponseAsync();
                        } catch (Throwable ignored) {
                            finished.set(true);
                            latch.countDown();
                        }
                    }
                    if (done) {
                        finished.set(true);
                        latch.countDown();
                    }
                }
            });
            boolean completed = latch.await(GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (cancelRequested.get()) {
                listener.onError(LlmError.CANCELLED, "annulé");
                return;
            }
            if (!completed) {
                try {
                    session.cancelGenerateResponseAsync();
                } catch (Throwable ignored) {
                    // On ferme la session dans finally.
                }
                listener.onError(LlmError.TIMEOUT, "délai dépassé");
                return;
            }
            String full = accumulated.toString().trim();
            if (full.isEmpty()) {
                listener.onError(LlmError.FAILED, "réponse vide");
            } else {
                listener.onComplete(full);
            }
        } catch (OutOfMemoryError error) {
            Log.e(TAG, "Mémoire insuffisante pendant la génération", error);
            listener.onError(LlmError.OUT_OF_MEMORY, safeMessage(error));
        } catch (Throwable error) {
            Log.e(TAG, "Génération interrompue", error);
            if (cancelRequested.get()) {
                listener.onError(LlmError.CANCELLED, safeMessage(error));
            } else {
                listener.onError(classify(error), safeMessage(error));
            }
        } finally {
            activeLatch = null;
            activeSession = null;
            if (session != null) {
                try {
                    session.close();
                } catch (Throwable closeError) {
                    Log.w(TAG, "Fermeture de session", closeError);
                }
            }
        }
    }

    private int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            if (llm == null) {
                return approximateTokens(text);
            }
            return llm.sizeInTokens(text);
        } catch (Throwable error) {
            Log.w(TAG, "Comptage de tokens indisponible, estimation utilisée", error);
            return approximateTokens(text);
        }
    }

    private static int approximateTokens(String text) {
        return Math.max(1, text.length() / 3);
    }

    private static int randomSeed() {
        int seed = (int) (System.nanoTime() & 0x7fffffff);
        return seed == 0 ? 1 : seed;
    }

    private void releaseOnWorker() {
        cancelRequested.set(true);
        LlmInferenceSession session = activeSession;
        activeSession = null;
        if (session != null) {
            try {
                session.cancelGenerateResponseAsync();
            } catch (Throwable ignored) {
                // Déjà terminée.
            }
            try {
                session.close();
            } catch (Throwable ignored) {
                // Déjà fermée.
            }
        }
        CountDownLatch latch = activeLatch;
        if (latch != null) {
            latch.countDown();
        }
        if (llm != null) {
            try {
                llm.close();
            } catch (Throwable error) {
                Log.w(TAG, "Fermeture du modèle", error);
            }
            llm = null;
        }
        loaded = false;
        Log.i(TAG, "Mémoire du modèle libérée");
    }

    private static LlmError classify(Throwable error) {
        String message = safeMessage(error).toLowerCase(java.util.Locale.ROOT);
        if (error instanceof OutOfMemoryError || message.contains("memory") || message.contains("oom")) {
            return LlmError.OUT_OF_MEMORY;
        }
        if (error instanceof UnsatisfiedLinkError || message.contains("unsatisfiedlink")) {
            return LlmError.UNSUPPORTED_DEVICE;
        }
        if (message.contains("no such file") || message.contains("not found") || message.contains("failed to open")) {
            return LlmError.MISSING_MODEL;
        }
        return LlmError.FAILED;
    }

    private static String safeMessage(Throwable error) {
        if (error == null || error.getMessage() == null) {
            return "";
        }
        return error.getMessage();
    }

    boolean isLoading() {
        return loading;
    }
}
