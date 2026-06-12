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
import java.util.ArrayList;
import java.util.List;

public class RecentFragment extends Fragment implements VideoAdapter.OnVideoClickListener {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private VideoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            recyclerView = view.findViewById(R.id.rv_recent);
            tvEmpty = view.findViewById(R.id.tv_empty);
            adapter = new VideoAdapter(requireContext(), false);
            adapter.setOnVideoClickListener(this);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerView.setAdapter(adapter);
            loadHistory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadHistory() {
        try {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.historyDao().getRecent(20).observe(getViewLifecycleOwner(), historyItems -> {
                try {
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
                    if (tvEmpty != null) {
                        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onVideoClick(VideoItem video, int position) {
        try {
            Intent intent = new Intent(requireContext(), PlayerActivity.class);
            intent.putExtra("extra_video_path", video.getPath());
            intent.putExtra("video_title", video.getTitle());
            intent.putExtra("resume_position", video.getResumePosition());
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onVideoLongClick(VideoItem video, int position) {}
}
