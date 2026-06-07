package com.videviewer.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

/**
 * FavoriteDao - Room DAO for favorites table
 */
@Dao
public interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteEntity entity);

    @Delete
    void delete(FavoriteEntity entity);

    @Query("DELETE FROM favorites WHERE videoPath = :path")
    void deleteByPath(String path);

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    LiveData<List<FavoriteEntity>> getAll();

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    List<FavoriteEntity> getAllSync();

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE videoPath = :path)")
    boolean isFavorite(String path);

    @Query("SELECT COUNT(*) FROM favorites")
    int getCount();
}
