package com.videviewer.fragments;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.videviewer.R;
import com.videviewer.activities.PlayerActivity;
import com.videviewer.adapters.DownloadAdapter;
import com.videviewer.databinding.FragmentDownloadsBinding;
import com.videviewer.models.DownloadItem;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.VideoUrlResolver;
import com.videviewer.utils.CobaltResolver;
import android.media.MediaScannerConnection;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DownloadsFragment extends Fragment {
    private static final String PREFS_DL = "dl_tracker";

    private FragmentDownloadsBinding binding;
    private DownloadAdapter adapter;
    private final List<DownloadItem> downloadList = new ArrayList<>();

    // DownloadManager tracking: dmId → list position
    private final Map<Long, Integer> dmTracker = new HashMap<>();

    private DownloadManager downloadManager;
    private Handler progressHandler;
    private Runnable progressRunnable;

    // Receives DownloadManager.ACTION_DOWNLOAD_COMPLETE
    private final BroadcastReceiver dmReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Integer pos = dmTracker.remove(id);
            // Remove persisted tracker
            if (ctx != null) ctx.getSharedPreferences(PREFS_DL, 0)
                .edit().remove(String.valueOf(id)).apply();
            if (pos == null || pos < 0 || pos >= downloadList.size()) return;

            // Query final status
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(id);
            Cursor cur = downloadManager.query(q);
            if (cur != null && cur.moveToFirst()) {
                int status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                String localUri = cur.getString(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                cur.close();

                DownloadItem item = downloadList.get(pos);
                if (status == DownloadManager.STATUS_SUCCESSFUL && localUri != null) {
                    item.status   = DownloadItem.STATUS_COMPLETED;
                    item.progress = 100;
                    item.filePath = Uri.parse(localUri).getPath();
                      // Scan so Videos tab picks it up immediately
                      try {
                          final String _sp = item.filePath;
                          if (_sp != null && ctx != null) {
                              MediaScannerConnection.scanFile(ctx, new String[]{_sp}, null, null);
                          }
                      } catch (Exception _se) { _se.printStackTrace(); }
                      // ✅ Notify MediaStore → Videos tab will show this file after refresh
                      try {
                          final String scanPath = item.filePath;
                          if (scanPath != null && ctx != null) {
                              MediaScannerConnection.scanFile(ctx,
                                  new String[]{scanPath}, null,
                                  (path, uri) -> { /* indexed */ });
                          }
                      } catch (Exception scanEx) { scanEx.printStackTrace(); }
                    if (adapter != null) adapter.notifyItemChanged(pos);
                    if (ctx != null)
                        Toast.makeText(ctx, "✅ " + item.filename, Toast.LENGTH_SHORT).show();
                } else {
                    item.status = DownloadItem.STATUS_FAILED;
                    if (adapter != null) adapter.notifyItemChanged(pos);
                    if (ctx != null)
                        Toast.makeText(ctx, "❌ Download failed: " + item.filename, Toast.LENGTH_SHORT).show();
                }
            }
            updateEmpty();
        }
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDownloadsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        progressHandler  = new Handler(Looper.getMainLooper());

        adapter = new DownloadAdapter(requireContext(), downloadList);
        binding.recyclerDownloads.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerDownloads.setAdapter(adapter);

        // Tap → play
        adapter.setOnItemClickListener(item -> {
            if (item.status == DownloadItem.STATUS_COMPLETED
                    && item.filePath != null && !item.filePath.isEmpty()) {
                try {
                    Intent intent = new Intent(requireContext(), PlayerActivity.class);
                    intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, item.filePath);
                    intent.putExtra(AppConstants.EXTRA_VIDEO_TITLE, item.filename);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Cannot open: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Long press → delete
        adapter.setOnItemDeleteListener((pos, item) -> {
            if (pos < 0 || pos >= downloadList.size()) return;
            // Cancel active DM download if any
            for (Map.Entry<Long, Integer> e : new HashMap<>(dmTracker).entrySet()) {
                if (e.getValue().equals(pos)) {
                    downloadManager.remove(e.getKey());
                    dmTracker.remove(e.getKey());
                    break;
                }
            }
            // Delete file
            if (item.filePath != null && !item.filePath.isEmpty()) {
                File f = new File(item.filePath);
                if (f.exists()) f.delete();
            }
            downloadList.remove(pos);
            adapter.notifyItemRemoved(pos);
            // Shift dmTracker positions
            Map<Long, Integer> shifted = new HashMap<>();
            for (Map.Entry<Long, Integer> e : dmTracker.entrySet())
                shifted.put(e.getKey(), e.getValue() > pos ? e.getValue() - 1 : e.getValue());
            dmTracker.clear();
            dmTracker.putAll(shifted);
            updateEmpty();
            Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show();
        });

        binding.btnAddDownload.setOnClickListener(v -> showAddDownloadDialog());
    }

    @Override public void onResume() {
        super.onResume();
        requireContext().registerReceiver(dmReceiver,
            new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        startProgressPolling();
        scanDownloadedFiles();
        recoverPersistedDownloads();

        Bundle args = getArguments();
        if (args != null && args.containsKey("share_url")) {
            String url = args.getString("share_url");
            args.remove("share_url");
            if (url != null && !url.isEmpty()) resolveAndDownload(url);
        }
    }

    @Override public void onPause() {
        super.onPause();
        try { requireContext().unregisterReceiver(dmReceiver); } catch (Exception ignored) {}
        stopProgressPolling();
    }

    // ── Progress polling ─────────────────────────────────────────
    private void startProgressPolling() {
        progressRunnable = new Runnable() {
            @Override public void run() {
                if (adapter == null || dmTracker.isEmpty()) {
                    progressHandler.postDelayed(this, 1500);
                    return;
                }
                for (Map.Entry<Long, Integer> entry : new HashMap<>(dmTracker).entrySet()) {
                    long dmId = entry.getKey();
                    int  pos  = entry.getValue();
                    if (pos < 0 || pos >= downloadList.size()) continue;

                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(dmId);
                    Cursor cur = downloadManager.query(q);
                    if (cur != null && cur.moveToFirst()) {
                        int  status  = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                        long dl      = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        long total   = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        cur.close();

                        int pct = (total > 0) ? (int)(dl * 100 / total) : 0;
                        DownloadItem item = downloadList.get(pos);
                        item.progress = pct;

                        if (status == DownloadManager.STATUS_FAILED) {
                            item.status = DownloadItem.STATUS_FAILED;
                            dmTracker.remove(dmId);
                        } else if (status == DownloadManager.STATUS_PAUSED) {
                            item.speedMbps = 0;
                        }
                        adapter.notifyItemChanged(pos);
                    }
                }
                progressHandler.postDelayed(this, 1000);
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressPolling() {
        if (progressHandler != null && progressRunnable != null)
            progressHandler.removeCallbacks(progressRunnable);
    }

    // ── Scan existing files ──────────────────────────────────────
    private void scanDownloadedFiles() {
        File dir = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            AppConstants.DOWNLOAD_DIR);
        if (!dir.exists()) { updateEmpty(); return; }

        Set<String> tracked = new HashSet<>();
        for (DownloadItem d : downloadList) if (d.filePath != null) tracked.add(d.filePath);

        File[] files = dir.listFiles();
        if (files == null) { updateEmpty(); return; }
        int added = 0;
        for (File f : files) {
            if (!f.isFile()) continue;
            String n = f.getName().toLowerCase();
            if (!n.endsWith(".mp4") && !n.endsWith(".mkv") && !n.endsWith(".webm")
                && !n.endsWith(".avi") && !n.endsWith(".mov") && !n.endsWith(".3gp")
                && !n.endsWith(".m4v") && !n.endsWith(".flv")) continue;
            if (tracked.contains(f.getAbsolutePath())) continue;
            downloadList.add(new DownloadItem(f.getAbsolutePath(), f.getName()));
            added++;
        }
        if (added > 0 && adapter != null) adapter.notifyDataSetChanged();
        updateEmpty();
    }

    private void updateEmpty() {
        if (binding == null) return;
        binding.tvEmpty.setVisibility(downloadList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Public entry — called from BrowserFragment & MainActivity ─
      public void resolveAndDownload(String pageUrl) {
          if (pageUrl == null || pageUrl.isEmpty()) return;

          // cobalt.tools: YouTube, Instagram, TikTok, Twitter/X, Facebook, Reddit…
          if (CobaltResolver.isSupportedUrl(pageUrl)) {
              String tmpName = "video_" + System.currentTimeMillis() + ".mp4";
              DownloadItem pending = new DownloadItem(pageUrl, 0, DownloadItem.STATUS_DOWNLOADING);
              pending.filename = tmpName;
              String ytId = VideoUrlResolver.extractYouTubeId(pageUrl);
              if (ytId != null) pending.thumbnailUrl = VideoUrlResolver.youtubeThumbnail(ytId);
              int pendingPos = 0;
              downloadList.add(pendingPos, pending);
              if (adapter != null) adapter.notifyItemInserted(pendingPos);
              updateEmpty();
              if (getContext() != null)
                  Toast.makeText(getContext(), "🔗 Resolving link…", Toast.LENGTH_SHORT).show();

              CobaltResolver.resolve(pageUrl, new CobaltResolver.Callback() {
                  @Override
                  public void onResolved(String directUrl, String filename) {
                      if (getContext() == null || binding == null) return;
                      String fname = (filename != null && !filename.isEmpty()) ? filename : tmpName;
                      if (!fname.contains(".")) fname += ".mp4";
                      if (pendingPos < downloadList.size()) {
                          downloadList.get(pendingPos).filename = fname;
                          if (adapter != null) adapter.notifyItemChanged(pendingPos);
                      }
                      long dmId = enqueueDownload(directUrl, fname, null);
                      if (dmId >= 0) {
                          dmTracker.put(dmId, pendingPos);
                          Toast.makeText(getContext(), "⬇️ Downloading: " + fname, Toast.LENGTH_SHORT).show();
                      } else {
                          if (pendingPos < downloadList.size())
                              downloadList.get(pendingPos).status = DownloadItem.STATUS_FAILED;
                          if (adapter != null) adapter.notifyItemChanged(pendingPos);
                          Toast.makeText(getContext(), "❌ Failed to start download", Toast.LENGTH_SHORT).show();
                      }
                  }
                  @Override
                  public void onError(String message) {
                      if (getContext() == null) return;
                      // Fallback to VideoUrlResolver for YouTube
                      if (VideoUrlResolver.isSupportedPlatform(pageUrl)) {
                          VideoUrlResolver.resolve(pageUrl, new VideoUrlResolver.Callback() {
                              @Override public void onResolved(String streamUrl, String thumbUrl, String title) {
                                  if (getContext() == null || binding == null) return;
                                  String fname = (title != null && !title.isEmpty()) ? title : tmpName;
                                  if (pendingPos < downloadList.size()) {
                                      DownloadItem item = downloadList.get(pendingPos);
                                      item.filename = fname;
                                      if (thumbUrl != null) item.thumbnailUrl = thumbUrl;
                                      if (adapter != null) adapter.notifyItemChanged(pendingPos);
                                  }
                                  long dmId = enqueueDownload(streamUrl, fname, thumbUrl);
                                  if (dmId >= 0) {
                                      dmTracker.put(dmId, pendingPos);
                                  } else {
                                      if (pendingPos < downloadList.size())
                                          downloadList.get(pendingPos).status = DownloadItem.STATUS_FAILED;
                                      if (adapter != null) adapter.notifyItemChanged(pendingPos);
                                      Toast.makeText(getContext(), "❌ Failed to start download", Toast.LENGTH_SHORT).show();
                                  }
                              }
                              @Override public void onError(String err) {
                                  if (getContext() == null) return;
                                  if (pendingPos < downloadList.size()) {
                                      downloadList.get(pendingPos).status = DownloadItem.STATUS_FAILED;
                                      if (adapter != null) adapter.notifyItemChanged(pendingPos);
                                  }
                                  Toast.makeText(getContext(), "❌ " + err, Toast.LENGTH_LONG).show();
                              }
                          });
                      } else {
                          if (pendingPos < downloadList.size()) {
                              downloadList.get(pendingPos).status = DownloadItem.STATUS_FAILED;
                              if (adapter != null) adapter.notifyItemChanged(pendingPos);
                          }
                          Toast.makeText(getContext(), "❌ " + message, Toast.LENGTH_LONG).show();
                      }
                  }
              });

          } else {
              // Direct URL
              String filename = pageUrl.substring(pageUrl.lastIndexOf('/') + 1);
              if (filename.contains("?")) filename = filename.substring(0, filename.indexOf('?'));
              if (filename.isEmpty() || !filename.contains("."))
                  filename = "video_" + System.currentTimeMillis() + ".mp4";
              DownloadItem item = new DownloadItem(pageUrl, 0, DownloadItem.STATUS_DOWNLOADING);
              item.filename = filename;
              int pos = 0;
              downloadList.add(pos, item);
              if (adapter != null) adapter.notifyItemInserted(pos);
              updateEmpty();
              long dmId = enqueueDownload(pageUrl, filename, null);
              if (dmId >= 0) {
                  dmTracker.put(dmId, pos);
                  Toast.makeText(requireContext(), "⬇️ Download started…", Toast.LENGTH_SHORT).show();
              } else {
                  item.status = DownloadItem.STATUS_FAILED;
                  if (adapter != null) adapter.notifyItemChanged(pos);
                  Toast.makeText(requireContext(), "❌ Failed to start", Toast.LENGTH_SHORT).show();
              }
          }
      }

      /** Enqueue a download with Android DownloadManager; returns dmId or -1 on error */
    private long enqueueDownload(String url, String filename, String thumbnailUrl) {
        try {
            // Ensure filename is safe
            if (filename == null || filename.isEmpty())
                filename = "video_" + System.currentTimeMillis() + ".mp4";
            if (!filename.contains(".")) filename += ".mp4";
            // Sanitize — remove chars that break DownloadManager or filesystem
            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

            File destDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                AppConstants.DOWNLOAD_DIR);
            destDir.mkdirs();

            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setTitle("VidViewer");
            req.setDescription(filename);
            req.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, AppConstants.DOWNLOAD_DIR + "/" + filename);
            req.addRequestHeader("User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
            // Do NOT hardcode a Referer — Invidious proxied URLs and cobalt tunnel URLs
            // don't need a YouTube referer; wrong Referer can cause 403s.
            req.setAllowedOverMetered(true);
            req.setAllowedOverRoaming(true);
            req.allowScanningByMediaScanner();

            long _id = downloadManager.enqueue(req);
            requireContext().getSharedPreferences(PREFS_DL, 0)
                .edit().putString(String.valueOf(_id), filename).apply();
            return _id;
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null)
                android.widget.Toast.makeText(getContext(), "Download error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            return -1;
        }
    }

    // ── Add URL dialog ───────────────────────────────────────────
    private void showAddDownloadDialog() {
        if (getContext() == null) return;
        android.app.AlertDialog.Builder b =
            new android.app.AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme);
        b.setTitle("Download Video 🎥");
        final EditText et = new EditText(requireContext());
        et.setHint("YouTube, Instagram, TikTok, Twitter/X, or direct video URL");
        et.setTextColor(0xFFFFFFFF);
        et.setHintTextColor(0xFF888888);
        et.setPadding(48, 24, 48, 24);
        b.setView(et);
        b.setPositiveButton("Download", (d, w) -> {
            String url = et.getText().toString().trim();
            if (url.isEmpty()) return;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(requireContext(),
                    "URL must start with http:// or https://", Toast.LENGTH_SHORT).show();
                return;
            }
            resolveAndDownload(url);
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

        /** Recover downloads that completed or are still running while fragment was paused */
    private void recoverPersistedDownloads() {
        if (downloadManager == null || getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_DL, 0);
        java.util.Map<String, ?> all = prefs.getAll();
        if (all == null || all.isEmpty()) return;
        Set<String> trackedPaths = new HashSet<>();
        for (DownloadItem d : downloadList) if (d.filePath != null) trackedPaths.add(d.filePath);
        boolean changed = false;
        for (java.util.Map.Entry<String, ?> entry : all.entrySet()) {
            try {
                long dmId = Long.parseLong(entry.getKey());
                String fname = String.valueOf(entry.getValue());
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(dmId);
                Cursor cur = downloadManager.query(q);
                if (cur == null) { prefs.edit().remove(entry.getKey()).apply(); continue; }
                if (!cur.moveToFirst()) { cur.close(); prefs.edit().remove(entry.getKey()).apply(); continue; }
                int dmStatus = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                String localUri = cur.getString(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                cur.close();
                if (dmStatus == DownloadManager.STATUS_SUCCESSFUL && localUri != null) {
                    String path = Uri.parse(localUri).getPath();
                    if (path != null && !trackedPaths.contains(path)) {
                        DownloadItem item = new DownloadItem(path, fname);
                        downloadList.add(0, item);
                        trackedPaths.add(path);
                        changed = true;
                        // Scan so Videos tab picks it up
                        MediaScannerConnection.scanFile(requireContext(),
                            new String[]{path}, null, null);
                    }
                    prefs.edit().remove(entry.getKey()).apply();
                } else if (dmStatus == DownloadManager.STATUS_FAILED) {
                    prefs.edit().remove(entry.getKey()).apply();
                } else if (!dmTracker.containsKey(dmId)) {
                    // Still running — re-track it
                    DownloadItem item = new DownloadItem(fname, 0, DownloadItem.STATUS_DOWNLOADING);
                    item.filename = fname;
                    int pos = downloadList.size();
                    downloadList.add(item);
                    dmTracker.put(dmId, pos);
                    changed = true;
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }
        if (changed && adapter != null) adapter.notifyDataSetChanged();
        updateEmpty();
    }

@Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
