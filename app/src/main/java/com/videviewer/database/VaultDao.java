package com.videviewer.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

/**
 * VaultDao - Room DAO for vault videos
 */
@Dao
public interface VaultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(VaultVideoEntity entity);

    @Delete
    void delete(VaultVideoEntity entity);

    @Query("DELETE FROM vault_videos WHERE originalPath = :path")
    void deleteByOriginalPath(String path);

    @Query("SELECT * FROM vault_videos ORDER BY addedToVault DESC")
    LiveData<List<VaultVideoEntity>> getAll();

    @Query("SELECT * FROM vault_videos ORDER BY addedToVault DESC")
    List<VaultVideoEntity> getAllSync();

    @Query("SELECT EXISTS(SELECT 1 FROM vault_videos WHERE originalPath = :path)")
    boolean isInVault(String path);

    @Query("SELECT COUNT(*) FROM vault_videos")
    int getCount();
}
