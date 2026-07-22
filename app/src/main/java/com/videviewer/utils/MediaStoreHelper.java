package com.videviewer.utils;

  import android.content.Context;
  import android.database.Cursor;
  import android.net.Uri;
  import android.provider.MediaStore;
  import com.videviewer.models.VideoItem;
  import java.util.ArrayList;
  import java.util.List;

  public class MediaStoreHelper {

      public static List<VideoItem> getAllVideos(Context context) {
          List<VideoItem> videos = new ArrayList<>();
          // ── Build vault-excluded path set so vault videos stay hidden ──────
          java.util.Set<String> vaultPaths = new java.util.HashSet<>();
          try {
              java.util.List<com.videviewer.database.VaultVideoEntity> vaultList =
                  com.videviewer.database.AppDatabase.getInstance(context).vaultDao().getAllSync();
              for (com.videviewer.database.VaultVideoEntity v : vaultList) {
                  if (v.originalPath != null) vaultPaths.add(v.originalPath);
              }
          } catch (Exception ignored) {}
          Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
          String[] projection = {
              MediaStore.Video.Media._ID,
              MediaStore.Video.Media.TITLE,
              MediaStore.Video.Media.DATA,
              MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
              MediaStore.Video.Media.DURATION,
              MediaStore.Video.Media.SIZE,
              MediaStore.Video.Media.MIME_TYPE,
              MediaStore.Video.Media.DATE_ADDED
          };
          String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

          try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, sortOrder)) {
              if (cursor != null && cursor.moveToFirst()) {
                  int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                  int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
                  int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                  int folderCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                  int durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                  int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                  int mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);
                  int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);

                  do {
                      VideoItem item = new VideoItem();
                      item.id = cursor.getLong(idCol);
                      item.title = cursor.getString(titleCol);
                      item.path = cursor.getString(dataCol);
                      item.folder = cursor.getString(folderCol);
                      item.duration = cursor.getLong(durCol);
                      item.size = cursor.getLong(sizeCol);
                      item.mimeType = cursor.getString(mimeCol);
                      item.dateAdded = cursor.getLong(dateCol);
                      if (item.path != null && new java.io.File(item.path).exists()
                              && !vaultPaths.contains(item.path)) {
                          videos.add(item);
                      }
                  } while (cursor.moveToNext());
              }
          } catch (Exception e) {
              e.printStackTrace();
          }
          return videos;
      }

      public static boolean deleteVideo(Context context, String path) {
          Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
          int deleted = context.getContentResolver().delete(uri, MediaStore.Video.Media.DATA + "=?", new String[]{path});
          return deleted > 0;
      }
  }