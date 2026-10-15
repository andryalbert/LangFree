package com.langfree.model;

import java.util.Locale;

/**
 * Réponse découpée du tuteur. Si {@link #fallback} est vrai, seul {@link #rawText} est affichable.
 */
public class TutorResponse {
    public final String correction;
    public final String explanation;
    public final String reply;
    public final String rawText;
    public final boolean fallback;

    public TutorResponse(String correction, String explanation, String reply,
                          String rawText, boolean fallback) {
        this.correction = correction == null ? "" : correction;
        this.explanation = explanation == null ? "" : explanation;
        this.reply = reply == null ? "" : reply;
        this.rawText = rawText == null ? "" : rawText;
        this.fallback = fallback;
    }

    public static TutorResponse fallback(String rawText) {
        String raw = rawText == null ? "" : rawText.trim();
        return new TutorResponse("", "", raw, raw, true);
    }

    public boolean isPerfect() {
        return looksPerfect(correction);
    }

    /** Reconnaît la convention « Perfect! » demandée dans le prompt, et quelques variantes. */
    public static boolean looksPerfect(String correction) {
        if (correction == null) {
            return false;
        }
        String normalized = correction.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("perfect")
                || normalized.equals("perfect!")
                || normalized.equals("perfect.")
                || normalized.startsWith("perfect!")
                || normalized.startsWith("perfect.");
    }
}
