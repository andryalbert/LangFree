package com.langfree.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Un passage de quiz : plusieurs questions posées à la suite. */
@Entity(tableName = "quiz_sessions")
public class QuizSession {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long startedAt;
    public long updatedAt;
    public String levelEstimate;
    public int questionCount;
}
