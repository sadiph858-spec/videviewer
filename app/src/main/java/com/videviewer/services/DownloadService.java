package com.videviewer.services;

  import android.app.Notification;
  import android.app.NotificationChannel;
  import android.app.NotificationManager;
  import android.app.PendingIntent;
  import android.app.Service;
  import android.content.Intent;
  import android.os.Build;
  import android.os.Environment;
  import android.os.IBinder;
  import androidx.annotation.Nullable;
  import androidx.core.app.NotificationCompat;
  import com.videviewer.R;
  import com.videviewer.utils.AppConstants;
  import java.io.File;
  import java.io.FileOutputStream;
  import java.io.InputStream;
  import java.net.HttpURLConnection;
  import java.net.URL;
  import java.util.concurrent.ExecutorService;
  import java.util.concurrent.Executors;

  public class DownloadService extends Service {

      private NotificationManager notificationManager;
      private ExecutorService executor = Executors.newFixedThreadPool(3);

      @Override
      public void onCreate() {
          super.onCreate();
          notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
          createNotificationChannel();
      }

      @Override
      public int onStartCommand(Intent intent, int flags, int startId) {
          String url = intent != null ? intent.getStringExtra("url") : null;
          if (url == null) { stopSelf(); return START_NOT_STICKY; }
          startForeground(AppConstants.NOTIFICATION_DOWNLOAD_ID, buildNotification("Starting download...", 0));
          executor.execute(() -> downloadFile(url, startId));
          return START_NOT_STICKY;
      }

      private void downloadFile(String urlStr, int startId) {
          String filename = urlStr.substring(urlStr.lastIndexOf('/') + 1);
          if (!filename.contains(".")) filename = "video_" + System.currentTimeMillis() + ".mp4";
          File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), AppConstants.DOWNLOAD_DIR);
          dir.mkdirs();
          File dest = new File(dir, filename);

          try {
              URL url = new URL(urlStr);
              HttpURLConnection conn = (HttpURLConnection) url.openConnection();
              conn.connect();
              long total = conn.getContentLengthLong();
              InputStream in = conn.getInputStream();
              FileOutputStream out = new FileOutputStream(dest);
              byte[] buf = new byte[8192];
              long downloaded = 0;
              int len;
              long lastUpdate = System.currentTimeMillis();
              long lastBytes = 0;

              while ((len = in.read(buf)) > 0) {
                  out.write(buf, 0, len);
                  downloaded += len;
                  long now = System.currentTimeMillis();
                  if (now - lastUpdate > 1000) {
                      double speed = (downloaded - lastBytes) / 1024.0 / 1024.0;
                      int pct = total > 0 ? (int)(downloaded * 100 / total) : -1;
                      String msg = String.format("%.1f MB/s · %d%%", speed, pct);
                      notificationManager.notify(AppConstants.NOTIFICATION_DOWNLOAD_ID, buildNotification(msg, pct));
                      lastUpdate = now;
                      lastBytes = downloaded;
                  }
              }
              out.close();
              in.close();
              conn.disconnect();
              notificationManager.notify(AppConstants.NOTIFICATION_DOWNLOAD_ID, buildNotification("Download complete: " + filename, 100));
          } catch (Exception e) {
              notificationManager.notify(AppConstants.NOTIFICATION_DOWNLOAD_ID, buildNotification("Download failed: " + e.getMessage(), -1));
          }
          stopSelf(startId);
      }

      private Notification buildNotification(String text, int progress) {
          NotificationCompat.Builder builder = new NotificationCompat.Builder(this, AppConstants.CHANNEL_DOWNLOAD)
              .setSmallIcon(R.drawable.ic_download)
              .setContentTitle("VidViewer Download")
              .setContentText(text)
              .setOngoing(true)
              .setPriority(NotificationCompat.PRIORITY_LOW);
          if (progress >= 0 && progress < 100) builder.setProgress(100, progress, false);
          else if (progress == 100) builder.setProgress(0, 0, false).setOngoing(false);
          return builder.build();
      }

      private void createNotificationChannel() {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              NotificationChannel ch = new NotificationChannel(AppConstants.CHANNEL_DOWNLOAD, "Downloads", NotificationManager.IMPORTANCE_LOW);
              notificationManager.createNotificationChannel(ch);
          }
      }

      @Nullable @Override public IBinder onBind(Intent intent) { return null; }
      @Override public void onDestroy() { super.onDestroy(); executor.shutdownNow(); }
  }