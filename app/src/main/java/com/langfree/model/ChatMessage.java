package com.langfree.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bulle de conversation. Pour l'assistant, les trois sections sont stockées à part
 * afin de les réafficher sans relancer le modèle. {@link #rawText} garde la sortie brute.
 */
@Entity(
        tableName = "messages",
        foreignKeys = @ForeignKey(
                entity = Conversation.class,
                parentColumns = "id",
                childColumns = "conversationId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("conversationId")}
)
public class ChatMessage {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long conversationId;
    public String role;
    public String rawText;
    public String correction;
    public String explanation;
    public String reply;
    public boolean fallback;
    public long timestamp;

    public boolean isUser() {
        return ROLE_USER.equals(role);
    }
}
