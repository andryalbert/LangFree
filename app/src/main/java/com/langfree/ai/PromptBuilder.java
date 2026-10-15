package com.langfree.ai;

import com.langfree.model.ChatMessage;
import com.langfree.model.CorrectionMode;
import com.langfree.model.Topic;
import com.langfree.model.UserSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Construit le prompt du tuteur et ne garde que les 6 derniers échanges
 * (12 messages), puis raccourcit encore si le budget de tokens est dépassé.
 */
public final class PromptBuilder {
    /** 6 échanges = au plus 6 messages élève + 6 messages tuteur. */
    public static final int MAX_CONTEXT_MESSAGES = 12;
    /** Marge laissée au modèle pour écrire la réponse (maxTokens du moteur = 1024). */
    public static final int INPUT_TOKEN_BUDGET = 700;

    public interface TokenCounter {
        int count(String text);
    }

    private PromptBuilder() {
    }

    public static String build(UserSettings settings, List<ChatMessage> chronological,
                                TokenCounter counter) {
        ArrayList<ChatMessage> window = new ArrayList<>();
        if (chronological != null) {
            int start = Math.max(0, chronological.size() - MAX_CONTEXT_MESSAGES);
            for (int i = start; i < chronological.size(); i++) {
                window.add(chronological.get(i));
            }
        }
        String prompt = render(settings, window, Integer.MAX_VALUE);
        int guard = 0;
        while (window.size() > 1 && counter.count(prompt) > INPUT_TOKEN_BUDGET && guard < 24) {
            window.remove(0);
            prompt = render(settings, window, Integer.MAX_VALUE);
            guard++;
        }
        if (counter.count(prompt) > INPUT_TOKEN_BUDGET) {
            prompt = render(settings, window, 500);
        }
        return prompt;
    }

    public static String exercisePrompt(String mistakesBlock) {
        return "You are an English teacher for a French speaker.\n"
                + "The student often makes these mistakes:\n"
                + mistakesBlock
                + "\nWrite a short practice in EXACTLY this shape:\n"
                + "[CONSIGNE] one sentence of instructions in French\n"
                + "[PHRASES] three numbered English sentences that contain similar mistakes\n"
                + "[CORRIGE] the three corrected sentences\n"
                + "Do not add any other section.";
    }

    private static String render(UserSettings settings, List<ChatMessage> window, int maxLastChars) {
        StringBuilder builder = new StringBuilder();
        builder.append(systemPrompt(settings)).append("\n\n");
        if (window.isEmpty()) {
            builder.append("Student: Hello\n");
        } else {
            for (int i = 0; i < window.size(); i++) {
                ChatMessage message = window.get(i);
                boolean last = i == window.size() - 1;
                int limit = last ? maxLastChars : 700;
                String body = clip(message.rawText, limit);
                if (message.isUser()) {
                    builder.append("Student: ").append(body).append('\n');
                } else {
                    builder.append("Tutor: ").append(body).append('\n');
                }
            }
        }
        builder.append("Tutor:");
        return builder.toString();
    }

    static String systemPrompt(UserSettings settings) {
        String mode = settings.correctionMode == CorrectionMode.STRICT
                ? "Correction style: strict. Point out every grammar, vocabulary, conjugation and spelling mistake."
                : "Correction style: gentle. Correct only the important mistakes and keep the French explanation encouraging.";
        String topic = settings.topic == Topic.FREE
                ? "Conversation theme: free. Follow the student."
                : "Conversation theme: " + settings.topic.promptHint
                + ". Gently keep the chat on this theme.";
        return String.format(Locale.ROOT,
                "You are a friendly English tutor. The student is a French speaker at level %s.\n"
                        + "%s\n"
                        + "%s\n"
                        + "For every student message, reply in EXACTLY this format:\n"
                        + "[CORRECTION] the corrected sentence (or 'Perfect!' if no mistake)\n"
                        + "[EXPLICATION] short explanation in French of each mistake\n"
                        + "[REPONSE] your natural reply in English, adapted to the level, ending with a question to keep the conversation going.\n"
                        + "Keep replies short. Never skip a section. Do not write anything before [CORRECTION].",
                settings.level.promptValue,
                mode,
                topic);
    }

    private static String clip(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (maxChars == Integer.MAX_VALUE || trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxChars) + "…";
    }
}
