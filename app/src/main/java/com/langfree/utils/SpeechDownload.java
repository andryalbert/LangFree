package com.langfree.utils;

import android.content.Intent;
import android.speech.ModelDownloadListener;
import android.speech.SpeechRecognizer;

import java.util.concurrent.Executor;

/**
 * Téléchargement du pack vocal par le service du téléphone. Android 14 seulement.
 */
final class SpeechDownload {
    private SpeechDownload() {
    }

    static void start(SpeechRecognizer recognizer, Intent intent, Executor executor, SpeechPack.Host host) {
        host.onProgress(0);
        recognizer.triggerModelDownload(intent, executor, new ModelDownloadListener() {
            @Override
            public void onProgress(int completedPercent) {
                host.onProgress(completedPercent);
            }

            @Override
            public void onSuccess() {
                host.onReady();
            }

            @Override
            public void onScheduled() {
                host.onProgress(-1);
            }

            @Override
            public void onError(int error) {
                host.onMissing();
            }
        });
    }
}
