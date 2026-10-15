package com.langfree.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Lecture en anglais (en-US) via le moteur TTS du téléphone. Hors ligne si la voix est installée.
 */
public final class SpeechOutput {
    public interface ReadyListener {
        void onReady(boolean englishAvailable);
    }

    private TextToSpeech tts;
    private boolean ready;
    private boolean englishAvailable;
    private float rate = 1.0f;

    public void init(Context context, ReadyListener listener) {
        if (tts != null) {
            listener.onReady(englishAvailable);
            return;
        }
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            ready = status == TextToSpeech.SUCCESS;
            englishAvailable = false;
            if (ready && tts != null) {
                int result = tts.setLanguage(Locale.US);
                englishAvailable = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setSpeechRate(rate);
            }
            listener.onReady(englishAvailable);
        });
    }

    public void setRate(float speechRate) {
        rate = speechRate;
        if (tts != null && ready) {
            tts.setSpeechRate(speechRate);
        }
    }

    public boolean isEnglishAvailable() {
        return englishAvailable;
    }

    public void speak(String text) {
        if (tts == null || !ready || !englishAvailable || text == null || text.trim().isEmpty()) {
            return;
        }
        tts.setSpeechRate(rate);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "langfree-reply");
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        ready = false;
        englishAvailable = false;
    }
}
