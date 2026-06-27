package com.videviewer.database;

  import android.content.Context;
  import android.os.AsyncTask;
  import androidx.room.Database;
  import androidx.room.Room;
  import androidx.room.RoomDatabase;
  import androidx.room.TypeConverters;
  import com.videviewer.utils.AppConstants;

  /**
   * AppDatabase - Room database for favorites, history, playlists, vault
   */
  @Database(
      entities = {FavoriteEntity.class, HistoryEntity.class, PlaylistEntity.class, PlaylistVideoEntity.class, VaultVideoEntity.class, StatisticsEntity.class},
      version = AppConstants.DB_VERSION,
      exportSchema = false
  )
  @TypeConverters({Converters.class})
  public abstract class AppDatabase extends RoomDatabase {

      private static volatile AppDatabase INSTANCE;

      public abstract FavoriteDao favoriteDao();
      public abstract HistoryDao historyDao();
      public abstract PlaylistDao playlistDao();
      public abstract VaultDao vaultDao();
      public abstract StatisticsDao statisticsDao();

      public static AppDatabase getInstance(Context context) {
          if (INSTANCE == null) {
              synchronized (AppDatabase.class) {
                  if (INSTANCE == null) {
                      INSTANCE = Room.databaseBuilder(
                          context.getApplicationContext(),
                          AppDatabase.class,
                          AppConstants.DB_NAME
                      )
                      .fallbackToDestructiveMigration()
                      .build();
                  }
              }
          }
          return INSTANCE;
      }

      public static class ClearHistoryTask extends AsyncTask<Void, Void, Void> {
          private final Context context;
          public ClearHistoryTask(Context context) { this.context = context.getApplicationContext(); }

          @Override
          protected Void doInBackground(Void... voids) {
              getInstance(context).historyDao().clearAll();
              return null;
          }
      }
  }