package com.langfree.ai;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lit la question et la correction produites par le modèle. */
public final class QuizParser {
    private static final Pattern LEVEL = Pattern.compile("\\b(A1|A2|B1|B2|C1|C2)\\b");

    private QuizParser() {
    }

    public static String question(String raw) {
        String text = strip(raw);
        String body = after(text, "QUESTION");
        if (body.isEmpty()) {
            body = text;
        }
        int cut = indexOfTag(body);
        if (cut > 0) {
            body = body.substring(0, cut);
        }
        return oneBlock(body);
    }

    public static Grade grade(String raw) {
        String text = strip(raw);
        String correction = between(text, "CORRECTION", "EXPLICATION", "EXPLANATION");
        String explanation = between(text, "EXPLICATION", "NIVEAU", "LEVEL", "NIVEAU CECRL");
        if (explanation.isEmpty()) {
            explanation = between(text, "EXPLANATION", "NIVEAU", "LEVEL");
        }
        String levelBlock = after(text, "NIVEAU");
        if (levelBlock.isEmpty()) {
            levelBlock = after(text, "LEVEL");
        }
        String level = levelCode(levelBlock.isEmpty() ? text : levelBlock);
        if (correction.isEmpty() && explanation.isEmpty()) {
            return new Grade(text, "", level);
        }
        return new Grade(correction, explanation, level);
    }

    public static String levelLabel(String code) {
        if (code == null) {
            return "";
        }
        switch (code) {
            case "A1":
                return "A1 · Débutant";
            case "A2":
                return "A2 · Élémentaire";
            case "B1":
                return "B1 · Intermédiaire";
            case "B2":
                return "B2 · Intermédiaire avancé";
            case "C1":
                return "C1 · Avancé";
            case "C2":
                return "C2 · Maîtrise";
            default:
                return code;
        }
    }

    private static String levelCode(String text) {
        Matcher matcher = LEVEL.matcher(text.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static String between(String text, String startTag, String... endTags) {
        int[] start = findTag(text, startTag);
        if (start[0] < 0) {
            return "";
        }
        int from = start[0] + start[1];
        int end = text.length();
        for (String tag : endTags) {
            int[] found = findTag(text, tag);
            if (found[0] > from && found[0] < end) {
                end = found[0];
            }
        }
        return oneBlock(text.substring(from, end));
    }

    private static String after(String text, String tag) {
        int[] start = findTag(text, tag);
        if (start[0] < 0) {
            return "";
        }
        return text.substring(start[0] + start[1]).trim();
    }

    private static int indexOfTag(String text) {
        int best = -1;
        String upper = text.toUpperCase(Locale.ROOT);
        for (String tag : new String[]{"CORRECTION", "EXPLICATION", "EXPLANATION", "NIVEAU", "QUESTION"}) {
            int index = upper.indexOf("[" + tag + "]");
            if (index >= 0 && (best < 0 || index < best)) {
                best = index;
            }
        }
        return best;
    }

    private static int[] findTag(String text, String tag) {
        String needle = "[" + tag.toUpperCase(Locale.ROOT) + "]";
        int index = text.toUpperCase(Locale.ROOT).indexOf(needle);
        if (index < 0) {
            return new int[]{-1, 0};
        }
        return new int[]{index, needle.length()};
    }

    private static String strip(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\r\n", "\n").trim();
    }

    private static String oneBlock(String value) {
        String trimmed = value == null ? "" : value.trim();
        int student = trimmed.indexOf("\nStudent:");
        if (student > 0) {
            trimmed = trimmed.substring(0, student).trim();
        }
        if (trimmed.startsWith(":") || trimmed.startsWith("-")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed;
    }

    public static final class Grade {
        public final String correction;
        public final String explanation;
        public final String level;

        Grade(String correction, String explanation, String level) {
            this.correction = correction;
            this.explanation = explanation;
            this.level = level;
        }
    }
}
