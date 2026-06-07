package com.videviewer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
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
import com.videviewer.utils.AppConstants;
import java.util.concurrent.Executors;

/**
 * PlaylistActivity - Create, view, manage playlists
 */
public class PlaylistActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        db = AppDatabase.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.playlists);
        }

        recyclerView = findViewById(R.id.rv_playlists);
        tvEmpty = findViewById(R.id.tv_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // FAB: create new playlist
        FloatingActionButton fab = findViewById(R.id.fab_create_playlist);
        if (fab != null) {
            fab.setOnClickListener(v -> showCreatePlaylistDialog());
        }

        loadPlaylists();
    }

    private void loadPlaylists() {
        db.playlistDao().getAllPlaylists().observe(this, playlists -> {
            tvEmpty.setVisibility(
                (playlists == null || playlists.isEmpty()) ? View.VISIBLE : View.GONE);
            // Adapter update would go here with full PlaylistAdapter implementation
        });
    }

    private void showCreatePlaylistDialog() {
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
    }

    private void createPlaylist(String name) {
        Executors.newSingleThreadExecutor().execute(() -> {
            PlaylistEntity entity = new PlaylistEntity();
            entity.name = name;
            entity.createdAt = System.currentTimeMillis();
            entity.updatedAt = System.currentTimeMillis();
            db.playlistDao().insertPlaylist(entity);
            runOnUiThread(() ->
                Toast.makeText(this, "Playlist '" + name + "' created!", Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
