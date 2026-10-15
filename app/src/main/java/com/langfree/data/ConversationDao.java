package com.langfree.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.langfree.model.Conversation;

import java.util.List;

@Dao
public interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    LiveData<List<Conversation>> observeAll();

    @Query("SELECT * FROM conversations WHERE id = :id")
    LiveData<Conversation> observeById(long id);

    @Query("SELECT * FROM conversations WHERE id = :id")
    Conversation getById(long id);

    @Insert
    long insert(Conversation conversation);

    @Update
    void update(Conversation conversation);

    @Delete
    void delete(Conversation conversation);
}
