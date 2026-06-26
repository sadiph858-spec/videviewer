package com.videviewer.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.videviewer.R;
import com.videviewer.activities.PlayerActivity;
import com.videviewer.adapters.VideoAdapter;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.PermissionHelper;
import com.videviewer.utils.VideoScanner;
import java.util.ArrayList;
import java.util.List;

/**
 * VideosFragment - Displays all scanned videos in grid or list view
 */
public class VideosFragment extends Fragment implements VideoAdapter.OnVideoClickListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private View permissionView;

    private VideoAdapter adapter;
    private SharedPreferences prefs;
    private List<VideoItem> allVideos = new ArrayList<>();
    private String currentSort;
    private String currentView;

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

        prefs = requireContext().getSharedPreferences(AppConstants.PREFS_NAME,
            android.content.Context.MODE_PRIVATE);
        currentSort = prefs.getString(AppConstants.PREF_SORT_ORDER, AppConstants.SORT_DATE_NEW);
        currentView = prefs.getString(AppConstants.PREF_VIEW_MODE, AppConstants.VIEW_GRID);

        recyclerView = view.findViewById(R.id.rv_videos);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        tvEmpty = view.findViewById(R.id.tv_empty);
        permissionView = view.findViewById(R.id.layout_permission);

        setupAdapter();
        setupSwipeRefresh();

        View btnGrantPermission = view.findViewById(R.id.btn_grant_permission);
        if (btnGrantPermission != null) {
            btnGrantPermission.setOnClickListener(v ->
                PermissionHelper.requestStoragePermissions(requireActivity(),
                    AppConstants.REQUEST_PERMISSION_STORAGE));
        }

        loadVideos();
    }

    private void setupAdapter() {
        boolean isGrid = AppConstants.VIEW_GRID.equals(currentView);
        int spanCount = isGrid ? getResources().getInteger(R.integer.grid_columns) : 1;

        adapter = new VideoAdapter(requireContext(), isGrid);
        adapter.setOnVideoClickListener(this);

        RecyclerView.LayoutManager layoutManager = isGrid
            ? new GridLayoutManager(requireContext(), spanCount)
            : new LinearLayoutManager(requireContext());

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            loadVideos();
        });
        swipeRefresh.setColorSchemeResources(R.color.md_theme_primary);
    }

    private void loadVideos() {
        if (!PermissionHelper.hasStoragePermission(requireContext())) {
            showPermissionView(true);
            swipeRefresh.setRefreshing(false);
            return;
        }
        showPermissionView(false);

        // Async scan
        new AsyncTask<Void, Void, List<VideoItem>>() {
            @Override
            protected void onPreExecute() {
                if (!swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(true);
            }

            @Override
            protected List<VideoItem> doInBackground(Void... voids) {
                VideoScanner scanner = new VideoScanner(requireContext());
                List<VideoItem> videos = scanner.scanAllVideos();
                return scanner.sortVideos(videos, currentSort);
            }

            @Override
            protected void onPostExecute(List<VideoItem> videos) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                allVideos = videos;
                adapter.submitList(videos);
                tvEmpty.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }.execute();
    }

    private void showPermissionView(boolean show) {
        if (permissionView != null) {
            permissionView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    public void toggleViewMode() {
        currentView = AppConstants.VIEW_GRID.equals(currentView)
            ? AppConstants.VIEW_LIST : AppConstants.VIEW_GRID;
        prefs.edit().putString(AppConstants.PREF_VIEW_MODE, currentView).apply();
        setupAdapter();
        adapter.submitList(allVideos);
    }

    public void sortBy(String sortOrder) {
        currentSort = sortOrder;
        prefs.edit().putString(AppConstants.PREF_SORT_ORDER, sortOrder).apply();
        VideoScanner scanner = new VideoScanner(requireContext());
        adapter.submitList(scanner.sortVideos(allVideos, sortOrder));
    }

    public void onPermissionResult() {
        loadVideos();
    }

    // ── Video Click Callbacks ────────────────────────────────────────────────
    @Override
    public void onVideoClick(VideoItem video, int position) {
        Intent intent = new Intent(requireContext(), PlayerActivity.class);
        intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
        intent.putExtra("video_title", video.getTitle());
        startActivity(intent);
    }

    @Override
    public void onVideoLongClick(VideoItem video, int position) {
        // Show options bottom sheet
        VideoOptionsBottomSheet sheet = VideoOptionsBottomSheet.newInstance(video);
        sheet.show(getParentFragmentManager(), "video_options");
    }
}
