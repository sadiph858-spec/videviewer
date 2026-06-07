package com.videviewer.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * StatisticsEntity - Room entity for video watch statistics
 */
@Entity(tableName = "statistics", indices = {@Index(value = "videoPath", unique = true)})
public class StatisticsEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String videoPath;
    public String videoTitle;
    public int watchCount;
    public long totalWatchTime;
    public long lastWatched;
    public long firstWatched;
}
