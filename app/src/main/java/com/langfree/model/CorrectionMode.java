package com.langfree.model;

/**
 * Stricte : chaque faute est signalée. Douce : seulement les fautes importantes.
 */
public enum CorrectionMode {
    STRICT("Stricte"),
    GENTLE("Douce");

    public final String labelFr;

    CorrectionMode(String labelFr) {
        this.labelFr = labelFr;
    }

    public static CorrectionMode fromName(String name) {
        if (name == null) {
            return GENTLE;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return GENTLE;
        }
    }
}
