package com.langfree.utils;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;

/**
 * Surligne, dans la phrase corrigée, les mots qui diffèrent de la phrase de l'élève.
 * Comparaison par plus longue sous-séquence commune, limitée à 80 mots.
 */
public final class TextDiff {
    private static final int MAX_WORDS = 80;

    private TextDiff() {
    }

    public static CharSequence highlight(String corrected, String original, int highlightColor) {
        if (corrected == null || corrected.isEmpty()) {
            return "";
        }
        if (original == null || original.isEmpty()) {
            return corrected;
        }
        String[] correctedWords = corrected.split("\\s+");
        String[] originalWords = original.split("\\s+");
        if (correctedWords.length > MAX_WORDS || originalWords.length > MAX_WORDS) {
            return corrected;
        }
        boolean[] changed = changedWords(correctedWords, originalWords);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (int i = 0; i < correctedWords.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            int start = builder.length();
            builder.append(correctedWords[i]);
            if (changed[i]) {
                builder.setSpan(new BackgroundColorSpan(highlightColor), start, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return builder;
    }

    private static boolean[] changedWords(String[] corrected, String[] original) {
        int rows = corrected.length;
        int columns = original.length;
        int[][] lcs = new int[rows + 1][columns + 1];
        for (int i = rows - 1; i >= 0; i--) {
            for (int j = columns - 1; j >= 0; j--) {
                if (corrected[i].equalsIgnoreCase(original[j])) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }
        boolean[] changed = new boolean[rows];
        int i = 0;
        int j = 0;
        while (i < rows && j < columns) {
            if (corrected[i].equalsIgnoreCase(original[j])) {
                changed[i] = false;
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                changed[i] = true;
                i++;
            } else {
                j++;
            }
        }
        while (i < rows) {
            changed[i] = true;
            i++;
        }
        return changed;
    }
}
