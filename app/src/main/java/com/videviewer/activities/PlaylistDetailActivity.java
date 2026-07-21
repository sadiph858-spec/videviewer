package com.videviewer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.videviewer.R;
import com.videviewer.adapters.VideoAdapter;
import com.videviewer.database.AppDatabase;
import com.videviewer.database.PlaylistVideoEntity;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class PlaylistDetailActivity extends AppCompatActivity
        implements VideoAdapter.OnVideoClickListener {

    public static final String EXTRA_PLAYLIST_ID   = "extra_playlist_id";
    public static final String EXTRA_PLAYLIST_NAME = "extra_playlist_name";

    private RecyclerView recyclerView;
    private View tvEmpty;
    private VideoAdapter adapter;
    private AppDatabase db;
    private long playlistId;
    private final List<VideoItem> videoItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_playlist_detail);
            db         = AppDatabase.getInstance(this);
            playlistId = getIntent().getLongExtra(EXTRA_PLAYLIST_ID, -1);
            String name = getIntent().getStringExtra(EXTRA_PLAYLIST_NAME);

            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle(name != null ? name : "Playlist");
                }
            }

            recyclerView = findViewById(R.id.rv_playlist_videos);
            tvEmpty      = findViewById(R.id.tv_empty);

            adapter = new VideoAdapter(this, false);
            adapter.setOnVideoClickListener(this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);

            loadVideos();
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    @Override
    protected void onResume() { super.onResume(); loadVideos(); }

    private void loadVideos() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<PlaylistVideoEntity> pvList = db.playlistDao().getPlaylistVideos(playlistId);
                List<VideoItem> items = new ArrayList<>();
                for (PlaylistVideoEntity pv : pvList) {
                    File f = new File(pv.videoPath);
                    VideoItem item = new VideoItem();
                    item.setPath(pv.videoPath);
                    if (f.exists()) {
                        String n = f.getName(); int dot = n.lastIndexOf('.');
                        item.setTitle(dot > 0 ? n.substring(0, dot) : n);
                    } else {
                        item.setTitle(pv.videoPath.substring(pv.videoPath.lastIndexOf('/') + 1));
                    }
                    items.add(item);
                }
                videoItems.clear(); videoItems.addAll(items);
                runOnUiThread(() -> {
                    adapter.submitList(new ArrayList<>(items));
                    if (tvEmpty != null)
                        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @Override
    public void onVideoClick(VideoItem video, int position) {
        try {
            ArrayList<String> paths = new ArrayList<>();
            for (VideoItem v : videoItems) paths.add(v.getPath());
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
            intent.putExtra(AppConstants.EXTRA_VIDEO_TITLE, video.getTitle());
            intent.putStringArrayListExtra("video_list", paths);
            intent.putExtra("current_index", position);
            startActivity(intent);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onVideoLongClick(VideoItem video, int position) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Remove from playlist?")
            .setMessage(video.getTitle())
            .setPositiveButton("Remove", (d, w) ->
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        db.playlistDao().removeVideoFromPlaylist(playlistId, video.getPath());
                        runOnUiThread(this::loadVideos);
                    } catch (Exception e) { e.printStackTrace(); }
                }))
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
