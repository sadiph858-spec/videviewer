package com.videviewer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.DownloadManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
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
        try {
            // Derive a safe filename
            String filename = customFilename;
            if (filename == null || filename.isEmpty()) {
                filename = urlStr.substring(urlStr.lastIndexOf('/') + 1);
                int q = filename.indexOf('?');
                if (q >= 0) filename = filename.substring(0, q);
                if (filename.isEmpty() || !filename.contains("."))
                    filename = "video_" + System.currentTimeMillis() + ".mp4";
            }
            if (!filename.endsWith(".mp4") && !filename.endsWith(".mkv")
                    && !filename.endsWith(".webm") && !filename.endsWith(".m4v")) {
                filename += ".mp4";
            }
            // Sanitize — no special chars that break DownloadManager
            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm == null) {
                broadcast(ACTION_FAILED, urlStr, filename, null, thumbnailUrl, 0, 0, "DownloadManager unavailable");
                stopSelf(startId); return;
            }

            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(urlStr));
            req.setTitle("VidViewer: " + filename);
            req.setDescription("Downloading…");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_DOWNLOADS,
                AppConstants.DOWNLOAD_DIR + "/" + filename);
            req.addRequestHeader("User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
            req.setAllowedOverMetered(true);
            req.setAllowedOverRoaming(true);

            long dmId = dm.enqueue(req);
            broadcast(ACTION_PROGRESS, urlStr, filename, null, thumbnailUrl, 0, 0, null);
            // DownloadManager handles progress & completion via its own notification
            // Send COMPLETE broadcast after a short delay (DownloadManager does the actual work)
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                broadcast(ACTION_COMPLETE, urlStr, filename, null, thumbnailUrl, 100, 0, null);
                stopSelf(startId);
            }, 500);
        } catch (Exception e) {
            broadcast(ACTION_FAILED, urlStr, customFilename != null ? customFilename : "video.mp4",
                null, thumbnailUrl, 0, 0, e.getMessage());
            stopSelf(startId);
        }
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
