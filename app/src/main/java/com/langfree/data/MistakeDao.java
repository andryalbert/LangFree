package com.langfree.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.langfree.model.Mistake;

import java.util.List;

@Dao
public interface MistakeDao {
    @Query("SELECT * FROM mistakes ORDER BY hitCount DESC, updatedAt DESC")
    LiveData<List<Mistake>> observeAll();

    @Query("SELECT * FROM mistakes ORDER BY hitCount DESC, updatedAt DESC LIMIT :limit")
    List<Mistake> top(int limit);

    @Query("SELECT * FROM mistakes WHERE signature = :signature LIMIT 1")
    Mistake findBySignature(String signature);

    @Insert
    long insert(Mistake mistake);

    @Update
    void update(Mistake mistake);

    @Delete
    void delete(Mistake mistake);

    @Query("DELETE FROM mistakes")
    void deleteAll();
}
