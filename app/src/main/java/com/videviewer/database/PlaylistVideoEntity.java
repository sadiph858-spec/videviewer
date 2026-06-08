package com.videviewer.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * PlaylistVideoEntity - Junction table linking playlists to videos
 */
@Entity(tableName = "playlist_videos", primaryKeys = {"playlistId", "videoPath"})
public class PlaylistVideoEntity {
    public long playlistId;
    @NonNull
    public String videoPath = "";
    public int sortOrder;
    public long addedAt;
}
