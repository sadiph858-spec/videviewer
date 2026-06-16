package com.videviewer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

    public static final String ACTION_PROGRESS   = "com.videviewer.DOWNLOAD_PROGRESS";
    public static final String ACTION_COMPLETE    = "com.videviewer.DOWNLOAD_COMPLETE";
    public static final String ACTION_FAILED      = "com.videviewer.DOWNLOAD_FAILED";
    public static final String EXTRA_URL          = "url";
    public static final String EXTRA_FILENAME     = "filename";
    public static final String EXTRA_FILEPATH     = "filepath";
    public static final String EXTRA_PROGRESS     = "progress";
    public static final String EXTRA_SPEED        = "speed";
    public static final String EXTRA_ERROR        = "error";
    public static final String EXTRA_THUMBNAIL    = "thumbnail_url";

    private NotificationManager notificationManager;
    private LocalBroadcastManager lbm;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        lbm = LocalBroadcastManager.getInstance(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url          = intent != null ? intent.getStringExtra("url")          : null;
        String customName   = intent != null ? intent.getStringExtra("filename")      : null;
        String thumbnailUrl = intent != null ? intent.getStringExtra(EXTRA_THUMBNAIL) : null;
        if (url == null) { stopSelf(); return START_NOT_STICKY; }
        startForeground(AppConstants.NOTIFICATION_DOWNLOAD_ID, buildNotification("Starting…", 0));
        final String tUrl = thumbnailUrl;
        final String cName = customName;
        executor.execute(() -> downloadFile(url, cName, tUrl, startId));
        return START_NOT_STICKY;
    }

    private void downloadFile(String urlStr, String customFilename, String thumbnailUrl, int startId) {
        // Derive filename
        String filename = customFilename;
        if (filename == null || filename.isEmpty()) {
            filename = urlStr.substring(urlStr.lastIndexOf('/') + 1);
            if (filename.contains("?")) filename = filename.substring(0, filename.indexOf('?'));
            if (filename.isEmpty() || !filename.contains("."))
                filename = "video_" + System.currentTimeMillis() + ".mp4";
        }
        if (!filename.endsWith(".mp4") && !filename.endsWith(".mkv")
                && !filename.endsWith(".webm") && !filename.endsWith(".m4v")) {
            filename = filename + ".mp4";
        }

        File dir = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            AppConstants.DOWNLOAD_DIR);
        dir.mkdirs();
        File dest = new File(dir, filename);

        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.connect();

            // Reject HTML responses (YouTube page, etc.)
            String ct = conn.getContentType();
            if (ct != null && ct.contains("text/html")) {
                conn.disconnect();
                broadcast(ACTION_FAILED, urlStr, filename, null, thumbnailUrl, 0, 0,
                    "Not a direct video stream. Please use the Download button in Browser.");
                stopSelf(startId);
                return;
            }

            long total = conn.getContentLengthLong();
            InputStream in    = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[16384];
            long downloaded = 0, lastBytes = 0;
            long lastUpdate = System.currentTimeMillis();
            int len;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                downloaded += len;
                long now = System.currentTimeMillis();
                if (now - lastUpdate >= 1000) {
                    double speed = (downloaded - lastBytes) / 1024.0 / 1024.0;
                    int pct = total > 0 ? (int)(downloaded * 100 / total) : 0;
                    notificationManager.notify(AppConstants.NOTIFICATION_DOWNLOAD_ID,
                        buildNotification(String.format("%.1f MB/s · %d%%", speed, pct), pct));
                    broadcast(ACTION_PROGRESS, urlStr, filename, null, thumbnailUrl, pct, speed, null);
                    lastUpdate = now;
                    lastBytes  = downloaded;
                }
            }
            out.close(); in.close(); conn.disconnect();

            notificationManager.notify(AppConstants.NOTIFICATION_DOWNLOAD_ID,
                buildNotification("✅ " + filename, 100));
            broadcast(ACTION_COMPLETE, urlStr, filename, dest.getAbsolutePath(), thumbnailUrl, 100, 0, null);

        } catch (Exception e) {
            if (dest.exists() && dest.length() == 0) dest.delete();
            notificationManager.notify(AppConstants.NOTIFICATION_DOWNLOAD_ID,
                buildNotification("Failed: " + e.getMessage(), -1));
            broadcast(ACTION_FAILED, urlStr, filename, null, thumbnailUrl, 0, 0, e.getMessage());
        }
        stopSelf(startId);
    }

    private void broadcast(String action, String url, String filename, String filepath,
                           String thumbnailUrl, int progress, double speed, String error) {
        Intent i = new Intent(action);
        i.putExtra(EXTRA_URL,      url);
        i.putExtra(EXTRA_FILENAME, filename);
        i.putExtra(EXTRA_PROGRESS, progress);
        i.putExtra(EXTRA_SPEED,    speed);
        if (filepath     != null) i.putExtra(EXTRA_FILEPATH,  filepath);
        if (thumbnailUrl != null) i.putExtra(EXTRA_THUMBNAIL, thumbnailUrl);
        if (error        != null) i.putExtra(EXTRA_ERROR,     error);
        lbm.sendBroadcast(i);
    }

    private Notification buildNotification(String text, int progress) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, AppConstants.CHANNEL_DOWNLOAD)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("VidViewer Download")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW);
        if (progress >= 0 && progress < 100) b.setProgress(100, progress, false);
        else if (progress >= 100)            b.setProgress(0, 0, false).setOngoing(false);
        return b.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                AppConstants.CHANNEL_DOWNLOAD, "Downloads",
                NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(ch);
        }
    }

    @Nullable @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() { super.onDestroy(); executor.shutdownNow(); }
}
