package com.videviewer.activities;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.videviewer.R;
import com.videviewer.adapters.VideoAdapter;
import com.videviewer.database.AppDatabase;
import com.videviewer.database.VaultVideoEntity;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.VaultManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * VaultActivity - Secure vault with PIN/Password lock authentication.
 * Unlocked videos are shown in a 2-column grid.
 * Long-press any video to restore it to gallery or permanently delete it.
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

    private VideoAdapter adapter;

    // Static flag so the vault stays unlocked while the user is in PlayerActivity
    // and returns here. It is reset in onStop() to auto-lock when truly leaving.
    private static boolean isUnlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_vault);

            vaultManager = VaultManager.getInstance(this);
            db = AppDatabase.getInstance(this);

            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle(R.string.private_vault);
                }
            }

            layoutLock = findViewById(R.id.layout_lock);
            layoutVault = findViewById(R.id.layout_vault);
            layoutNoLock = findViewById(R.id.layout_no_lock);
            etPinPassword = findViewById(R.id.et_pin_password);
            btnUnlock = findViewById(R.id.btn_unlock);
            rvVaultVideos = findViewById(R.id.rv_vault_videos);
            tvEmpty = findViewById(R.id.tv_empty);

            MaterialButton btnSetupLock = findViewById(R.id.btn_setup_lock);
            if (btnSetupLock != null) {
                btnSetupLock.setOnClickListener(v ->
                    startActivity(new Intent(this, LockSetupActivity.class)));
            }

            if (btnUnlock != null) {
                btnUnlock.setOnClickListener(v -> attemptUnlock());
            }

            if (etPinPassword != null) {
                etPinPassword.setOnEditorActionListener((v, actionId, event) -> {
                    attemptUnlock();
                    return true;
                });
            }

            determineState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            determineState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void determineState() {
        try {
            if (!vaultManager.isLockSet()) {
                safeSetVisibility(layoutLock, View.GONE);
                safeSetVisibility(layoutVault, View.GONE);
                safeSetVisibility(layoutNoLock, View.VISIBLE);
            } else if (isUnlocked) {
                showVaultContents();
            } else {
                safeSetVisibility(layoutLock, View.VISIBLE);
                safeSetVisibility(layoutVault, View.GONE);
                safeSetVisibility(layoutNoLock, View.GONE);

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
                        default:
                            tvLockHint.setText(R.string.enter_pin);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void attemptUnlock() {
        try {
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
                default:
                    verified = vaultManager.verifyPin(input);
            }

            if (verified) {
                isUnlocked = true;
                etPinPassword.setError(null);
                showVaultContents();
            } else {
                etPinPassword.setError(getString(R.string.incorrect_credentials));
                etPinPassword.setText("");
                etPinPassword.requestFocus();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showVaultContents() {
        try {
            safeSetVisibility(layoutLock, View.GONE);
            safeSetVisibility(layoutNoLock, View.GONE);
            safeSetVisibility(layoutVault, View.VISIBLE);

            if (rvVaultVideos == null) return;

            // 2-column grid layout
            if (rvVaultVideos.getLayoutManager() == null) {
                rvVaultVideos.setLayoutManager(new GridLayoutManager(this, 2));
            }

            if (adapter == null) {
                adapter = new VideoAdapter(this, true);
                rvVaultVideos.setAdapter(adapter);

                adapter.setOnVideoClickListener(new VideoAdapter.OnVideoClickListener() {
                    @Override
                    public void onVideoClick(VideoItem video, int position) {
                        try {
                            Intent intent = new Intent(VaultActivity.this, PlayerActivity.class);
                            intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPlaybackUri());
                            intent.putExtra("video_title", video.getTitle());
                            intent.putExtra(AppConstants.EXTRA_FROM_VAULT, true);
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onVideoLongClick(VideoItem video, int position) {
                        showVaultVideoOptions(video);
                    }
                });
            }

            // Observe vault DB
            db.vaultDao().getAll().observe(this, vaultVideos -> {
                try {
                    List<VideoItem> items = new ArrayList<>();
                    if (vaultVideos != null) {
                        for (VaultVideoEntity v : vaultVideos) {
                            try {
                                VideoItem item = new VideoItem();
                                item.setPath(v.vaultPath);
                                item.setContentUri(v.vaultPath);
                                item.setTitle(v.videoTitle);
                                item.setDuration(v.duration);
                                item.setSize(v.fileSize);
                                item.setInVault(true);
                                items.add(item);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
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
        }
    }

    /**
     * Shows Play / Restore to Gallery / Delete options for a vault video.
     */
    private void showVaultVideoOptions(VideoItem video) {
        try {
            String[] options = {
                getString(R.string.play),
                getString(R.string.restore_from_vault),
                getString(R.string.delete_from_vault)
            };

            new MaterialAlertDialogBuilder(this)
                .setTitle(video.getTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Play
                            try {
                                Intent intent = new Intent(this, PlayerActivity.class);
                                intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPlaybackUri());
                                intent.putExtra("video_title", video.getTitle());
                                intent.putExtra(AppConstants.EXTRA_FROM_VAULT, true);
                                startActivity(intent);
                            } catch (Exception e) { e.printStackTrace(); }
                            break;
                        case 1: // Restore
                            restoreFromVault(video);
                            break;
                        case 2: // Delete permanently
                            confirmDeleteFromVault(video);
                            break;
                    }
                })
                .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restoreFromVault(VideoItem video) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String vaultPath = video.getPath();
                VaultVideoEntity entity = db.vaultDao().getByVaultPath(vaultPath);

                if (entity == null) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_SHORT).show());
                    return;
                }

                // Determine restore destination
                String destPath = entity.originalPath;
                if (destPath == null || destPath.isEmpty()) {
                    // Fallback: restore to Downloads
                    destPath = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                        + "/" + new File(vaultPath).getName();
                }

                boolean ok = VaultManager.getInstance(this).restoreFromVault(vaultPath, destPath);
                if (ok) {
                    // Remove from vault DB
                    db.vaultDao().deleteByVaultPath(vaultPath);

                    // Notify MediaStore so the video appears in gallery
                    final String finalDestPath = destPath;
                    MediaScannerConnection.scanFile(this,
                        new String[]{finalDestPath}, new String[]{"video/*"}, null);

                    runOnUiThread(() -> Toast.makeText(this, R.string.restored_from_vault, Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void confirmDeleteFromVault(VideoItem video) {
        try {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_from_vault)
                .setMessage(R.string.delete_video_confirm)
                .setPositiveButton(R.string.delete, (d, w) -> permanentlyDeleteFromVault(video))
                .setNegativeButton(R.string.cancel, null)
                .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void permanentlyDeleteFromVault(VideoItem video) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String vaultPath = video.getPath();
                boolean deleted = VaultManager.getInstance(this).deleteFromVault(vaultPath);
                db.vaultDao().deleteByVaultPath(vaultPath);
                runOnUiThread(() -> Toast.makeText(this,
                    deleted ? R.string.deleted_from_vault : R.string.delete_failed,
                    Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void safeSetVisibility(View view, int visibility) {
        if (view != null) view.setVisibility(visibility);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Auto-lock: reset unlock state so re-entry requires PIN again
        isUnlocked = false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
