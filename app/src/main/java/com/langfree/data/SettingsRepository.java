package com.langfree.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.langfree.model.CorrectionMode;
import com.langfree.model.Level;
import com.langfree.model.Topic;
import com.langfree.model.UserSettings;

/**
 * Préférences locales : niveau, thème, voix, taille du texte, mode nuit, jeton Hugging Face.
 * Le jeton ne sert qu'au premier téléchargement du modèle et reste dans l'espace privé de l'app.
 */
public class SettingsRepository {
    private static final String PREFS = "langfree_settings";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_TOPIC = "topic";
    private static final String KEY_MODE = "correction_mode";
    private static final String KEY_RATE = "speech_rate";
    private static final String KEY_TEXT = "text_size_sp";
    private static final String KEY_NIGHT = "night_mode";
    private static final String KEY_TOKEN = "hf_token";

    private final SharedPreferences preferences;

    public SettingsRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public UserSettings load() {
        UserSettings settings = new UserSettings();
        settings.level = Level.fromName(preferences.getString(KEY_LEVEL, Level.A1_A2.name()));
        settings.topic = Topic.fromName(preferences.getString(KEY_TOPIC, Topic.FREE.name()));
        settings.correctionMode = CorrectionMode.fromName(
                preferences.getString(KEY_MODE, CorrectionMode.GENTLE.name()));
        settings.speechRate = preferences.getFloat(KEY_RATE, 1.0f);
        settings.textSizeSp = preferences.getInt(KEY_TEXT, 16);
        settings.nightMode = preferences.getInt(KEY_NIGHT, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (settings.speechRate < 0.7f || settings.speechRate > 1.4f) {
            settings.speechRate = 1.0f;
        }
        if (settings.textSizeSp < 14 || settings.textSizeSp > 22) {
            settings.textSizeSp = 16;
        }
        return settings;
    }

    public void save(UserSettings settings) {
        preferences.edit()
                .putString(KEY_LEVEL, settings.level.name())
                .putString(KEY_TOPIC, settings.topic.name())
                .putString(KEY_MODE, settings.correctionMode.name())
                .putFloat(KEY_RATE, settings.speechRate)
                .putInt(KEY_TEXT, settings.textSizeSp)
                .putInt(KEY_NIGHT, settings.nightMode)
                .apply();
    }

    public String getHfToken() {
        return preferences.getString(KEY_TOKEN, "");
    }

    public void setHfToken(String token) {
        String value = token == null ? "" : token.trim();
        preferences.edit().putString(KEY_TOKEN, value).apply();
    }
}
