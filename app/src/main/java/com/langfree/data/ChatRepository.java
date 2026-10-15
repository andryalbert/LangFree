package com.langfree.data;

import android.os.Looper;

import com.langfree.model.ChatMessage;
import com.langfree.model.Conversation;
import com.langfree.model.Mistake;
import com.langfree.model.TutorResponse;
import com.langfree.model.UserSettings;
import com.langfree.utils.TextNormalize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accès synchrone à Room. Chaque méthode doit être appelée hors du thread UI.
 */
public class ChatRepository {
    private static final int MAX_STORED_CHARS = 500;

    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final MistakeDao mistakeDao;

    public ChatRepository(AppDatabase database) {
        conversationDao = database.conversationDao();
        messageDao = database.messageDao();
        mistakeDao = database.mistakeDao();
    }

    public long createConversation(UserSettings settings) {
        checkBackground();
        long now = System.currentTimeMillis();
        Conversation conversation = new Conversation();
        conversation.title = "Nouvelle conversation";
        conversation.level = settings.level.name();
        conversation.topic = settings.topic.name();
        conversation.correctionMode = settings.correctionMode.name();
        conversation.createdAt = now;
        conversation.updatedAt = now;
        return conversationDao.insert(conversation);
    }

    public void addUserMessage(long conversationId, String text, UserSettings settings) {
        checkBackground();
        long now = System.currentTimeMillis();
        ChatMessage message = new ChatMessage();
        message.conversationId = conversationId;
        message.role = ChatMessage.ROLE_USER;
        message.rawText = text;
        message.timestamp = now;
        messageDao.insert(message);
        touchConversation(conversationId, text, settings, now, true);
    }

    public void addAssistantMessage(long conversationId, TutorResponse response, UserSettings settings) {
        checkBackground();
        long now = System.currentTimeMillis();
        ChatMessage message = new ChatMessage();
        message.conversationId = conversationId;
        message.role = ChatMessage.ROLE_ASSISTANT;
        message.rawText = response.rawText;
        message.correction = response.correction;
        message.explanation = response.explanation;
        message.reply = response.reply;
        message.fallback = response.fallback;
        message.timestamp = now;
        messageDao.insert(message);
        touchConversation(conversationId, null, settings, now, false);
    }

    /**
     * Derniers messages dans l'ordre chronologique (du plus ancien au plus récent).
     */
    public List<ChatMessage> latestChronological(long conversationId, int limit) {
        checkBackground();
        List<ChatMessage> newestFirst = messageDao.latestDesc(conversationId, limit);
        ArrayList<ChatMessage> chronological = new ArrayList<>(newestFirst);
        Collections.reverse(chronological);
        return chronological;
    }

    public List<Mistake> topMistakes(int limit) {
        checkBackground();
        return mistakeDao.top(limit);
    }

    /**
     * Mémorise une faute. La même explication (signature) incrémente le compteur.
     */
    public void recordMistake(long conversationId, String userText, TutorResponse response) {
        checkBackground();
        if (response == null || response.fallback || response.isPerfect()) {
            return;
        }
        String corrected = response.correction == null ? "" : response.correction.trim();
        if (corrected.isEmpty()) {
            return;
        }
        if (TextNormalize.same(userText, corrected)) {
            return;
        }
        String signature = TextNormalize.signature(response.explanation, userText, corrected);
        long now = System.currentTimeMillis();
        Mistake existing = mistakeDao.findBySignature(signature);
        if (existing == null) {
            Mistake mistake = new Mistake();
            mistake.signature = signature;
            mistake.originalText = clip(userText);
            mistake.correctedText = clip(corrected);
            mistake.explanation = clip(response.explanation);
            mistake.hitCount = 1;
            mistake.createdAt = now;
            mistake.updatedAt = now;
            mistake.conversationId = conversationId;
            mistakeDao.insert(mistake);
        } else {
            existing.hitCount = existing.hitCount + 1;
            existing.originalText = clip(userText);
            existing.correctedText = clip(corrected);
            existing.explanation = clip(response.explanation);
            existing.updatedAt = now;
            existing.conversationId = conversationId;
            mistakeDao.update(existing);
        }
    }

    public void deleteConversation(Conversation conversation) {
        checkBackground();
        conversationDao.delete(conversation);
    }

    public void deleteMistake(Mistake mistake) {
        checkBackground();
        mistakeDao.delete(mistake);
    }

    public void clearMistakes() {
        checkBackground();
        mistakeDao.deleteAll();
    }

    /** Recopie niveau, thème et mode sur la conversation ouverte, sans attendre un nouveau message. */
    public void applySettings(long conversationId, UserSettings settings) {
        checkBackground();
        Conversation conversation = conversationDao.getById(conversationId);
        if (conversation == null || settings == null) {
            return;
        }
        conversation.level = settings.level.name();
        conversation.topic = settings.topic.name();
        conversation.correctionMode = settings.correctionMode.name();
        conversationDao.update(conversation);
    }

    private void touchConversation(long conversationId, String firstUserText,
                                    UserSettings settings, long now, boolean maybeTitle) {
        Conversation conversation = conversationDao.getById(conversationId);
        if (conversation == null) {
            return;
        }
        if (maybeTitle && firstUserText != null
                && (conversation.title == null
                || conversation.title.isEmpty()
                || "Nouvelle conversation".equals(conversation.title))) {
            String trimmed = firstUserText.trim();
            if (trimmed.length() > 42) {
                conversation.title = trimmed.substring(0, 42) + "…";
            } else if (!trimmed.isEmpty()) {
                conversation.title = trimmed;
            }
        }
        conversation.level = settings.level.name();
        conversation.topic = settings.topic.name();
        conversation.correctionMode = settings.correctionMode.name();
        conversation.updatedAt = now;
        conversationDao.update(conversation);
    }

    private static String clip(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= MAX_STORED_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_STORED_CHARS);
    }

    private static void checkBackground() {
        if (Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("Accès Room interdit sur le thread UI");
        }
    }
}
