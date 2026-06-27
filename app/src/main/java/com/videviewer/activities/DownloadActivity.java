package com.videviewer.activities;

  import android.app.DownloadManager;
  import android.content.BroadcastReceiver;
  import android.content.Context;
  import android.content.Intent;
  import android.content.IntentFilter;
  import android.database.Cursor;
  import android.media.MediaScannerConnection;
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
  import com.videviewer.utils.VideoUrlResolver;
  import java.io.File;
  import java.util.HashMap;
  import java.util.Map;

  public class DownloadActivity extends AppCompatActivity {

      private TextInputEditText etUrl;
      private MaterialButton btnDownload;
      private ProgressBar progressBar;
      private TextView tvProgress, tvStatus;
      private DownloadManager downloadManager;
      private final Map<Long, String> activeDownloads = new HashMap<>();  // dmId → filePath
      private Handler progressHandler;
      private Runnable progressRunnable;

      private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
              try {
                  long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                  String savedPath = activeDownloads.remove(id);
                  if (savedPath == null) return;

                  // Query final status to get actual local URI
                  DownloadManager.Query q = new DownloadManager.Query();
                  q.setFilterById(id);
                  Cursor cur = downloadManager.query(q);
                  String localPath = savedPath;
                  boolean success = false;
                  if (cur != null && cur.moveToFirst()) {
                      int status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                      String localUri = cur.getString(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                      success = (status == DownloadManager.STATUS_SUCCESSFUL);
                      if (localUri != null) localPath = Uri.parse(localUri).getPath();
                      cur.close();
                  }

                  if (success) {
                      // ✅ Notify MediaStore so the Videos tab picks it up immediately
                      final String finalPath = localPath;
                      MediaScannerConnection.scanFile(context,
                          new String[]{ finalPath },
                          null,
                          (path, uri) -> {
                              // File is now indexed — Videos tab will show it on next refresh
                          });

                      String fileName = new File(finalPath).getName();
                      if (tvStatus != null) tvStatus.setText("✅ Download complete — check the Videos tab!");
                      if (progressBar != null) progressBar.setProgress(100);
                      Toast.makeText(context,
                          "✅ Downloaded: " + fileName + "\nPull-down to refresh Videos tab.",
                          Toast.LENGTH_LONG).show();
                  } else {
                      if (tvStatus != null) tvStatus.setText("❌ Download failed");
                      Toast.makeText(context, "❌ Download failed. Try a direct .mp4 link.", Toast.LENGTH_LONG).show();
                  }

                  stopProgressUpdates();
              } catch (Exception e) { e.printStackTrace(); }
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

          if (btnDownload != null) btnDownload.setOnClickListener(v -> startDownload());

          registerReceiver(downloadReceiver,
              new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
      }

      private void startDownload() {
          try {
              if (etUrl == null) return;
              String rawUrl = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";

              if (rawUrl.isEmpty()) {
                  Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
                  return;
              }
              if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                  Toast.makeText(this, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show();
                  return;
              }

              // Show loading state
              if (tvStatus != null) tvStatus.setText("Resolving URL...");
              if (progressBar != null) { progressBar.setVisibility(View.VISIBLE); progressBar.setProgress(0); }
              if (btnDownload != null) btnDownload.setEnabled(false);

              if (VideoUrlResolver.isSupportedPlatform(rawUrl)) {
                  // YouTube / Vimeo / Dailymotion — resolve to direct stream URL first
                  VideoUrlResolver.resolve(rawUrl, new VideoUrlResolver.Callback() {
                      @Override public void onResolved(String streamUrl, String thumb, String title) {
                          enqueueDownload(streamUrl, title);
                      }
                      @Override public void onError(String message) {
                          if (tvStatus != null) tvStatus.setText("❌ " + message);
                          if (btnDownload != null) btnDownload.setEnabled(true);
                          Toast.makeText(DownloadActivity.this,
                              "Cannot resolve link. Paste a direct .mp4 URL instead.",
                              Toast.LENGTH_LONG).show();
                      }
                  });
              } else {
                  // Direct video URL — download immediately
                  String guessedName = rawUrl.split("\\?")[0];
                  guessedName = guessedName.substring(guessedName.lastIndexOf('/') + 1);
                  if (guessedName.isEmpty() || !guessedName.contains(".")) {
                      guessedName = "video_" + System.currentTimeMillis() + ".mp4";
                  }
                  enqueueDownload(rawUrl, guessedName);
              }
          } catch (Exception e) {
              e.printStackTrace();
              Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
              if (btnDownload != null) btnDownload.setEnabled(true);
          }
      }

      private void enqueueDownload(String url, String fileName) {
          try {
              // Sanitise filename
              if (fileName == null || fileName.isEmpty()) fileName = "video_" + System.currentTimeMillis() + ".mp4";
              if (!fileName.contains(".")) fileName += ".mp4";
              fileName = fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_");

              // Save to Movies/VidViewer/ — this is reliably indexed by MediaStore
              File destDir = new File(
                  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "VidViewer");
              if (!destDir.exists()) destDir.mkdirs();
              String savedPath = new File(destDir, fileName).getAbsolutePath();

              DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
              request.setTitle("VidViewer – " + fileName);
              request.setDescription("Downloading video…");
              request.setNotificationVisibility(
                  DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
              request.setDestinationUri(Uri.fromFile(new File(savedPath)));
              request.setAllowedOverMetered(true);
              request.setAllowedOverRoaming(true);
              request.addRequestHeader("User-Agent",
                  "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");

              long downloadId = downloadManager.enqueue(request);
              activeDownloads.put(downloadId, savedPath);

              if (tvStatus != null) tvStatus.setText("Downloading: " + fileName);
              if (btnDownload != null) btnDownload.setEnabled(true);
              Toast.makeText(this, "Download started! Goes to Movies/VidViewer/", Toast.LENGTH_LONG).show();

              startProgressUpdates(downloadId);
          } catch (Exception e) {
              e.printStackTrace();
              if (tvStatus != null) tvStatus.setText("❌ Error: " + e.getMessage());
              if (btnDownload != null) btnDownload.setEnabled(true);
              Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
          }
      }

      private void startProgressUpdates(long downloadId) {
          progressRunnable = new Runnable() {
              @Override public void run() {
                  try {
                      DownloadManager.Query query = new DownloadManager.Query();
                      query.setFilterById(downloadId);
                      Cursor cursor = downloadManager.query(query);
                      if (cursor != null && cursor.moveToFirst()) {
                          int status = cursor.getInt(
                              cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                          long downloaded = cursor.getLong(
                              cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                          long total = cursor.getLong(
                              cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                          cursor.close();
                          if (total > 0) {
                              int progress = (int) (downloaded * 100 / total);
                              if (progressBar != null) progressBar.setProgress(progress);
                              if (tvProgress != null)
                                  tvProgress.setText(formatSize(downloaded) + " / " + formatSize(total) + " (" + progress + "%)");
                          }
                          if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING)
                              progressHandler.postDelayed(this, 1000);
                      }
                  } catch (Exception e) { e.printStackTrace(); }
              }
          };
          progressHandler.post(progressRunnable);
      }

      private void stopProgressUpdates() {
          try { if (progressRunnable != null) progressHandler.removeCallbacks(progressRunnable); }
          catch (Exception e) { e.printStackTrace(); }
      }

      private String formatSize(long bytes) {
          if (bytes < 1024) return bytes + " B";
          if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
          return String.format("%.1f MB", bytes / (1024.0 * 1024));
      }

      @Override protected void onDestroy() {
          super.onDestroy();
          stopProgressUpdates();
          try { unregisterReceiver(downloadReceiver); } catch (Exception e) {}
      }

      @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
  }
  