package com.videviewer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.videviewer.R;
import com.videviewer.activities.PlayerActivity;
import com.videviewer.adapters.VideoAdapter;
import com.videviewer.database.AppDatabase;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import java.util.ArrayList;
import java.util.List;

/**
 * RecentFragment - Shows recently watched videos from history
 */
public class RecentFragment extends Fragment implements VideoAdapter.OnVideoClickListener {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private VideoAdapter adapter;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        recyclerView = view.findViewById(R.id.rv_recent);
        tvEmpty = view.findViewById(R.id.tv_empty);

        adapter = new VideoAdapter(requireContext(), false);
        adapter.setOnVideoClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        db.historyDao().getRecent(AppConstants.MAX_RECENT_SIZE).observe(getViewLifecycleOwner(),
            historyItems -> {
                List<VideoItem> items = new ArrayList<>();
                if (historyItems != null) {
                    for (var h : historyItems) {
                        VideoItem item = new VideoItem();
                        item.setPath(h.videoPath);
                        item.setTitle(h.videoTitle);
                        item.setDuration(h.videoDuration);
                        item.setLastWatched(h.lastWatched);
                        item.setResumePosition(h.resumePosition);
                        items.add(item);
                    }
                }
                adapter.submitList(items);
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
    }

    @Override
    public void onVideoClick(VideoItem video, int position) {
        Intent intent = new Intent(requireContext(), PlayerActivity.class);
        intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
        intent.putExtra("video_title", video.getTitle());
        intent.putExtra("resume_position", video.getResumePosition());
        startActivity(intent);
    }

    @Override
    public void onVideoLongClick(VideoItem video, int position) {
        VideoOptionsBottomSheet.newInstance(video)
            .show(getParentFragmentManager(), "options");
    }
}
