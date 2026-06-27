package com.videviewer.utils;

  import android.content.ContentProvider;
  import android.content.ContentValues;
  import android.content.Context;
  import android.database.Cursor;
  import android.media.MediaScannerConnection;
  import android.net.Uri;
  import android.os.Environment;
  import android.util.Log;

  import java.io.File;

  /**
   * FolderInitProvider – auto-initialises the VidViewer folder on first app launch.
   *
   * Because ContentProviders are created before Application.onCreate(), this runs
   * very early in the process without touching any existing file.
   *
   * Created folder: <ExternalStorage>/Movies/VidViewer/
   * Any video placed in that folder is automatically indexed by MediaStore and
   * will appear in the Videos section of the app.
   */
  public class FolderInitProvider extends ContentProvider {

      private static final String TAG = "FolderInitProvider";
      /** Public folder name shown to the user */
      public static final String FOLDER_NAME = "VidViewer";

      @Override
      public boolean onCreate() {
          try {
              createVidViewerFolder(getContext());
          } catch (Exception e) {
              Log.e(TAG, "onCreate error", e);
          }
          return false; // false = we are not a real data provider
      }

      /**
       * Creates Movies/VidViewer/ and triggers a MediaStore scan so the folder
       * appears immediately without a reboot.
       */
      public static void createVidViewerFolder(Context context) {
          try {
              if (context == null) return;

              // Primary location: /sdcard/Movies/VidViewer/
              File moviesDir = Environment.getExternalStoragePublicDirectory(
                      Environment.DIRECTORY_MOVIES);
              File vidViewerDir = new File(moviesDir, FOLDER_NAME);

              if (!vidViewerDir.exists()) {
                  boolean created = vidViewerDir.mkdirs();
                  Log.d(TAG, "VidViewer folder created: " + created + " @ " + vidViewerDir.getAbsolutePath());
              } else {
                  Log.d(TAG, "VidViewer folder already exists @ " + vidViewerDir.getAbsolutePath());
              }

              // Place a .nomedia file so thumbnails aren't shown in the system gallery
              // (they WILL still appear inside VidViewer because we use MediaStore directly)
              // ── DISABLED: keep media visible everywhere ──
              // new File(vidViewerDir, ".nomedia").createNewFile();

              // Trigger MediaStore scan so any existing videos in the folder appear immediately
              try {
                  MediaScannerConnection.scanFile(
                          context,
                          new String[]{vidViewerDir.getAbsolutePath()},
                          null,
                          (path, uri) -> Log.d(TAG, "Scanned: " + path)
                  );
              } catch (Exception scanEx) {
                  Log.e(TAG, "MediaScan error", scanEx);
              }

          } catch (Exception e) {
              Log.e(TAG, "createVidViewerFolder error", e);
          }
      }

      /**
       * Returns the full path of the VidViewer folder (creates it if missing).
       * Use this from any activity/fragment to get a copy-target path.
       */
      public static File getVidViewerFolder() {
          try {
              File dir = new File(
                      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                      FOLDER_NAME);
              if (!dir.exists()) dir.mkdirs();
              return dir;
          } catch (Exception e) {
              return null;
          }
      }

      // ── Unused ContentProvider overrides (required by the API) ──────────────

      @Override public Cursor query(Uri u, String[] p, String s, String[] sa, String so) { return null; }
      @Override public String getType(Uri u) { return null; }
      @Override public Uri insert(Uri u, ContentValues v) { return null; }
      @Override public int delete(Uri u, String s, String[] sa) { return 0; }
      @Override public int update(Uri u, ContentValues v, String s, String[] sa) { return 0; }
  }
  