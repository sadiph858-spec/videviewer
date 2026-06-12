package com.videviewer.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.videviewer.R;
import com.videviewer.activities.PlayerActivity;
import com.videviewer.adapters.VideoAdapter;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.VideoScanner;
import java.util.List;
import java.util.concurrent.Executors;

public class VideosFragment extends Fragment implements VideoAdapter.OnVideoClickListener {

    private static final int PERMISSION_REQUEST = 100;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View tvEmpty;           // LinearLayout in XML — keep as View, not TextView
    private View layoutPermission;  // LinearLayout in XML
    private VideoAdapter adapter;

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
            recyclerView   = view.findViewById(R.id.rv_videos);
            swipeRefresh   = view.findViewById(R.id.swipe_refresh);
            tvEmpty        = view.findViewById(R.id.tv_empty);
            layoutPermission = view.findViewById(R.id.layout_permission);

            adapter = new VideoAdapter(requireContext(), false);
            adapter.setOnVideoClickListener(this);
            adapter.setOnVideoMenuClickListener((video, position, anchor) -> {
                try {
                    VideoOptionsBottomSheet.newInstance(video)
                            .show(getParentFragmentManager(), "options");
                } catch (Exception e) { e.printStackTrace(); }
            });

            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerView.setAdapter(adapter);

            if (swipeRefresh != null) {
                swipeRefresh.setOnRefreshListener(this::loadVideos);
                swipeRefresh.setColorSchemeResources(R.color.accent_primary);
            }

            Button btnGrant = view.findViewById(R.id.btn_grant_permission);
            if (btnGrant != null) {
                btnGrant.setOnClickListener(v -> requestStoragePermission());
            }

            checkPermissionAndLoad();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (hasStoragePermission()) loadVideos();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasStoragePermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            } else {
                return ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void requestStoragePermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_MEDIA_VIDEO}, PERMISSION_REQUEST);
            } else {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkPermissionAndLoad() {
        try {
            if (hasStoragePermission()) {
                setVisibility(layoutPermission, View.GONE);
                setVisibility(swipeRefresh, View.VISIBLE);
                loadVideos();
            } else {
                setVisibility(layoutPermission, View.VISIBLE);
                setVisibility(swipeRefresh, View.GONE);
                setVisibility(tvEmpty, View.GONE);
                requestStoragePermission();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadVideos() {
        try {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
            Executors.newSingleThreadExecutor().execute(() -> {
                List<VideoItem> videos = null;
                try {
                    VideoScanner scanner = new VideoScanner(requireContext());
                    videos = scanner.scanAllVideos();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                final List<VideoItem> finalVideos = videos;
                try {
                    requireActivity().runOnUiThread(() -> {
                        try {
                            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                            if (finalVideos != null) {
                                adapter.submitList(finalVideos);
                                setVisibility(tvEmpty, finalVideos.isEmpty() ? View.VISIBLE : View.GONE);
                            } else {
                                setVisibility(tvEmpty, View.VISIBLE);
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
        }
    }

    /** Called by MainActivity after permission result */
    public void onPermissionResult() {
        checkPermissionAndLoad();
    }

    private void setVisibility(View v, int vis) {
        if (v != null) v.setVisibility(vis);
    }

    @Override
    public void onVideoClick(VideoItem video, int position) {
        try {
            Intent intent = new Intent(requireContext(), PlayerActivity.class);
            intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPlaybackUri());
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
