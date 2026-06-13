package com.videviewer.utils;

  import android.content.ContentValues;
  import android.content.Context;
  import android.content.Intent;
  import android.net.Uri;
  import android.os.Build;
  import android.os.Environment;
  import android.provider.MediaStore;
  import androidx.core.content.FileProvider;
  import java.io.File;
  import java.io.FileInputStream;
  import java.io.FileOutputStream;
  import java.io.InputStream;
  import java.io.OutputStream;

  public class FileUtils {

      public static void shareVideo(Context context, String path) {
          File file = new File(path);
          Uri uri;
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
              uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
          } else {
              uri = Uri.fromFile(file);
          }
          Intent intent = new Intent(Intent.ACTION_SEND);
          intent.setType("video/*");
          intent.putExtra(Intent.EXTRA_STREAM, uri);
          intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          context.startActivity(Intent.createChooser(intent, "Share Video"));
      }

      public static void saveToGallery(Context context, String sourcePath) {
          try {
              File source = new File(sourcePath);
              String filename = source.getName();

              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                  ContentValues values = new ContentValues();
                  values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                  String mimeType = sourcePath.endsWith(".mp4") ? "video/mp4" : "image/jpeg";
                  values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                  values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/VidViewer");
                  Uri collection = sourcePath.endsWith(".mp4")
                      ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                      : MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                  Uri itemUri = context.getContentResolver().insert(collection, values);
                  if (itemUri != null) {
                      try (OutputStream out = context.getContentResolver().openOutputStream(itemUri);
                           InputStream in = new FileInputStream(source)) {
                          byte[] buf = new byte[8192];
                          int len;
                          while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                      }
                  }
              } else {
                  File destDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "VidViewer");
                  destDir.mkdirs();
                  File dest = new File(destDir, filename);
                  try (InputStream in = new FileInputStream(source);
                       OutputStream out = new FileOutputStream(dest)) {
                      byte[] buf = new byte[8192];
                      int len;
                      while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                  }
                  context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(dest)));
              }
          } catch (Exception e) {
              e.printStackTrace();
          }
      }
  }