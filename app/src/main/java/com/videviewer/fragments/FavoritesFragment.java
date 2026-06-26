package com.videviewer.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
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
 * FavoritesFragment - Shows favorited videos from Room DB
 */
public class FavoritesFragment extends Fragment implements VideoAdapter.OnVideoClickListener {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private VideoAdapter adapter;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(requireContext());

        recyclerView = view.findViewById(R.id.rv_favorites);
        tvEmpty = view.findViewById(R.id.tv_empty);

        adapter = new VideoAdapter(requireContext(), false);
        adapter.setOnVideoClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        // Observe Room LiveData
        db.favoriteDao().getAll().observe(getViewLifecycleOwner(), favorites -> {
            List<VideoItem> items = new ArrayList<>();
            if (favorites != null) {
                for (var fav : favorites) {
                    VideoItem item = new VideoItem();
                    item.setPath(fav.videoPath);
                    item.setTitle(fav.videoTitle);
                    item.setDuration(fav.videoDuration);
                    item.setSize(fav.videoSize);
                    item.setFavorite(true);
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
        startActivity(intent);
    }

    @Override
    public void onVideoLongClick(VideoItem video, int position) {
        VideoOptionsBottomSheet.newInstance(video)
            .show(getParentFragmentManager(), "options");
    }
}
