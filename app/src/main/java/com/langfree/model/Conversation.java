package com.langfree.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Une conversation persistée. Les messages associés sont dans {@link ChatMessage}.
 */
@Entity(tableName = "conversations")
public class Conversation {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String level;
    public String topic;
    public String correctionMode;
    public long createdAt;
    public long updatedAt;
}
