package com.videviewer.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.videviewer.R;
import com.videviewer.activities.PlayerActivity;
import com.videviewer.adapters.DownloadAdapter;
import com.videviewer.databinding.FragmentDownloadsBinding;
import com.videviewer.models.DownloadItem;
import com.videviewer.services.DownloadService;
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
    private final Map<String, Integer> activeByUrl = new HashMap<>();

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            if (binding == null) return;
            String action       = intent.getAction();
            String url          = intent.getStringExtra(DownloadService.EXTRA_URL);
            String filename     = intent.getStringExtra(DownloadService.EXTRA_FILENAME);
            String thumbnailUrl = intent.getStringExtra(DownloadService.EXTRA_THUMBNAIL);
            int    progress     = intent.getIntExtra(DownloadService.EXTRA_PROGRESS, 0);
            double speed        = intent.getDoubleExtra(DownloadService.EXTRA_SPEED, 0);

            if (DownloadService.ACTION_PROGRESS.equals(action)) {
                Integer pos = activeByUrl.get(url);
                if (pos != null && pos < downloadList.size()) {
                    DownloadItem item = downloadList.get(pos);
                    item.progress  = progress;
                    item.speedMbps = speed;
                    adapter.notifyItemChanged(pos);
                }

            } else if (DownloadService.ACTION_COMPLETE.equals(action)) {
                String filepath = intent.getStringExtra(DownloadService.EXTRA_FILEPATH);
                Integer pos = activeByUrl.remove(url);
                if (pos != null && pos < downloadList.size()) {
                    DownloadItem item = downloadList.get(pos);
                    item.status   = DownloadItem.STATUS_COMPLETED;
                    item.progress = 100;
                    item.filePath = filepath;
                    adapter.notifyItemChanged(pos);
                } else {
                    DownloadItem done = new DownloadItem(filepath, filename);
                    if (thumbnailUrl != null) done.thumbnailUrl = thumbnailUrl;
                    downloadList.add(0, done);
                    adapter.notifyItemInserted(0);
                }
                updateEmpty();
                Toast.makeText(ctx, "✅ Downloaded: " + filename, Toast.LENGTH_SHORT).show();

            } else if (DownloadService.ACTION_FAILED.equals(action)) {
                String error = intent.getStringExtra(DownloadService.EXTRA_ERROR);
                Integer pos = activeByUrl.remove(url);
                if (pos != null && pos < downloadList.size()) {
                    downloadList.get(pos).status = DownloadItem.STATUS_FAILED;
                    adapter.notifyItemChanged(pos);
                }
                Toast.makeText(ctx, "❌ " + error, Toast.LENGTH_LONG).show();
            }
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
        adapter = new DownloadAdapter(requireContext(), downloadList);
        binding.recyclerDownloads.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerDownloads.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            if (item.status == DownloadItem.STATUS_COMPLETED && item.filePath != null) {
                Intent intent = new Intent(requireContext(), PlayerActivity.class);
                intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, item.filePath);
                intent.putExtra(AppConstants.EXTRA_VIDEO_TITLE, item.filename);
                startActivity(intent);
            }
        });
        binding.btnAddDownload.setOnClickListener(v -> showAddDownloadDialog());
    }

    @Override public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_PROGRESS);
        filter.addAction(DownloadService.ACTION_COMPLETE);
        filter.addAction(DownloadService.ACTION_FAILED);
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(downloadReceiver, filter);
        scanDownloadedFiles();

        // Handle URL passed from share intent or BrowserFragment
        Bundle args = getArguments();
        if (args != null && args.containsKey("share_url")) {
            String shareUrl = args.getString("share_url");
            args.remove("share_url");
            if (shareUrl != null && !shareUrl.isEmpty()) {
                resolveAndDownload(shareUrl);
            }
        }
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(downloadReceiver);
    }

    private void scanDownloadedFiles() {
        File dir = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            AppConstants.DOWNLOAD_DIR);
        if (!dir.exists()) { updateEmpty(); return; }
        Set<String> tracked = new HashSet<>();
        for (DownloadItem d : downloadList)
            if (d.filePath != null) tracked.add(d.filePath);
        File[] files = dir.listFiles();
        if (files == null) { updateEmpty(); return; }
        int inserted = 0;
        for (File f : files) {
            if (!f.isFile()) continue;
            String n = f.getName().toLowerCase();
            if (!n.endsWith(".mp4") && !n.endsWith(".mkv")
                && !n.endsWith(".webm") && !n.endsWith(".avi")
                && !n.endsWith(".mov") && !n.endsWith(".3gp")) continue;
            if (tracked.contains(f.getAbsolutePath())) continue;
            downloadList.add(new DownloadItem(f.getAbsolutePath(), f.getName()));
            inserted++;
        }
        if (inserted > 0) adapter.notifyDataSetChanged();
        updateEmpty();
    }

    private void updateEmpty() {
        if (binding == null) return;
        binding.tvEmpty.setVisibility(downloadList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Public entry point called from BrowserFragment or MainActivity (share intent) */
    public void resolveAndDownload(String pageUrl) {
        if (VideoUrlResolver.isSupportedPlatform(pageUrl)) {
            // Show thumbnail immediately while resolving
            String ytId   = VideoUrlResolver.extractYouTubeId(pageUrl);
            String thumb  = VideoUrlResolver.youtubeThumbnail(ytId);
            String tmpName = (ytId != null ? ytId : "video_" + System.currentTimeMillis()) + ".mp4";

            DownloadItem pending = new DownloadItem(pageUrl, 0, DownloadItem.STATUS_DOWNLOADING);
            pending.filename     = tmpName;
            pending.thumbnailUrl = thumb;
            int pos = 0;
            downloadList.add(pos, pending);
            activeByUrl.put(pageUrl, pos);
            if (adapter != null) adapter.notifyItemInserted(pos);
            updateEmpty();

            Toast.makeText(requireContext(), "Resolving video stream…", Toast.LENGTH_SHORT).show();

            VideoUrlResolver.resolve(pageUrl, new VideoUrlResolver.Callback() {
                @Override public void onResolved(String streamUrl, String thumbnailUrl, String title) {
                    // Update pending item thumbnail if we got a better one
                    Integer p = activeByUrl.get(pageUrl);
                    if (p != null && p < downloadList.size()) {
                        DownloadItem item = downloadList.get(p);
                        if (thumbnailUrl != null) item.thumbnailUrl = thumbnailUrl;
                        item.filename = title;
                        if (adapter != null) adapter.notifyItemChanged(p);
                    }
                    // Remove old URL key, register stream URL
                    activeByUrl.remove(pageUrl);
                    if (p != null) activeByUrl.put(streamUrl, p);

                    // Start actual download
                    startDownloadService(streamUrl, title, thumbnailUrl != null ? thumbnailUrl : thumb);
                }
                @Override public void onError(String message) {
                    Integer p = activeByUrl.remove(pageUrl);
                    if (p != null && p < downloadList.size()) {
                        downloadList.get(p).status = DownloadItem.STATUS_FAILED;
                        if (adapter != null) adapter.notifyItemChanged(p);
                    }
                    if (getContext() != null)
                        Toast.makeText(getContext(), "❌ " + message, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Direct URL — download immediately
            startDownload(pageUrl, null, null);
        }
    }

    private void startDownload(String url, String filename, String thumbnailUrl) {
        if (activeByUrl.containsKey(url)) {
            Toast.makeText(requireContext(), "Already downloading", Toast.LENGTH_SHORT).show();
            return;
        }
        DownloadItem item = new DownloadItem(url, 0, DownloadItem.STATUS_DOWNLOADING);
        if (thumbnailUrl != null) item.thumbnailUrl = thumbnailUrl;
        if (filename     != null) item.filename     = filename;
        int pos = 0;
        downloadList.add(pos, item);
        activeByUrl.put(url, pos);
        if (adapter != null) adapter.notifyItemInserted(pos);
        updateEmpty();
        startDownloadService(url, filename, thumbnailUrl);
        Toast.makeText(requireContext(), "Download started…", Toast.LENGTH_SHORT).show();
    }

    private void startDownloadService(String url, String filename, String thumbnailUrl) {
        Intent intent = new Intent(requireContext(), DownloadService.class);
        intent.putExtra("url", url);
        if (filename     != null) intent.putExtra("filename", filename);
        if (thumbnailUrl != null) intent.putExtra(DownloadService.EXTRA_THUMBNAIL, thumbnailUrl);
        requireContext().startForegroundService(intent);
    }

    private void showAddDownloadDialog() {
        android.app.AlertDialog.Builder builder =
            new android.app.AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme);
        builder.setTitle("Download Video");
        final EditText et = new EditText(requireContext());
        et.setHint("Paste video URL or YouTube/social link");
        et.setTextColor(0xFFFFFFFF);
        et.setHintTextColor(0xFF888888);
        et.setPadding(40, 20, 40, 20);
        builder.setView(et);
        builder.setPositiveButton("Download", (d, w) -> {
            String url = et.getText().toString().trim();
            if (url.isEmpty()) return;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(requireContext(), "URL must start with http:// or https://", Toast.LENGTH_SHORT).show();
                return;
            }
            resolveAndDownload(url);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
