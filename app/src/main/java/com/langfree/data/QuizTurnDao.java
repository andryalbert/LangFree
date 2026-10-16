package com.langfree.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.langfree.model.QuizTurn;

import java.util.List;

@Dao
public interface QuizTurnDao {
    @Query("SELECT * FROM quiz_turns WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    LiveData<List<QuizTurn>> observe(long sessionId);

    @Query("SELECT question FROM quiz_turns WHERE sessionId = :sessionId ORDER BY createdAt DESC LIMIT 6")
    List<String> recentQuestions(long sessionId);

    @Query("SELECT * FROM quiz_turns WHERE id = :id")
    QuizTurn getById(long id);

    @Insert
    long insert(QuizTurn turn);

    @Update
    void update(QuizTurn turn);

    @Query("DELETE FROM quiz_turns WHERE sessionId = :sessionId")
    void deleteForSession(long sessionId);
}
