package com.langfree.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Erreur regroupée par signature (explication normalisée). {@link #hitCount} augmente
 * quand la même faute revient.
 */
@Entity(
        tableName = "mistakes",
        indices = {@Index(value = "signature", unique = true)}
)
public class Mistake {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String signature;
    public String originalText;
    public String correctedText;
    public String explanation;
    public int hitCount;
    public long createdAt;
    public long updatedAt;
    public long conversationId;
}
