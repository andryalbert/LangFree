package com.langfree;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.langfree.ai.LlmManager;
import com.langfree.data.SettingsRepository;

/**
 * Point d'entrée. Le moteur IA est partagé par les écrans et libéré si la RAM devient critique.
 */
public class LangFreeApp extends Application {
    private LlmManager llmManager;

    @Override
    public void onCreate() {
        super.onCreate();
        SettingsRepository settingsRepository = new SettingsRepository(this);
        AppCompatDelegate.setDefaultNightMode(settingsRepository.load().nightMode);
        llmManager = new LlmManager();
        registerComponentCallbacks(new ComponentCallbacks2() {
            @Override
            public void onTrimMemory(int level) {
                // On ne libère pas quand l'app passe simplement en arrière-plan (UI_HIDDEN),
                // sinon chaque retour sur l'écran rechargerait le modèle.
                if (level == TRIM_MEMORY_RUNNING_CRITICAL
                        || level == TRIM_MEMORY_MODERATE
                        || level >= TRIM_MEMORY_COMPLETE) {
                    llmManager.release();
                }
            }

            @Override
            public void onConfigurationChanged(@NonNull Configuration newConfig) {
            }

            @Override
            public void onLowMemory() {
                llmManager.release();
            }
        });
    }

    public LlmManager getLlmManager() {
        return llmManager;
    }
}
