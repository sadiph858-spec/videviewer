package com.videviewer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.videviewer.R;
import com.videviewer.activities.PlayerActivity;
import com.videviewer.utils.AppConstants;

/**
 * PlaybackService - Foreground service for media playback
 * Keeps playback alive in background / PiP mode
 * Shows a persistent notification with playback controls
 */
public class PlaybackService extends Service {

    private static final String TAG = "PlaybackService";

    public class LocalBinder extends Binder {
        public PlaybackService getService() { return PlaybackService.this; }
    }

    private final IBinder binder = new LocalBinder();
    private String currentVideoTitle = "";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getStringExtra("video_title") != null) {
            currentVideoTitle = intent.getStringExtra("video_title");
        }
        startForeground(AppConstants.NOTIFICATION_ID_PLAYBACK, buildNotification());
        return START_NOT_STICKY;
    }

    private Notification buildNotification() {
        // Tap notification to return to player
        Intent openIntent = new Intent(this, PlayerActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_PLAYBACK)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(currentVideoTitle.isEmpty()
                ? getString(R.string.loading) : currentVideoTitle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_PLAYBACK,
                "Video Playback",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows video playback status");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    public void updateTitle(String title) {
        currentVideoTitle = title;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(AppConstants.NOTIFICATION_ID_PLAYBACK, buildNotification());
        }
    }
}
