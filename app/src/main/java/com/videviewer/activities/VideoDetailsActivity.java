package com.videviewer.activities;

import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.videviewer.R;
import com.videviewer.utils.AppConstants;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * VideoDetailsActivity - Shows detailed metadata about a video file
 */
public class VideoDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_details);

        String videoPath = getIntent().getStringExtra(AppConstants.EXTRA_VIDEO_PATH);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.video_details);
        }

        if (videoPath != null) {
            loadVideoDetails(videoPath);
        }
    }

    private void loadVideoDetails(String path) {
        ImageView ivThumbnail = findViewById(R.id.iv_detail_thumbnail);
        TextView tvTitle = findViewById(R.id.tv_detail_title);
        TextView tvPath = findViewById(R.id.tv_detail_path);
        TextView tvSize = findViewById(R.id.tv_detail_size);
        TextView tvDuration = findViewById(R.id.tv_detail_duration);
        TextView tvResolution = findViewById(R.id.tv_detail_resolution);
        TextView tvDateAdded = findViewById(R.id.tv_detail_date);
        TextView tvMimeType = findViewById(R.id.tv_detail_mime);

        // Load thumbnail
        Glide.with(this).load(path)
            .placeholder(R.drawable.ic_video_placeholder)
            .into(ivThumbnail);

        // Load metadata asynchronously
        new AsyncTask<String, Void, String[]>() {
            @Override
            protected String[] doInBackground(String... paths) {
                String p = paths[0];
                File file = new File(p);
                String title = file.getName();
                String filePath = p;
                String size = formatSize(file.length());
                String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(new Date(file.lastModified()));

                String duration = "Unknown";
                String resolution = "Unknown";
                String mimeType = "video/*";

                try {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(p);
                    String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (dur != null) {
                        long ms = Long.parseLong(dur);
                        long s = ms / 1000;
                        long h = s / 3600; long m = (s % 3600) / 60; long sec = s % 60;
                        duration = h > 0 ? String.format("%d:%02d:%02d", h, m, sec)
                            : String.format("%02d:%02d", m, sec);
                    }
                    String w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String h2 = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    if (w != null && h2 != null) resolution = w + " × " + h2;
                    String mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
                    if (mime != null) mimeType = mime;
                    retriever.release();
                } catch (Exception ignored) {}

                return new String[]{title, filePath, size, duration, resolution, date, mimeType};
            }

            @Override
            protected void onPostExecute(String[] info) {
                if (tvTitle != null) tvTitle.setText(info[0]);
                if (tvPath != null) tvPath.setText(info[1]);
                if (tvSize != null) tvSize.setText(info[2]);
                if (tvDuration != null) tvDuration.setText(info[3]);
                if (tvResolution != null) tvResolution.setText(info[4]);
                if (tvDateAdded != null) tvDateAdded.setText(info[5]);
                if (tvMimeType != null) tvMimeType.setText(info[6]);
            }
        }.execute(path);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
