package com.videviewer.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * HistoryEntity - Room entity for watch history
 */
@Entity(tableName = "history", indices = {@Index(value = "videoPath", unique = true)})
public class HistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String videoPath;
    public String videoTitle;
    public long lastWatched;
    public long resumePosition;
    public int watchCount;
    public long videoDuration;
    public long videoSize;
}
