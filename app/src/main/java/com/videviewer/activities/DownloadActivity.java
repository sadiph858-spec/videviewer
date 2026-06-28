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
      /** dmId -> fileName (just the filename, not full path) */
      private final Map<Long, String> activeDownloads = new HashMap<>();
      private Handler progressHandler;
      private Runnable progressRunnable;

      private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
              try {
                  long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                  String fileName = activeDownloads.remove(id);
                  if (fileName == null) return;

                  // Compute the expected file path deterministically
                  File destDir = new File(
                      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                      "VidViewer");
                  File destFile = new File(destDir, fileName);

                  // Check DownloadManager status
                  DownloadManager.Query q = new DownloadManager.Query();
                  q.setFilterById(id);
                  Cursor cur = downloadManager.query(q);
                  boolean success = false;
                  if (cur != null && cur.moveToFirst()) {
                      int status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                      success = (status == DownloadManager.STATUS_SUCCESSFUL);
                      cur.close();
                  }

                  stopProgressUpdates();

                  if (success && destFile.exists()) {
                      // Notify MediaStore — Videos tab will see this file on next refresh
                      MediaScannerConnection.scanFile(context,
                          new String[]{ destFile.getAbsolutePath() }, null, null);

                      if (tvStatus != null)
                          tvStatus.setText("Downloaded! Open Videos tab and pull down to refresh.");
                      if (progressBar != null) progressBar.setProgress(100);
                      Toast.makeText(context,
                          "Done: " + fileName + " saved to Movies/VidViewer/",
                          Toast.LENGTH_LONG).show();
                  } else {
                      if (tvStatus != null) tvStatus.setText("Download failed.");
                      Toast.makeText(context, "Download failed. Try a direct .mp4 link.", Toast.LENGTH_LONG).show();
                  }
              } catch (Exception e) { e.printStackTrace(); }
          }
      };

      @Override
      protected void onCreate(Bundle savedInstanceState) {
          try {
              super.onCreate(savedInstanceState);
              setContentView(R.layout.activity_download);

              downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
              progressHandler  = new Handler(Looper.getMainLooper());

              MaterialToolbar toolbar = findViewById(R.id.toolbar);
              setSupportActionBar(toolbar);
              if (getSupportActionBar() != null) {
                  getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                  getSupportActionBar().setTitle("Download Video");
              }

              etUrl       = findViewById(R.id.et_download_url);
              btnDownload = findViewById(R.id.btn_download);
              progressBar = findViewById(R.id.download_progress);
              tvProgress  = findViewById(R.id.tv_progress);
              tvStatus    = findViewById(R.id.tv_download_status);

              if (btnDownload != null) btnDownload.setOnClickListener(v -> startDownload());

              registerReceiver(downloadReceiver,
                  new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
          } catch (Exception e) { e.printStackTrace(); }
      }

      private void startDownload() {
          try {
              if (etUrl == null) return;
              String rawUrl = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";

              if (rawUrl.isEmpty()) {
                  Toast.makeText(this, "URL দিন", Toast.LENGTH_SHORT).show(); return;
              }
              if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                  Toast.makeText(this, "URL অবশ্যই http:// বা https:// দিয়ে শুরু হতে হবে", Toast.LENGTH_SHORT).show(); return;
              }

              if (tvStatus != null) tvStatus.setText("URL যাচাই করা হচ্ছে...");
              if (progressBar != null) { progressBar.setVisibility(View.VISIBLE); progressBar.setProgress(0); }
              if (btnDownload != null) btnDownload.setEnabled(false);

              if (VideoUrlResolver.isSupportedPlatform(rawUrl)) {
                  VideoUrlResolver.resolve(rawUrl, new VideoUrlResolver.Callback() {
                      @Override public void onResolved(String streamUrl, String thumb, String title) {
                          enqueueDownload(streamUrl, title);
                      }
                      @Override public void onError(String message) {
                          if (tvStatus != null) tvStatus.setText("Failed: " + message);
                          if (btnDownload != null) btnDownload.setEnabled(true);
                          Toast.makeText(DownloadActivity.this,
                              "YouTube লিংক হলে সরাসরি .mp4 লিংক paste করুন।", Toast.LENGTH_LONG).show();
                      }
                  });
              } else {
                  // Direct link
                  int _qi = rawUrl.indexOf('?');
                  String name = _qi >= 0 ? rawUrl.substring(0, _qi) : rawUrl;
                  name = name.substring(name.lastIndexOf('/') + 1);
                  if (name.isEmpty() || !name.contains(".")) name = "video_" + System.currentTimeMillis() + ".mp4";
                  enqueueDownload(rawUrl, name);
              }
          } catch (Exception e) {
              e.printStackTrace();
              if (btnDownload != null) btnDownload.setEnabled(true);
          }
      }

      private void enqueueDownload(String url, String rawName) {
          try {
              if (rawName == null || rawName.isEmpty()) rawName = "video_" + System.currentTimeMillis() + ".mp4";
              if (!rawName.contains(".")) rawName += ".mp4";
              // Safe filename — only alphanumeric, dot, dash, underscore
              final String fileName = rawName.replaceAll("[^a-zA-Z0-9._-]", "_");

              DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
              req.setTitle("VidViewer – " + fileName);
              req.setDescription("Downloading…");
              req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
              // setDestinationInExternalPublicDir uses DownloadManager's system permissions — safe on all API levels
              req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VidViewer/" + fileName);
              req.setAllowedOverMetered(true);
              req.setAllowedOverRoaming(true);
              req.addRequestHeader("User-Agent",
                  "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");

              long dmId = downloadManager.enqueue(req);
              activeDownloads.put(dmId, fileName);

              if (tvStatus != null) tvStatus.setText("Downloading: " + fileName);
              if (btnDownload != null) btnDownload.setEnabled(true);
              Toast.makeText(this, "শুরু হয়েছে! Movies/VidViewer/ এ সেভ হবে।", Toast.LENGTH_LONG).show();
              startProgressUpdates(dmId);
          } catch (Exception e) {
              e.printStackTrace();
              if (tvStatus != null) tvStatus.setText("Error: " + e.getMessage());
              if (btnDownload != null) btnDownload.setEnabled(true);
              Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
          }
      }

      private void startProgressUpdates(long downloadId) {
          progressRunnable = new Runnable() {
              @Override public void run() {
                  try {
                      DownloadManager.Query query = new DownloadManager.Query();
                      query.setFilterById(downloadId);
                      Cursor c = downloadManager.query(query);
                      if (c != null && c.moveToFirst()) {
                          int status   = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                          long done    = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                          long total   = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                          c.close();
                          if (total > 0) {
                              int pct = (int)(done * 100 / total);
                              if (progressBar != null) progressBar.setProgress(pct);
                              if (tvProgress  != null) tvProgress.setText(fmt(done) + " / " + fmt(total) + " (" + pct + "%)");
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
          try { if (progressRunnable != null) progressHandler.removeCallbacks(progressRunnable); } catch (Exception e) {}
      }

      private String fmt(long b) {
          if (b < 1024) return b + " B";
          if (b < 1024*1024) return String.format("%.1f KB", b/1024.0);
          return String.format("%.1f MB", b/(1024.0*1024));
      }

      @Override protected void onDestroy() {
          super.onDestroy();
          stopProgressUpdates();
          try { unregisterReceiver(downloadReceiver); } catch (Exception e) {}
      }

      @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
  }
  