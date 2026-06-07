package com.videviewer.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * VaultVideoEntity - Room entity for vault-hidden videos
 */
@Entity(tableName = "vault_videos", indices = {@Index(value = "originalPath", unique = true)})
public class VaultVideoEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String originalPath;
    public String vaultPath;
    public String videoTitle;
    public long fileSize;
    public long duration;
    public long addedToVault;
    public String originalFolder;
}
