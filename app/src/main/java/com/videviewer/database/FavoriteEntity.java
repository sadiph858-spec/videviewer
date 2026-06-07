package com.videviewer.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * FavoriteEntity - Room entity for favorited videos
 */
@Entity(tableName = "favorites", indices = {@Index(value = "videoPath", unique = true)})
public class FavoriteEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String videoPath;
    public String videoTitle;
    public long addedAt;
    public long videoDuration;
    public long videoSize;
}
