package com.videviewer.fragments;

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
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DownloadsFragment extends Fragment {

    private FragmentDownloadsBinding binding;
    private DownloadAdapter adapter;
    private final List<DownloadItem> downloadList = new ArrayList<>();

    // dmId → list position
    private final Map<Long, Integer> dmTracker = new HashMap<>();
    private DownloadManager downloadManager;
    private Handler progressHandler;
    private Runnable progressRunnable;

    private final BroadcastReceiver dmReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Integer pos = dmTracker.remove(id);
            if (pos == null || pos < 0 || pos >= downloadList.size()) return;

            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(id);
            Cursor cur = downloadManager.query(q);
            if (cur != null && cur.moveToFirst()) {
                int status   = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                String local = cur.getString(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                cur.close();
                DownloadItem item = downloadList.get(pos);
                if (status == DownloadManager.STATUS_SUCCESSFUL && local != null) {
                    item.status   = DownloadItem.STATUS_COMPLETED;
                    item.progress = 100;
                    item.filePath = Uri.parse(local).getPath();
                    if (ctx != null)
                        Toast.makeText(ctx, "✅ " + item.filename, Toast.LENGTH_SHORT).show();
                } else {
                    item.status = DownloadItem.STATUS_FAILED;
                    if (ctx != null)
                        Toast.makeText(ctx, "❌ Failed: " + item.filename, Toast.LENGTH_SHORT).show();
                }
                if (adapter != null) adapter.notifyItemChanged(pos);
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
                    Intent it = new Intent(requireContext(), PlayerActivity.class);
                    it.putExtra(AppConstants.EXTRA_VIDEO_PATH, item.filePath);
                    it.putExtra(AppConstants.EXTRA_VIDEO_TITLE, item.filename);
                    startActivity(it);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Cannot open: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Long press → delete
        adapter.setOnItemDeleteListener((pos, item) -> {
            if (pos < 0 || pos >= downloadList.size()) return;
            for (Map.Entry<Long, Integer> e : new HashMap<>(dmTracker).entrySet()) {
                if (e.getValue().equals(pos)) {
                    downloadManager.remove(e.getKey()); dmTracker.remove(e.getKey()); break;
                }
            }
            if (item.filePath != null) { File f = new File(item.filePath); if (f.exists()) f.delete(); }
            downloadList.remove(pos);
            adapter.notifyItemRemoved(pos);
            Map<Long, Integer> shifted = new HashMap<>();
            for (Map.Entry<Long, Integer> e : dmTracker.entrySet())
                shifted.put(e.getKey(), e.getValue() > pos ? e.getValue() - 1 : e.getValue());
            dmTracker.clear(); dmTracker.putAll(shifted);
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

    // ── Called by BrowserFragment when it already started a DM download ──
    public void addPendingDownload(String url, String filename, String thumbnailUrl) {
        if (getContext() == null) return;
        DownloadItem item = new DownloadItem(url, 0, DownloadItem.STATUS_DOWNLOADING);
        item.filename     = filename;
        item.thumbnailUrl = thumbnailUrl;
        downloadList.add(0, item);
        if (adapter != null) adapter.notifyItemInserted(0);
        updateEmpty();
        // Try to find the matching DM entry and track it
        trackMatchingDmDownload(filename, 0);
    }

    private void trackMatchingDmDownload(String filename, int pos) {
        if (downloadManager == null) return;
        DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterByStatus(DownloadManager.STATUS_RUNNING
            | DownloadManager.STATUS_PENDING
            | DownloadManager.STATUS_PAUSED);
        Cursor cur = downloadManager.query(q);
        if (cur == null) return;
        while (cur.moveToNext()) {
            String title = cur.getString(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_DESCRIPTION));
            if (filename.equals(title)) {
                long dmId = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
                dmTracker.put(dmId, pos);
                break;
            }
        }
        cur.close();
    }

    // ── Public: resolve URL via API then enqueue download ────────────
    public void resolveAndDownload(String pageUrl) {
        if (pageUrl == null || pageUrl.isEmpty()) return;

        if (VideoUrlResolver.isSupportedPlatform(pageUrl)) {
            String ytId  = VideoUrlResolver.extractYouTubeId(pageUrl);
            String thumb = VideoUrlResolver.youtubeThumbnail(ytId);
            String tmpName = (ytId != null ? ytId : "video_" + System.currentTimeMillis()) + ".mp4";

            DownloadItem pending = new DownloadItem(pageUrl, 0, DownloadItem.STATUS_DOWNLOADING);
            pending.filename     = tmpName;
            pending.thumbnailUrl = thumb;
            downloadList.add(0, pending);
            if (adapter != null) adapter.notifyItemInserted(0);
            updateEmpty();
            if (getContext() != null)
                Toast.makeText(getContext(), "Resolving stream…", Toast.LENGTH_SHORT).show();

            VideoUrlResolver.resolve(pageUrl, new VideoUrlResolver.Callback() {
                @Override public void onResolved(String streamUrl, String thumbUrl, String title) {
                    if (getContext() == null || binding == null) return;
                    if (!downloadList.isEmpty()) {
                        DownloadItem item = downloadList.get(0);
                        item.filename     = title;
                        if (thumbUrl != null) item.thumbnailUrl = thumbUrl;
                        if (adapter != null) adapter.notifyItemChanged(0);
                    }
                    long dmId = enqueueDownload(streamUrl, title,
                        thumbUrl != null ? thumbUrl : thumb);
                    if (dmId >= 0) dmTracker.put(dmId, 0);
                    else {
                        if (!downloadList.isEmpty()) downloadList.get(0).status = DownloadItem.STATUS_FAILED;
                        if (adapter != null) adapter.notifyItemChanged(0);
                        Toast.makeText(getContext(), "Failed to start download", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onError(String message) {
                    if (getContext() == null) return;
                    if (!downloadList.isEmpty()) {
                        downloadList.get(0).status = DownloadItem.STATUS_FAILED;
                        if (adapter != null) adapter.notifyItemChanged(0);
                    }
                    Toast.makeText(getContext(), "❌ " + message, Toast.LENGTH_LONG).show();
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
            downloadList.add(0, item);
            if (adapter != null) adapter.notifyItemInserted(0);
            updateEmpty();
            long dmId = enqueueDownload(pageUrl, filename, null);
            if (dmId >= 0) {
                dmTracker.put(dmId, 0);
                if (getContext() != null)
                    Toast.makeText(getContext(), "Download started…", Toast.LENGTH_SHORT).show();
            } else {
                item.status = DownloadItem.STATUS_FAILED;
                if (adapter != null) adapter.notifyItemChanged(0);
            }
        }
    }

    private long enqueueDownload(String url, String filename, String thumbnailUrl) {
        try {
            if (filename == null || filename.isEmpty())
                filename = "video_" + System.currentTimeMillis() + ".mp4";
            if (!filename.contains(".")) filename += ".mp4";
            File destDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                AppConstants.DOWNLOAD_DIR);
            destDir.mkdirs();
            DownloadManager.Request req =
                new DownloadManager.Request(Uri.parse(url));
            req.setTitle("VidViewer");
            req.setDescription(filename);
            req.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                AppConstants.DOWNLOAD_DIR + "/" + filename);
            req.addRequestHeader("User-Agent",
                "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 "
                + "Chrome/120.0.0.0 Mobile Safari/537.36");
            req.addRequestHeader("Referer", "https://www.youtube.com/");
            req.setAllowedOverMetered(true);
            req.setAllowedOverRoaming(true);
            req.allowScanningByMediaScanner();
            return downloadManager.enqueue(req);
        } catch (Exception e) { return -1; }
    }

    // ── Progress polling ─────────────────────────────────────────────
    private void startProgressPolling() {
        progressRunnable = new Runnable() {
            @Override public void run() {
                if (adapter != null && !dmTracker.isEmpty()) {
                    for (Map.Entry<Long, Integer> entry : new HashMap<>(dmTracker).entrySet()) {
                        long dmId = entry.getKey(); int pos = entry.getValue();
                        if (pos < 0 || pos >= downloadList.size()) continue;
                        DownloadManager.Query q = new DownloadManager.Query();
                        q.setFilterById(dmId);
                        Cursor cur = downloadManager.query(q);
                        if (cur != null && cur.moveToFirst()) {
                            int  status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                            long dl     = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            long total  = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            cur.close();
                            int pct = (total > 0) ? (int)(dl * 100 / total) : 0;
                            DownloadItem item = downloadList.get(pos);
                            item.progress = pct;
                            if (status == DownloadManager.STATUS_FAILED) {
                                item.status = DownloadItem.STATUS_FAILED;
                                dmTracker.remove(dmId);
                            }
                            adapter.notifyItemChanged(pos);
                        }
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

    // ── Scan existing files ──────────────────────────────────────────
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

    // ── Add URL dialog ───────────────────────────────────────────────
    private void showAddDownloadDialog() {
        if (getContext() == null) return;
        android.app.AlertDialog.Builder b =
            new android.app.AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme);
        b.setTitle("Download Video");
        final EditText et = new EditText(requireContext());
        et.setHint("YouTube link or direct video URL");
        et.setTextColor(0xFFFFFFFF);
        et.setHintTextColor(0xFF888888);
        et.setPadding(48, 24, 48, 24);
        b.setView(et);
        b.setPositiveButton("Download", (d, w) -> {
            String url = et.getText().toString().trim();
            if (url.isEmpty()) return;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(requireContext(), "URL must start with http:// or https://",
                    Toast.LENGTH_SHORT).show();
                return;
            }
            resolveAndDownload(url);
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
