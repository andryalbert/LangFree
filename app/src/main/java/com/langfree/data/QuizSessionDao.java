package com.langfree.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.langfree.model.QuizSession;

import java.util.List;

@Dao
public interface QuizSessionDao {
    @Query("SELECT * FROM quiz_sessions WHERE questionCount > 0 ORDER BY updatedAt DESC")
    LiveData<List<QuizSession>> observeAnswered();

    @Query("SELECT * FROM quiz_sessions WHERE id = :id")
    LiveData<QuizSession> observeById(long id);

    @Query("SELECT * FROM quiz_sessions WHERE id = :id")
    QuizSession getById(long id);

    @Insert
    long insert(QuizSession session);

    @Update
    void update(QuizSession session);

    @Delete
    void delete(QuizSession session);
}
