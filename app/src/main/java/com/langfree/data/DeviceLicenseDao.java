package com.langfree.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.langfree.model.DeviceLicenseEntry;

@Dao
public interface DeviceLicenseDao {
    @Query("SELECT COUNT(*) FROM device_licenses WHERE codeHash = :codeHash")
    int count(String codeHash);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DeviceLicenseEntry entry);
}
