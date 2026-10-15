package com.langfree.utils;

import android.content.Intent;
import android.os.Build;
import android.speech.RecognitionSupport;
import android.speech.RecognitionSupportCallback;
import android.speech.SpeechRecognizer;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * Présence du pack anglais hors ligne. Cette classe n'est chargée qu'à partir d'Android 13,
 * car les types qu'elle utilise n'existent pas avant.
 */
final class SpeechPack {
    interface Host {
        void onReady();

        void onMissing();

        void onProgress(int percent);
    }

    private SpeechPack() {
    }

    static void prepare(SpeechRecognizer recognizer, Intent intent, Executor executor, Host host) {
        recognizer.checkRecognitionSupport(intent, executor, new RecognitionSupportCallback() {
            @Override
            public void onSupportResult(RecognitionSupport support) {
                if (containsEnglishUs(support.getInstalledOnDeviceLanguages())) {
                    host.onReady();
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    SpeechDownload.start(recognizer, intent, executor, host);
                    return;
                }
                host.onMissing();
            }

            @Override
            public void onError(int error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    SpeechDownload.start(recognizer, intent, executor, host);
                    return;
                }
                host.onReady();
            }
        });
    }

    static boolean containsEnglishUs(List<String> languages) {
        if (languages == null) {
            return false;
        }
        for (String language : languages) {
            if (language == null) {
                continue;
            }
            String tag = language.replace('_', '-').toLowerCase(Locale.US);
            if ("en-us".equals(tag) || tag.startsWith("en-us-")) {
                return true;
            }
        }
        return false;
    }
}
