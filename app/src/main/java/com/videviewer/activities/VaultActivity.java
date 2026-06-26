package com.videviewer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.videviewer.R;
import com.videviewer.adapters.VideoAdapter;
import com.videviewer.database.AppDatabase;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.VaultManager;
import java.util.ArrayList;
import java.util.List;

/**
 * VaultActivity - Secure vault with lock authentication
 * Supports PIN, Password, and Pattern lock
 */
public class VaultActivity extends AppCompatActivity {

    private VaultManager vaultManager;
    private AppDatabase db;

    private View layoutLock;
    private View layoutVault;
    private View layoutNoLock;

    private TextInputEditText etPinPassword;
    private MaterialButton btnUnlock;
    private RecyclerView rvVaultVideos;
    private TextView tvEmpty;

    private static boolean isUnlocked = false; // session unlock flag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        vaultManager = VaultManager.getInstance(this);
        db = AppDatabase.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.private_vault);
        }

        layoutLock = findViewById(R.id.layout_lock);
        layoutVault = findViewById(R.id.layout_vault);
        layoutNoLock = findViewById(R.id.layout_no_lock);
        etPinPassword = findViewById(R.id.et_pin_password);
        btnUnlock = findViewById(R.id.btn_unlock);
        rvVaultVideos = findViewById(R.id.rv_vault_videos);
        tvEmpty = findViewById(R.id.tv_empty);

        // Set up vault
        MaterialButton btnSetupLock = findViewById(R.id.btn_setup_lock);
        if (btnSetupLock != null) {
            btnSetupLock.setOnClickListener(v ->
                startActivity(new Intent(this, LockSetupActivity.class)));
        }

        if (btnUnlock != null) {
            btnUnlock.setOnClickListener(v -> attemptUnlock());
        }

        determineState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        determineState();
    }

    private void determineState() {
        if (!vaultManager.isLockSet()) {
            // No lock configured
            layoutLock.setVisibility(View.GONE);
            layoutVault.setVisibility(View.GONE);
            layoutNoLock.setVisibility(View.VISIBLE);
        } else if (isUnlocked) {
            // Already unlocked in this session
            showVaultContents();
        } else {
            // Show lock screen
            layoutLock.setVisibility(View.VISIBLE);
            layoutVault.setVisibility(View.GONE);
            layoutNoLock.setVisibility(View.GONE);

            String lockType = vaultManager.getLockType();
            TextView tvLockHint = findViewById(R.id.tv_lock_hint);
            if (tvLockHint != null) {
                switch (lockType) {
                    case AppConstants.LOCK_PIN:
                        tvLockHint.setText(R.string.enter_pin);
                        break;
                    case AppConstants.LOCK_PASSWORD:
                        tvLockHint.setText(R.string.enter_password);
                        break;
                    case AppConstants.LOCK_PATTERN:
                        tvLockHint.setText(R.string.enter_pattern);
                        break;
                }
            }
        }
    }

    private void attemptUnlock() {
        if (etPinPassword == null) return;
        String input = etPinPassword.getText() != null
            ? etPinPassword.getText().toString().trim() : "";

        if (input.isEmpty()) {
            etPinPassword.setError(getString(R.string.enter_credentials));
            return;
        }

        String lockType = vaultManager.getLockType();
        boolean verified = false;

        switch (lockType) {
            case AppConstants.LOCK_PIN:
                verified = vaultManager.verifyPin(input);
                break;
            case AppConstants.LOCK_PASSWORD:
                verified = vaultManager.verifyPassword(input);
                break;
        }

        if (verified) {
            isUnlocked = true;
            showVaultContents();
        } else {
            etPinPassword.setError(getString(R.string.incorrect_credentials));
            etPinPassword.setText("");
        }
    }

    private void showVaultContents() {
        layoutLock.setVisibility(View.GONE);
        layoutNoLock.setVisibility(View.GONE);
        layoutVault.setVisibility(View.VISIBLE);

        VideoAdapter adapter = new VideoAdapter(this, true);
        rvVaultVideos.setLayoutManager(new GridLayoutManager(this, 2));
        rvVaultVideos.setAdapter(adapter);

        db.vaultDao().getAll().observe(this, vaultVideos -> {
            List<VideoItem> items = new ArrayList<>();
            if (vaultVideos != null) {
                for (var v : vaultVideos) {
                    VideoItem item = new VideoItem();
                    item.setPath(v.vaultPath);
                    item.setTitle(v.videoTitle);
                    item.setDuration(v.duration);
                    item.setSize(v.fileSize);
                    item.setInVault(true);
                    items.add(item);
                }
            }
            adapter.submitList(items);
            tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        adapter.setOnVideoClickListener(new com.videviewer.adapters.VideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(VideoItem video, int position) {
                Intent intent = new Intent(VaultActivity.this,
                    com.videviewer.activities.PlayerActivity.class);
                intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
                intent.putExtra("video_title", video.getTitle());
                intent.putExtra(AppConstants.EXTRA_FROM_VAULT, true);
                startActivity(intent);
            }
            @Override
            public void onVideoLongClick(VideoItem video, int position) {}
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Auto-lock when leaving
        isUnlocked = false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
