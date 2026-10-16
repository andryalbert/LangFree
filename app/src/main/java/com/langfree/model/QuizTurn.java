package com.langfree.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Une question du quiz, la réponse de l'élève et la correction de l'IA. */
@Entity(
        tableName = "quiz_turns",
        indices = {@Index("sessionId")}
)
public class QuizTurn {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long sessionId;
    public String question;
    public String answer;
    public String correction;
    public String explanation;
    public String levelEstimate;
    public long createdAt;
}
