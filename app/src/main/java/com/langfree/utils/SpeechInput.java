package com.langfree.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Reconnaissance vocale système, forcée hors ligne (en-US) quand le pack vocal est installé.
 * À créer et à détruire sur le thread UI.
 */
public final class SpeechInput {
    public interface Callback {
        void onPartial(String text);

        void onResult(String text);

        void onError(int code, String message);
    }

    private SpeechRecognizer recognizer;
    private boolean listening;

    public boolean isListening() {
        return listening;
    }

    public void start(Context context, Callback callback) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onError(SpeechRecognizer.ERROR_CLIENT, "unavailable");
            return;
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
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
                callback.onError(error, "code " + error);
            }

            @Override
            public void onResults(Bundle results) {
                listening = false;
                callback.onResult(firstHypothesis(results));
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                String text = firstHypothesis(partialResults);
                if (!text.isEmpty()) {
                    callback.onPartial(text);
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.US.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        listening = true;
        recognizer.startListening(intent);
    }

    public void stop() {
        listening = false;
        if (recognizer != null) {
            try {
                recognizer.stopListening();
            } catch (RuntimeException ignored) {
                // Déjà arrêté.
            }
        }
    }

    public void release() {
        listening = false;
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
