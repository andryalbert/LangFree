package com.langfree.model;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Réglages locaux (préférences). Ils ne transitent pas par Room.
 */
public class UserSettings {
    public Level level = Level.A1_A2;
    public Topic topic = Topic.FREE;
    public CorrectionMode correctionMode = CorrectionMode.GENTLE;
    public float speechRate = 1.0f;
    public int textSizeSp = 16;
    public int nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    public UserSettings copy() {
        UserSettings clone = new UserSettings();
        clone.level = level;
        clone.topic = topic;
        clone.correctionMode = correctionMode;
        clone.speechRate = speechRate;
        clone.textSizeSp = textSizeSp;
        clone.nightMode = nightMode;
        return clone;
    }
}
