package com.langfree.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    private static PrefManager instance;
    private SharedPreferences prefs;
    
    private static final String PREF_NAME = "LangFreePrefs";
    
    // Clés
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_MODELS_INSTALLED = "models_installed";
    private static final String KEY_MODEL_PROFILE = "model_profile";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_EXPLANATION_LANGUAGE = "explanation_language";
    private static final String KEY_VOICE_SPEED = "voice_speed";
    private static final String KEY_SILENCE_DELAY = "silence_delay";
    private static final String KEY_MIC_SENSITIVITY = "mic_sensitivity";
    private static final String KEY_READ_CORRECTION = "read_correction";
    private static final String KEY_THEME = "theme";
    
    // Valeurs par défaut
    private static final String DEFAULT_LEVEL = "A2";
    private static final String DEFAULT_EXPLANATION_LANGUAGE = "fr";
    private static final float DEFAULT_VOICE_SPEED = 1.0f;
    private static final int DEFAULT_SILENCE_DELAY = 3;
    private static final int DEFAULT_MIC_SENSITIVITY = 1;
    private static final boolean DEFAULT_READ_CORRECTION = true;
    private static final String DEFAULT_THEME = "free_conversation";
    
    private PrefManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized PrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new PrefManager(context);
        }
        return instance;
    }
    
    // First launch
    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }
    
    public void setFirstLaunch(boolean firstLaunch) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, firstLaunch).apply();
    }
    
    // Models installed
    public boolean areModelsInstalled() {
        return prefs.getBoolean(KEY_MODELS_INSTALLED, false);
    }
    
    public void setModelsInstalled(boolean installed) {
        prefs.edit().putBoolean(KEY_MODELS_INSTALLED, installed).apply();
    }
    
    // Model profile
    public String getModelProfile() {
        return prefs.getString(KEY_MODEL_PROFILE, "auto");
    }
    
    public void setModelProfile(String profile) {
        prefs.edit().putString(KEY_MODEL_PROFILE, profile).apply();
    }
    
    // Level
    public String getLevel() {
        return prefs.getString(KEY_LEVEL, DEFAULT_LEVEL);
    }
    
    public void setLevel(String level) {
        prefs.edit().putString(KEY_LEVEL, level).apply();
    }
    
    // Explanation language
    public String getExplanationLanguage() {
        return prefs.getString(KEY_EXPLANATION_LANGUAGE, DEFAULT_EXPLANATION_LANGUAGE);
    }
    
    public void setExplanationLanguage(String language) {
        prefs.edit().putString(KEY_EXPLANATION_LANGUAGE, language).apply();
    }
    
    // Voice speed
    public float getVoiceSpeed() {
        return prefs.getFloat(KEY_VOICE_SPEED, DEFAULT_VOICE_SPEED);
    }
    
    public void setVoiceSpeed(float speed) {
        prefs.edit().putFloat(KEY_VOICE_SPEED, speed).apply();
    }
    
    // Silence delay
    public int getSilenceDelay() {
        return prefs.getInt(KEY_SILENCE_DELAY, DEFAULT_SILENCE_DELAY);
    }
    
    public void setSilenceDelay(int delay) {
        prefs.edit().putInt(KEY_SILENCE_DELAY, delay).apply();
    }
    
    // Mic sensitivity
    public int getMicSensitivity() {
        return prefs.getInt(KEY_MIC_SENSITIVITY, DEFAULT_MIC_SENSITIVITY);
    }
    
    public void setMicSensitivity(int sensitivity) {
        prefs.edit().putInt(KEY_MIC_SENSITIVITY, sensitivity).apply();
    }
    
    // Read correction
    public boolean shouldReadCorrection() {
        return prefs.getBoolean(KEY_READ_CORRECTION, DEFAULT_READ_CORRECTION);
    }
    
    public void setReadCorrection(boolean read) {
        prefs.edit().putBoolean(KEY_READ_CORRECTION, read).apply();
    }
    
    // Theme
    public String getTheme() {
        return prefs.getString(KEY_THEME, DEFAULT_THEME);
    }
    
    public void setTheme(String theme) {
        prefs.edit().putString(KEY_THEME, theme).apply();
    }
    
    // Clear all
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
