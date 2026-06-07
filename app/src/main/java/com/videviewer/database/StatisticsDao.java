package com.videviewer.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

/**
 * StatisticsDao - Room DAO for video statistics
 */
@Dao
public interface StatisticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StatisticsEntity entity);

    @Query("UPDATE statistics SET watchCount = watchCount + 1, " +
           "totalWatchTime = totalWatchTime + :watchTime, lastWatched = :time " +
           "WHERE videoPath = :path")
    void updateWatchStats(String path, long watchTime, long time);

    @Query("SELECT * FROM statistics ORDER BY watchCount DESC LIMIT :limit")
    LiveData<List<StatisticsEntity>> getMostWatched(int limit);

    @Query("SELECT * FROM statistics WHERE videoPath = :path")
    StatisticsEntity getStatsByPath(String path);

    @Query("DELETE FROM statistics")
    void clearAll();
}
