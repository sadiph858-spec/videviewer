package com.videviewer.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

/**
 * PlaylistDao - Room DAO for playlists
 */
@Dao
public interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPlaylist(PlaylistEntity entity);

    @Update
    void updatePlaylist(PlaylistEntity entity);

    @Delete
    void deletePlaylist(PlaylistEntity entity);

    @Query("DELETE FROM playlists WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    LiveData<List<PlaylistEntity>> getAllPlaylists();

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    List<PlaylistEntity> getAllPlaylistsSync();

    @Query("SELECT * FROM playlists WHERE id = :id")
    PlaylistEntity getPlaylistById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addVideoToPlaylist(PlaylistVideoEntity entity);

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoPath = :path")
    void removeVideoFromPlaylist(long playlistId, String path);

    @Query("SELECT * FROM playlist_videos WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    List<PlaylistVideoEntity> getPlaylistVideos(long playlistId);

    @Query("SELECT COUNT(*) FROM playlist_videos WHERE playlistId = :playlistId")
    int getVideoCountForPlaylist(long playlistId);
}
