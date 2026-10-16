package com.langfree.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Empreinte du code déjà vérifié pour ce téléphone. Le code en clair n'est pas stocké. */
@Entity(tableName = "device_licenses")
public class DeviceLicenseEntry {
    @PrimaryKey
    @NonNull
    public String codeHash = "";

    public long acceptedAt;
}
