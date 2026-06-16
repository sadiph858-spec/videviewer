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
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadsFragment extends Fragment {

    private FragmentDownloadsBinding binding;
    private DownloadAdapter adapter;
    private final List<DownloadItem> downloadList = new ArrayList<>();
    // url → list position for in-progress items
    private final Map<String, Integer> activeByUrl = new HashMap<>();

    // ── LocalBroadcast receiver ───────────────────────────────────
    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (binding == null) return;
            String action   = intent.getAction();
            String url      = intent.getStringExtra(DownloadService.EXTRA_URL);
            String filename = intent.getStringExtra(DownloadService.EXTRA_FILENAME);
            int    progress = intent.getIntExtra(DownloadService.EXTRA_PROGRESS, 0);
            double speed    = intent.getDoubleExtra(DownloadService.EXTRA_SPEED, 0);

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
                    // Not tracked in list yet — add it
                    DownloadItem done = new DownloadItem(filepath, filename);
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
        binding.recyclerDownloads.setLayoutManager(
            new LinearLayoutManager(requireContext()));
        binding.recyclerDownloads.setAdapter(adapter);

        // Tap completed item → play it
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

    @Override
    public void onResume() {
        super.onResume();
        // Register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_PROGRESS);
        filter.addAction(DownloadService.ACTION_COMPLETE);
        filter.addAction(DownloadService.ACTION_FAILED);
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(downloadReceiver, filter);

        // Scan existing downloaded files
        scanDownloadedFiles();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(downloadReceiver);
    }

    /** Scan Downloads/VidViewer/ for video files not yet shown */
    private void scanDownloadedFiles() {
        File dir = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            AppConstants.DOWNLOAD_DIR);
        if (!dir.exists()) { updateEmpty(); return; }

        // Build set of already-tracked file paths
        java.util.Set<String> tracked = new java.util.HashSet<>();
        for (DownloadItem d : downloadList) {
            if (d.filePath != null) tracked.add(d.filePath);
        }

        File[] files = dir.listFiles();
        if (files == null) { updateEmpty(); return; }
        int inserted = 0;
        for (File f : files) {
            if (!f.isFile()) continue;
            String name = f.getName().toLowerCase();
            if (!name.endsWith(".mp4") && !name.endsWith(".mkv")
                && !name.endsWith(".webm") && !name.endsWith(".avi")
                && !name.endsWith(".mov") && !name.endsWith(".3gp"))
                continue;
            if (tracked.contains(f.getAbsolutePath())) continue;

            DownloadItem item = new DownloadItem(f.getAbsolutePath(), f.getName());
            downloadList.add(item);
            inserted++;
        }
        if (inserted > 0) adapter.notifyDataSetChanged();
        updateEmpty();
    }

    private void updateEmpty() {
        if (binding == null) return;
        binding.tvEmpty.setVisibility(downloadList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddDownloadDialog() {
        android.app.AlertDialog.Builder builder =
            new android.app.AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme);
        builder.setTitle("Download Video");

        final EditText et = new EditText(requireContext());
        et.setHint("Paste direct video URL (.mp4, .mkv, .webm)");
        et.setTextColor(0xFFFFFFFF);
        et.setHintTextColor(0xFF888888);
        et.setPadding(40, 20, 40, 20);
        builder.setView(et);

        builder.setPositiveButton("Download", (d, w) -> {
            String url = et.getText().toString().trim();
            if (url.isEmpty()) return;

            // Detect YouTube / unsupported sites
            if (url.contains("youtube.com") || url.contains("youtu.be")
                || url.contains("facebook.com") || url.contains("instagram.com")
                || url.contains("tiktok.com")) {
                Toast.makeText(requireContext(),
                    "YouTube/social media links need a direct video URL.\nUse a downloader site to get the .mp4 link.",
                    Toast.LENGTH_LONG).show();
                return;
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(requireContext(), "URL must start with http:// or https://", Toast.LENGTH_SHORT).show();
                return;
            }
            startDownload(url);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void startDownload(String url) {
        // Check if already downloading this URL
        if (activeByUrl.containsKey(url)) {
            Toast.makeText(requireContext(), "Already downloading this URL", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), DownloadService.class);
        intent.putExtra("url", url);
        requireContext().startForegroundService(intent);

        DownloadItem item = new DownloadItem(url, 0, DownloadItem.STATUS_DOWNLOADING);
        int pos = 0;
        downloadList.add(pos, item);
        activeByUrl.put(url, pos);
        adapter.notifyItemInserted(pos);
        binding.tvEmpty.setVisibility(View.GONE);
        Toast.makeText(requireContext(), "Download started…", Toast.LENGTH_SHORT).show();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
