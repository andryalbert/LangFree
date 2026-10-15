package com.langfree.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.langfree.model.ChatMessage;

import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    LiveData<List<ChatMessage>> observe(long conversationId);

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC, id DESC LIMIT :limit")
    List<ChatMessage> latestDesc(long conversationId, int limit);

    @Insert
    long insert(ChatMessage message);
}
