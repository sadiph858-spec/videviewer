package com.videviewer.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.videviewer.R;
import com.videviewer.activities.PlayerActivity;
import com.videviewer.adapters.VideoAdapter;
import com.videviewer.database.AppDatabase;
import com.videviewer.database.VaultVideoEntity;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import java.util.*;
import java.util.concurrent.Executors;

public class VideosFragment extends Fragment implements VideoAdapter.OnVideoClickListener {

    private static final String TAG = "VideosFragment";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private EditText etSearch;
    private VideoAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Full unfiltered list — search filters from this */
    private final List<VideoItem> allVideos = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_videos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            recyclerView = view.findViewById(R.id.rv_videos);
            swipeRefresh = view.findViewById(R.id.swipe_refresh);
            tvEmpty      = view.findViewById(R.id.tv_empty);
            etSearch     = view.findViewById(R.id.et_search);

            adapter = new VideoAdapter(requireContext(), false);
            adapter.setOnVideoClickListener(this);

            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                recyclerView.setAdapter(adapter);
            }

            if (swipeRefresh != null) swipeRefresh.setOnRefreshListener(this::loadVideos);

            // ── Search / filter ──────────────────────────────────
            if (etSearch != null) {
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                    @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                    @Override public void afterTextChanged(Editable s) {
                        applySearch(s.toString().trim());
                    }
                });
            }

            checkPermission();
        } catch (Exception e) { Log.e(TAG, "onViewCreated", e); }
    }

    /** Filter allVideos by query and push to adapter */
    private void applySearch(String query) {
        try {
            if (query.isEmpty()) {
                adapter.submitList(new ArrayList<>(allVideos));
                if (tvEmpty != null)
                    tvEmpty.setVisibility(allVideos.isEmpty() ? View.VISIBLE : View.GONE);
                return;
            }
            String lower = query.toLowerCase(Locale.getDefault());
            List<VideoItem> filtered = new ArrayList<>();
            for (VideoItem v : allVideos) {
                String title  = v.getTitle()  != null ? v.getTitle().toLowerCase()  : "";
                String folder = v.getFolderName() != null ? v.getFolderName().toLowerCase() : "";
                if (title.contains(lower) || folder.contains(lower)) {
                    filtered.add(v);
                }
            }
            adapter.submitList(filtered);
            if (tvEmpty != null) {
                if (filtered.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("No results for "" + query + """);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) { Log.e(TAG, "applySearch", e); }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasPermission()) loadVideos();
    }

    private boolean hasPermission() {
        try {
            String p = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_VIDEO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
            return requireContext().checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) { return false; }
    }

    private void checkPermission() {
        if (hasPermission()) {
            loadVideos();
        } else {
            String p = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_VIDEO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
            requestPermissions(new String[]{p}, 200);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(code, p, r);
        if (code == 200 && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) {
            loadVideos();
        } else {
            if (tvEmpty != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Storage permission required.\nGo to Settings > Apps > VidViewer > Permissions > Allow Storage");
            }
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
        }
    }

    public void onPermissionResult() { checkPermission(); }

    private void loadVideos() {
        if (!isAdded()) return;
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);

        final Context appCtx;
        try { appCtx = requireContext().getApplicationContext(); }
        catch (Exception e) {
            Log.e(TAG, "Cannot get context", e);
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            List<VideoItem> videos = new ArrayList<>();
            try {
                Set<String> vaultPaths = new HashSet<>();
                try {
                    List<VaultVideoEntity> vaultList = AppDatabase.getInstance(appCtx).vaultDao().getAllSync();
                    for (VaultVideoEntity v : vaultList) {
                        if (v.originalPath != null) vaultPaths.add(v.originalPath);
                        if (v.vaultPath    != null) vaultPaths.add(v.vaultPath);
                    }
                } catch (Exception e) { Log.w(TAG, "vault exclude error", e); }

                String[] proj = {
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.TITLE,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.WIDTH,
                    MediaStore.Video.Media.HEIGHT,
                };

                try (Cursor c = appCtx.getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        proj,
                        MediaStore.Video.Media.SIZE + " > 0",
                        null,
                        MediaStore.Video.Media.DATE_ADDED + " DESC")) {

                    if (c != null) {
                        Log.d(TAG, "MediaStore rows: " + c.getCount());
                        while (c.moveToNext()) {
                            try {
                                String path = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                                if (path == null || vaultPaths.contains(path)) continue;

                                VideoItem v = new VideoItem();
                                v.setId(c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
                                String title       = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                                String displayName = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
                                v.setTitle(title != null && !title.isEmpty() ? title :
                                    (displayName != null ? displayName.replaceFirst("[.][^.]+$", "") : "Unknown"));
                                v.setDisplayName(displayName);
                                v.setPath(path);
                                v.setFolderName(c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)));
                                v.setSize(c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)));
                                v.setDuration(c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)));
                                v.setDateAdded(c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)));
                                v.setWidth(c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)));
                                v.setHeight(c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)));
                                int slash = path.lastIndexOf('/');
                                if (slash > 0) v.setFolderPath(path.substring(0, slash));
                                videos.add(v);
                            } catch (Exception e) { Log.w(TAG, "row parse error", e); }
                        }
                    } else {
                        Log.w(TAG, "MediaStore cursor is null");
                    }
                }
            } catch (Exception e) { Log.e(TAG, "loadVideos bg error", e); }

            Log.d(TAG, "Videos found: " + videos.size());

            mainHandler.post(() -> {
                try {
                    if (!isAdded()) return;
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                    // Store full list for search filtering
                    allVideos.clear();
                    allVideos.addAll(videos);

                    // Apply any active search query
                    String currentQuery = (etSearch != null && etSearch.getText() != null)
                        ? etSearch.getText().toString().trim() : "";
                    applySearch(currentQuery);
                } catch (Exception e) { Log.e(TAG, "UI update error", e); }
            });
        });
    }

    @Override
    public void onVideoClick(VideoItem video, int position) {
        try {
            Intent intent = new Intent(requireContext(), PlayerActivity.class);
            intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
            intent.putExtra(AppConstants.EXTRA_VIDEO_TITLE, video.getTitle());
            startActivity(intent);
        } catch (Exception e) { Log.e(TAG, "onVideoClick", e); }
    }

    @Override
    public void onVideoLongClick(VideoItem video, int position) {
        try {
            VideoOptionsBottomSheet.newInstance(video)
                .show(getParentFragmentManager(), "options");
        } catch (Exception e) { Log.e(TAG, "onVideoLongClick", e); }
    }
}
