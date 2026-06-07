package com.videviewer.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * PlaylistEntity - Room entity for playlists
 */
@Entity(tableName = "playlists")
public class PlaylistEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String description;
    public long createdAt;
    public long updatedAt;
    public String coverVideoPath;
}
