package com.videviewer.activities;

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
import com.videviewer.database.AppDatabase;
import com.videviewer.database.PlaylistEntity;
import java.util.concurrent.Executors;

public class PlaylistActivity extends AppCompatActivity {

    private static final String TAG = "PlaylistActivity";
    private RecyclerView recyclerView;
    private View tvEmpty;
    private AppDatabase db;

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
            tvEmpty = findViewById(R.id.tv_empty);

            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
            }

            FloatingActionButton fab = findViewById(R.id.fab_create_playlist);
            if (fab != null) {
                fab.setOnClickListener(v -> showCreatePlaylistDialog());
            }

            loadPlaylists();
        } catch (Exception e) {
            Log.e(TAG, "onCreate crashed", e);
        }
    }

    private void loadPlaylists() {
        try {
            db.playlistDao().getAllPlaylists().observe(this, playlists -> {
                try {
                    boolean empty = (playlists == null || playlists.isEmpty());
                    if (tvEmpty != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    if (recyclerView != null) recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                } catch (Exception e) { Log.e(TAG, "observer error", e); }
            });
        } catch (Exception e) {
            Log.e(TAG, "loadPlaylists error", e);
        }
    }

    private void showCreatePlaylistDialog() {
        try {
            EditText etName = new EditText(this);
            etName.setHint(getString(R.string.playlist_name));

            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.create_playlist)
                .setView(etName)
                .setPositiveButton(R.string.create_playlist, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        createPlaylist(name);
                    } else {
                        Toast.makeText(this, "Please enter a playlist name", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        } catch (Exception e) { Log.e(TAG, "dialog error", e); }
    }

    private void createPlaylist(String name) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                PlaylistEntity entity = new PlaylistEntity();
                entity.name = name;
                entity.createdAt = System.currentTimeMillis();
                entity.updatedAt = System.currentTimeMillis();
                db.playlistDao().insertPlaylist(entity);
                runOnUiThread(() ->
                    Toast.makeText(this, "Playlist '" + name + "' created!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) { Log.e(TAG, "createPlaylist error", e); }
        });
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
