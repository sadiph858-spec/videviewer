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

      @Delete
      void delete(HistoryEntity entity);

      @Query("SELECT * FROM history ORDER BY lastWatched DESC")
      LiveData<List<HistoryEntity>> getAll();

      @Query("SELECT * FROM history ORDER BY lastWatched DESC LIMIT :limit")
      List<HistoryEntity> getRecent(int limit);

      @Query("DELETE FROM history")
      void clearAll();

      @Query("SELECT * FROM history WHERE videoPath = :path LIMIT 1")
      HistoryEntity getByPath(String path);
  }