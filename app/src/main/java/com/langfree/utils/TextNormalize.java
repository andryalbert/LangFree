package com.langfree.utils;

import java.util.Locale;

/**
 * Normalisation de texte pour comparer une correction et fabriquer la signature d'une erreur.
 */
public final class TextNormalize {
    private TextNormalize() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT).trim();
        StringBuilder builder = new StringBuilder(lower.length());
        boolean pendingSpace = false;
        for (int i = 0; i < lower.length(); i++) {
            char character = lower.charAt(i);
            if (Character.isLetterOrDigit(character) || character == '\'') {
                if (pendingSpace && builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(character);
                pendingSpace = false;
            } else if (Character.isWhitespace(character)) {
                pendingSpace = true;
            }
        }
        return builder.toString().trim();
    }

    public static boolean same(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    public static String signature(String explanation, String original, String corrected) {
        String fromExplanation = normalize(explanation);
        String signature = fromExplanation.length() >= 12
                ? fromExplanation
                : normalize(original) + "->" + normalize(corrected);
        if (signature.length() > 180) {
            return signature.substring(0, 180);
        }
        return signature;
    }
}
