package com.videviewer.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.videviewer.R;
import com.videviewer.adapters.VideoAdapter;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.VideoScanner;
import java.util.ArrayList;
import java.util.List;

/**
 * FolderActivity - Displays all videos inside a specific folder
 */
public class FolderActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private VideoAdapter adapter;
    private String folderPath;
    private String folderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);

        folderPath = getIntent().getStringExtra(AppConstants.EXTRA_FOLDER_PATH);
        folderName = getIntent().getStringExtra("folder_name");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(folderName != null ? folderName : getString(R.string.folder_browser));
        }

        recyclerView = findViewById(R.id.rv_folder_videos);
        tvEmpty = findViewById(R.id.tv_empty);

        adapter = new VideoAdapter(this, true);
        adapter.setOnVideoClickListener(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        loadFolderVideos();
    }

    private void loadFolderVideos() {
        new AsyncTask<Void, Void, List<VideoItem>>() {
            @Override
            protected List<VideoItem> doInBackground(Void... v) {
                VideoScanner scanner = new VideoScanner(FolderActivity.this);
                List<VideoItem> all = scanner.scanAllVideos();
                List<VideoItem> folderVideos = new ArrayList<>();
                for (VideoItem video : all) {
                    if (folderPath != null && folderPath.equals(video.getFolderPath())) {
                        folderVideos.add(video);
                    }
                }
                return folderVideos;
            }

            @Override
            protected void onPostExecute(List<VideoItem> videos) {
                adapter.submitList(videos);
                tvEmpty.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }.execute();
    }

    @Override
    public void onVideoClick(VideoItem video, int position) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
        intent.putExtra("video_title", video.getTitle());
        startActivity(intent);
    }

    @Override
    public void onVideoLongClick(VideoItem video, int position) {
        com.videviewer.fragments.VideoOptionsBottomSheet.newInstance(video)
            .show(getSupportFragmentManager(), "options");
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
