package com.videviewer.activities;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.videviewer.R;
import java.util.HashMap;
import java.util.Map;

public class DownloadActivity extends AppCompatActivity {

    private TextInputEditText etUrl;
    private MaterialButton btnDownload;
    private ProgressBar progressBar;
    private TextView tvProgress, tvStatus;
    private DownloadManager downloadManager;
    private final Map<Long, String> activeDownloads = new HashMap<>();
    private Handler progressHandler;
    private Runnable progressRunnable;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (activeDownloads.containsKey(id)) {
                String fileName = activeDownloads.get(id);
                Toast.makeText(context,
                    "✅ Download complete: " + fileName, Toast.LENGTH_LONG).show();
                activeDownloads.remove(id);
                if (tvStatus != null) tvStatus.setText("Download complete!");
                if (progressBar != null) progressBar.setProgress(100);
                stopProgressUpdates();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        progressHandler = new Handler(Looper.getMainLooper());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Download Video");
        }

        etUrl = findViewById(R.id.et_download_url);
        btnDownload = findViewById(R.id.btn_download);
        progressBar = findViewById(R.id.download_progress);
        tvProgress = findViewById(R.id.tv_progress);
        tvStatus = findViewById(R.id.tv_download_status);

        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> startDownload());
        }

        // Register download complete receiver
        registerReceiver(downloadReceiver,
            new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void startDownload() {
        if (etUrl == null) return;
        String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";

        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "Invalid URL. Must start with http:// or https://",
                Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Get filename from URL
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            if (fileName.isEmpty() || !fileName.contains(".")) {
                fileName = "video_" + System.currentTimeMillis() + ".mp4";
            }

            // Setup download request
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("VidéViewer Download");
            request.setDescription("Downloading: " + fileName);
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, "VidViewer/" + fileName);
            request.allowScanningByMediaScanner();
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            // Add headers for some sites
            request.addRequestHeader("User-Agent",
                "Mozilla/5.0 (Android) VidViewer/1.0");

            long downloadId = downloadManager.enqueue(request);
            activeDownloads.put(downloadId, fileName);

            if (tvStatus != null) tvStatus.setText("Downloading: " + fileName);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
            }

            Toast.makeText(this,
                "Download started! Will continue in background.",
                Toast.LENGTH_LONG).show();

            // Start progress updates
            startProgressUpdates(downloadId);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Download failed: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    private void startProgressUpdates(long downloadId) {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    Cursor cursor = downloadManager.query(query);

                    if (cursor != null && cursor.moveToFirst()) {
                        int status = cursor.getInt(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_STATUS));
                        long downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        long total = cursor.getLong(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        cursor.close();

                        if (total > 0) {
                            int progress = (int) (downloaded * 100 / total);
                            if (progressBar != null) progressBar.setProgress(progress);
                            if (tvProgress != null) {
                                tvProgress.setText(formatSize(downloaded) +
                                    " / " + formatSize(total) + " (" + progress + "%)");
                            }
                        }

                        if (status == DownloadManager.STATUS_RUNNING ||
                            status == DownloadManager.STATUS_PENDING) {
                            progressHandler.postDelayed(this, 1000);
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdates();
        try { unregisterReceiver(downloadReceiver); } catch (Exception e) {}
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
