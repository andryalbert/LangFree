package com.langfree.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * Reconnaissance vocale système en anglais (États-Unis), hors ligne.
 * Si le pack n'est pas sur le téléphone, Android 14 le télécharge avant d'écouter.
 * À créer et à détruire sur le thread UI.
 */
public final class SpeechInput {
    public interface Callback {
        void onPartial(String text);

        void onResult(String text);

        void onError(int code, String message);

        /** 0 à 100 pendant le téléchargement du pack. -1 si le téléphone attend le Wi-Fi. */
        void onDownloadProgress(int percent);
    }

    private SpeechRecognizer recognizer;
    private Callback callback;
    private boolean listening;
    private boolean downloading;
    private int session;

    public boolean isListening() {
        return listening;
    }

    public boolean isBusy() {
        return listening || downloading;
    }

    public void start(Context context, Callback callback) {
        if (downloading) {
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onError(SpeechRecognizer.ERROR_CLIENT, "unavailable");
            return;
        }
        this.callback = callback;
        session++;
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        }
        int current = session;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SpeechPack.prepare(recognizer, recognizeIntent(context), mainExecutor(), new SpeechPack.Host() {
                @Override
                public void onReady() {
                    downloading = false;
                    if (current == session) {
                        listen(context);
                    }
                }

                @Override
                public void onMissing() {
                    downloading = false;
                    if (current == session && SpeechInput.this.callback != null) {
                        SpeechInput.this.callback.onError(
                                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, "missing");
                    }
                }

                @Override
                public void onProgress(int percent) {
                    downloading = percent >= 0;
                    if (current == session && SpeechInput.this.callback != null) {
                        SpeechInput.this.callback.onDownloadProgress(percent);
                    }
                }
            });
            return;
        }
        listen(context);
    }

    private void listen(Context context) {
        if (recognizer == null || callback == null) {
            return;
        }
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                listening = true;
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                listening = false;
            }

            @Override
            public void onError(int error) {
                listening = false;
                if (callback != null) {
                    callback.onError(error, "code " + error);
                }
            }

            @Override
            public void onResults(Bundle results) {
                listening = false;
                if (callback != null) {
                    callback.onResult(firstHypothesis(results));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                String text = firstHypothesis(partialResults);
                if (!text.isEmpty() && callback != null) {
                    callback.onPartial(text);
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
        listening = true;
        recognizer.startListening(recognizeIntent(context));
    }

    private static Intent recognizeIntent(Context context) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.US.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        return intent;
    }

    private static Executor mainExecutor() {
        Handler handler = new Handler(Looper.getMainLooper());
        return handler::post;
    }

    public void stop() {
        session++;
        listening = false;
        downloading = false;
        if (recognizer != null) {
            try {
                recognizer.stopListening();
            } catch (RuntimeException ignored) {
                // Déjà arrêté.
            }
        }
    }

    public void release() {
        session++;
        listening = false;
        downloading = false;
        callback = null;
        if (recognizer != null) {
            try {
                recognizer.destroy();
            } catch (RuntimeException ignored) {
                // Déjà détruit.
            }
            recognizer = null;
        }
    }

    private static String firstHypothesis(Bundle bundle) {
        if (bundle == null) {
            return "";
        }
        ArrayList<String> list = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list == null || list.isEmpty() || list.get(0) == null) {
            return "";
        }
        return list.get(0).trim();
    }
}
