package com.videviewer.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

/**
 * HistoryDao - Room DAO for watch history
 */
@Dao
public interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryEntity entity);

    @Query("DELETE FROM history WHERE videoPath = :path")
    void deleteByPath(String path);

    @Query("DELETE FROM history")
    void clearAll();

    @Query("SELECT * FROM history ORDER BY lastWatched DESC LIMIT :limit")
    LiveData<List<HistoryEntity>> getRecent(int limit);

    @Query("SELECT * FROM history ORDER BY lastWatched DESC")
    List<HistoryEntity> getAllSync();

    @Query("SELECT resumePosition FROM history WHERE videoPath = :path")
    long getResumePosition(String path);

    @Query("UPDATE history SET resumePosition = :position, lastWatched = :time WHERE videoPath = :path")
    void updateResumePosition(String path, long position, long time);

    @Query("SELECT COUNT(*) FROM history")
    int getCount();
}
