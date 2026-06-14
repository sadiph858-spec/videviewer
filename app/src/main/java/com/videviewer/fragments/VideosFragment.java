package com.videviewer.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.videviewer.R;
import com.videviewer.activities.PlayerActivity;
import com.videviewer.adapters.VideoAdapter;
import com.videviewer.database.AppDatabase;
import com.videviewer.database.VaultVideoEntity;
import com.videviewer.models.VideoItem;
import java.util.*;
import java.util.concurrent.Executors;

public class VideosFragment extends Fragment implements VideoAdapter.OnVideoClickListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private VideoAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
            tvEmpty = view.findViewById(R.id.tv_empty);
            adapter = new VideoAdapter(requireContext(), true);
            adapter.setOnVideoClickListener(this);
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            recyclerView.setAdapter(adapter);
            if (swipeRefresh != null) swipeRefresh.setOnRefreshListener(this::loadVideos);
            checkPermission();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasPermission()) loadVideos();
    }

    private boolean hasPermission() {
        String p = Build.VERSION.SDK_INT >= 33
            ? Manifest.permission.READ_MEDIA_VIDEO
            : Manifest.permission.READ_EXTERNAL_STORAGE;
        return requireContext().checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
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
                tvEmpty.setText("Permission denied!\nGo to Settings > Apps > VidViewer > Permissions > Allow Storage");
            }
        }
    }

    public void onPermissionResult() { checkPermission(); }

    private void loadVideos() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            List<VideoItem> videos = new ArrayList<>();
            try {
                // Get vault paths to exclude
                Set<String> vaultPaths = new HashSet<>();
                try {
                    AppDatabase db = AppDatabase.getInstance(requireContext());
                    List<VaultVideoEntity> vaultList = db.vaultDao().getAllSync();
                    for (VaultVideoEntity v : vaultList) {
                        if (v.originalPath != null) vaultPaths.add(v.originalPath);
                        if (v.vaultPath != null) vaultPaths.add(v.vaultPath);
                    }
                } catch (Exception e) { e.printStackTrace(); }

                // Scan MediaStore
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
                try (Cursor c = requireContext().getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        proj, MediaStore.Video.Media.SIZE + " > 0",
                        null, MediaStore.Video.Media.DATE_ADDED + " DESC")) {
                    if (c != null) {
                        while (c.moveToNext()) {
                            try {
                                String path = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                                if (vaultPaths.contains(path)) continue;
                                VideoItem v = new VideoItem();
                                v.setId(c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
                                String title = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                                String displayName = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
                                v.setTitle(title != null && !title.isEmpty() ? title :
                                    (displayName != null ? displayName.replaceFirst("[.][^.]+$","") : "Unknown"));
                                v.setDisplayName(displayName);
                                v.setPath(path);
                                v.setFolderName(c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)));
                                v.setSize(c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)));
                                v.setDuration(c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)));
                                v.setDateAdded(c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)));
                                v.setWidth(c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)));
                                v.setHeight(c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)));
                                if (path != null) {
                                    int s = path.lastIndexOf('/');
                                    if (s > 0) v.setFolderPath(path.substring(0, s));
                                }
                                videos.add(v);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }

            final List<VideoItem> result = videos;
            mainHandler.post(() -> {
                try {
                    if (!isAdded()) return;
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    adapter.submitList(result);
                    if (tvEmpty != null)
                        tvEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
                } catch (Exception e) { e.printStackTrace(); }
            });
        });
    }

    @Override
    public void onVideoClick(VideoItem video, int position) {
        try {
            Intent intent = new Intent(requireContext(), PlayerActivity.class);
            intent.putExtra("extra_video_path", video.getPath());
            intent.putExtra("video_title", video.getTitle());
            startActivity(intent);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onVideoLongClick(VideoItem video, int position) {
        try {
            VideoOptionsBottomSheet.newInstance(video)
                .show(getParentFragmentManager(), "options");
        } catch (Exception e) { e.printStackTrace(); }
    }
}
