package com.videviewer.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
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
 * SearchActivity - Real-time video search from scanned library
 */
public class SearchActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    private RecyclerView recyclerView;
    private TextView tvNoResults;
    private VideoAdapter adapter;
    private List<VideoItem> allVideos = new ArrayList<>();
    private VideoScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.search);
        }

        recyclerView = findViewById(R.id.rv_search_results);
        tvNoResults = findViewById(R.id.tv_no_results);
        com.google.android.material.textfield.TextInputEditText etSearch = findViewById(R.id.et_search);

        scanner = new VideoScanner(this);
        adapter = new VideoAdapter(this, new ArrayList<>());
        adapter.setOnVideoClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load all videos first
        new AsyncTask<Void, Void, List<VideoItem>>() {
            @Override
            protected List<VideoItem> doInBackground(Void... v) {
                return scanner.scanAllVideos();
            }
            @Override
            protected void onPostExecute(List<VideoItem> videos) {
                allVideos = videos;
            }
        }.execute();

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    performSearch(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            // Auto-focus keyboard
            etSearch.requestFocus();
        }
    }

    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            adapter.submitList(new ArrayList<>());
            tvNoResults.setVisibility(View.GONE);
            return;
        }
        List<VideoItem> results = scanner.searchVideos(allVideos, query);
        adapter.submitList(results);
        tvNoResults.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onVideoClick(VideoItem video, int position) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
        intent.putExtra("video_title", video.getTitle());
        startActivity(intent);
    }

    @Override
    public void onVideoLongClick(VideoItem video, int position) {}

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
