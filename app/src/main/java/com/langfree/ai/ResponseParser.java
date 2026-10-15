package com.langfree.ai;

import com.langfree.model.TutorResponse;

import java.util.Locale;

/**
 * Découpe la sortie du modèle sur les balises [CORRECTION], [EXPLICATION] et [REPONSE].
 * Si le format n'est pas respecté, toute la sortie part dans une bulle simple.
 */
public final class ResponseParser {
    private ResponseParser() {
    }

    public static TutorResponse parse(String raw) {
        if (raw == null) {
            return TutorResponse.fallback("");
        }
        String text = raw.replace("\r\n", "\n").trim();
        if (text.isEmpty()) {
            return TutorResponse.fallback("");
        }

        int[] correction = findTag(text, "CORRECTION");
        int[] explanation = findTag(text, "EXPLICATION", "EXPLANATION");
        int[] reply = findTag(text, "REPONSE", "RÉPONSE", "RESPONSE");

        if (correction[0] < 0 || explanation[0] <= correction[0] || reply[0] <= explanation[0]) {
            return TutorResponse.fallback(text);
        }

        String corrected = clean(text.substring(correction[0] + correction[1], explanation[0]));
        String explained = clean(text.substring(explanation[0] + explanation[1], reply[0]));
        String answer = clean(text.substring(reply[0] + reply[1]));

        int student = answer.indexOf("\nStudent:");
        if (student > 0) {
            answer = answer.substring(0, student).trim();
        }
        int tutorAgain = indexOfIgnoreCase(answer, "\n[CORRECTION]");
        if (tutorAgain > 0) {
            answer = answer.substring(0, tutorAgain).trim();
        }

        if (corrected.isEmpty() || answer.isEmpty()) {
            return TutorResponse.fallback(text);
        }
        return new TutorResponse(corrected, explained, answer, text, false);
    }

    /** Retourne {index, longueur} de la première balise trouvée, ou {-1, 0}. */
    private static int[] findTag(String text, String... tags) {
        String upper = text.toUpperCase(Locale.ROOT);
        int bestIndex = -1;
        int bestLength = 0;
        for (String tag : tags) {
            String needle = "[" + tag.toUpperCase(Locale.ROOT) + "]";
            int index = upper.indexOf(needle);
            if (index >= 0 && (bestIndex < 0 || index < bestIndex)) {
                bestIndex = index;
                bestLength = needle.length();
            }
        }
        return new int[]{bestIndex, bestLength};
    }

    private static int indexOfIgnoreCase(String text, String needle) {
        return text.toUpperCase(Locale.ROOT).indexOf(needle.toUpperCase(Locale.ROOT));
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith(":") || trimmed.startsWith("-")) {
            trimmed = trimmed.substring(1).trim();
        }
        // Le modèle entoure parfois la phrase de guillemets.
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '«' && last == '»')) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }
}
