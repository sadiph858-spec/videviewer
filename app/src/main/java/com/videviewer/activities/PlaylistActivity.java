package com.videviewer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.videviewer.R;
import com.videviewer.adapters.PlaylistAdapter;
import com.videviewer.database.AppDatabase;
import com.videviewer.database.PlaylistEntity;
import java.util.List;
import java.util.concurrent.Executors;

public class PlaylistActivity extends AppCompatActivity {

    private static final String TAG = "PlaylistActivity";
    private RecyclerView recyclerView;
    private View tvEmpty;
    private AppDatabase db;
    private PlaylistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_playlist);
            db = AppDatabase.getInstance(this);

            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle(R.string.playlists);
                }
            }

            recyclerView = findViewById(R.id.rv_playlists);
            tvEmpty      = findViewById(R.id.tv_empty);

            adapter = new PlaylistAdapter(this);
            adapter.setListener(new PlaylistAdapter.OnPlaylistClickListener() {
                @Override
                public void onPlaylistClick(PlaylistEntity playlist) {
                    Intent i = new Intent(PlaylistActivity.this, PlaylistDetailActivity.class);
                    i.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.id);
                    i.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, playlist.name);
                    startActivity(i);
                }
                @Override
                public void onPlaylistLongClick(PlaylistEntity playlist) {
                    showPlaylistOptions(playlist);
                }
            });

            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);
            }

            FloatingActionButton fab = findViewById(R.id.fab_create_playlist);
            if (fab != null) fab.setOnClickListener(v -> showCreatePlaylistDialog());

            loadPlaylists();
        } catch (Exception e) {
            Log.e(TAG, "onCreate crashed", e);
        }
    }

    @Override
    protected void onResume() { super.onResume(); loadPlaylists(); }

    private void loadPlaylists() {
        try {
            db.playlistDao().getAllPlaylists().observe(this, playlists -> {
                try {
                    if (playlists == null) return;
                    adapter.submitList(playlists);
                    if (tvEmpty != null)
                        tvEmpty.setVisibility(playlists.isEmpty() ? View.VISIBLE : View.GONE);
                    // Load video counts in background
                    Executors.newSingleThreadExecutor().execute(() -> {
                        for (PlaylistEntity p : playlists) {
                            int count = db.playlistDao().getVideoCountForPlaylist(p.id);
                            runOnUiThread(() -> adapter.setVideoCount(p.id, count));
                        }
                    });
                } catch (Exception e) { Log.e(TAG, "loadPlaylists observer", e); }
            });
        } catch (Exception e) {
            Log.e(TAG, "loadPlaylists", e);
        }
    }

    private void showCreatePlaylistDialog() {
        try {
            EditText et = new EditText(this);
            et.setHint("Playlist name");
            new MaterialAlertDialogBuilder(this)
                .setTitle("Create Playlist")
                .setView(et)
                .setPositiveButton("Create", (d, w) -> {
                    String name = et.getText() != null ? et.getText().toString().trim() : "";
                    if (name.isEmpty()) { Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show(); return; }
                    Executors.newSingleThreadExecutor().execute(() -> {
                        PlaylistEntity p = new PlaylistEntity();
                        p.name = name;
                        p.createdAt = System.currentTimeMillis();
                        p.updatedAt = System.currentTimeMillis();
                        db.playlistDao().insertPlaylist(p);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
        } catch (Exception e) { Log.e(TAG, "showCreatePlaylistDialog", e); }
    }

    private void showPlaylistOptions(PlaylistEntity playlist) {
        String[] options = {"Rename", "Delete"};
        new MaterialAlertDialogBuilder(this)
            .setTitle(playlist.name)
            .setItems(options, (d, which) -> {
                if (which == 0) {
                    // Rename
                    EditText et = new EditText(this);
                    et.setText(playlist.name);
                    new MaterialAlertDialogBuilder(this)
                        .setTitle("Rename Playlist")
                        .setView(et)
                        .setPositiveButton("Save", (dd, ww) -> {
                            String newName = et.getText() != null ? et.getText().toString().trim() : "";
                            if (!newName.isEmpty()) {
                                playlist.name = newName;
                                playlist.updatedAt = System.currentTimeMillis();
                                Executors.newSingleThreadExecutor().execute(() ->
                                    db.playlistDao().updatePlaylist(playlist));
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else {
                    // Delete
                    new MaterialAlertDialogBuilder(this)
                        .setTitle("Delete Playlist?")
                        .setMessage("\"" + playlist.name + "\" will be deleted. Videos won\'t be deleted.")
                        .setPositiveButton("Delete", (dd, ww) ->
                            Executors.newSingleThreadExecutor().execute(() ->
                                db.playlistDao().deleteById(playlist.id)))
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            })
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
