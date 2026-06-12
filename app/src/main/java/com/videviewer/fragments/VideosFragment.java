package com.videviewer.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.videviewer.R;
import com.videviewer.activities.PlayerActivity;
import com.videviewer.adapters.VideoAdapter;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.VideoScanner;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class VideosFragment extends Fragment implements VideoAdapter.OnVideoClickListener {

    private static final int PERMISSION_REQUEST = 100;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private LinearLayout layoutPermission;
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
            recyclerView = view.findViewById(R.id.rv_videos);
            swipeRefresh = view.findViewById(R.id.swipe_refresh);
            tvEmpty = view.findViewById(R.id.tv_empty);
            layoutPermission = view.findViewById(R.id.layout_permission);

            adapter = new VideoAdapter(requireContext(), true);
            adapter.setOnVideoClickListener(this);
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            recyclerView.setAdapter(adapter);

            if (swipeRefresh != null) {
                swipeRefresh.setOnRefreshListener(this::loadVideos);
            }

            // Grant permission button
            Button btnGrant = view.findViewById(R.id.btn_grant_permission);
            if (btnGrant != null) {
                btnGrant.setOnClickListener(v -> requestPermission());
            }

            checkPermissionAndLoad();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-check permission when returning from settings
        if (hasPermission()) {
            if (layoutPermission != null) layoutPermission.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            loadVideos();
        }
    }

    private boolean hasPermission() {
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

    private void requestPermission() {
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
            if (hasPermission()) {
                if (layoutPermission != null) layoutPermission.setVisibility(View.GONE);
                if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                loadVideos();
            } else {
                // Show permission UI
                if (layoutPermission != null) layoutPermission.setVisibility(View.VISIBLE);
                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                requestPermission();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadVideos() {
        try {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    VideoScanner scanner = new VideoScanner(requireContext());
                    List<VideoItem> videos = scanner.scanAllVideos();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                                adapter.submitList(videos);
                                if (tvEmpty != null) {
                                    tvEmpty.setVisibility(
                                        videos.isEmpty() ? View.VISIBLE : View.GONE);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                        });
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPermissionResult() {
        checkPermissionAndLoad();
    }

    @Override
    public void onVideoClick(VideoItem video, int position) {
        try {
            Intent intent = new Intent(requireContext(), PlayerActivity.class);
            intent.putExtra("extra_video_path", video.getPath());
            intent.putExtra("video_title", video.getTitle());
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onVideoLongClick(VideoItem video, int position) {
        try {
            VideoOptionsBottomSheet.newInstance(video)
                .show(getParentFragmentManager(), "options");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
