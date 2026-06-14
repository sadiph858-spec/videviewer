package com.videviewer.activities;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.videviewer.R;
import com.videviewer.adapters.VideoAdapter;
import com.videviewer.database.AppDatabase;
import com.videviewer.database.VaultVideoEntity;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.HashUtils;
import com.videviewer.utils.VaultManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class VaultActivity extends AppCompatActivity {

    private VaultManager vaultManager;
    private AppDatabase db;
    private View layoutLock, layoutVault, layoutNoLock;
    private TextInputEditText etPinPassword;
    private RecyclerView rvVaultVideos;
    private TextView tvEmpty;
    private static boolean isUnlocked = false;

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
        rvVaultVideos = findViewById(R.id.rv_vault_videos);
        tvEmpty = findViewById(R.id.tv_empty);

        MaterialButton btnSetupLock = findViewById(R.id.btn_setup_lock);
        if (btnSetupLock != null) {
            btnSetupLock.setOnClickListener(v ->
                startActivity(new Intent(this, LockSetupActivity.class)));
        }

        MaterialButton btnUnlock = findViewById(R.id.btn_unlock);
        if (btnUnlock != null) btnUnlock.setOnClickListener(v -> attemptUnlock());

        // Create vault dir and .nomedia
        File vaultDir = new File(getFilesDir(), ".vault");
        if (!vaultDir.exists()) vaultDir.mkdirs();
        File noMedia = new File(vaultDir, ".nomedia");
        try { if (!noMedia.exists()) noMedia.createNewFile(); } catch (Exception e) {}

        determineState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        determineState();
    }

    private void determineState() {
        if (!vaultManager.isLockSet()) {
            layoutLock.setVisibility(View.GONE);
            layoutVault.setVisibility(View.GONE);
            layoutNoLock.setVisibility(View.VISIBLE);
        } else if (isUnlocked) {
            showVaultContents();
        } else {
            layoutLock.setVisibility(View.VISIBLE);
            layoutVault.setVisibility(View.GONE);
            layoutNoLock.setVisibility(View.GONE);
        }
    }

    private void attemptUnlock() {
        if (etPinPassword == null) return;
        String input = etPinPassword.getText() != null
            ? etPinPassword.getText().toString().trim() : "";
        if (input.isEmpty()) {
            etPinPassword.setError("Enter PIN or Password");
            return;
        }
        String lockType = vaultManager.getLockType();
        boolean ok = false;
        if (AppConstants.LOCK_PIN.equals(lockType)) ok = vaultManager.verifyPin(input);
        else if (AppConstants.LOCK_PASSWORD.equals(lockType)) ok = vaultManager.verifyPassword(input);

        if (ok) {
            isUnlocked = true;
            showVaultContents();
        } else {
            etPinPassword.setError("Incorrect credentials");
            etPinPassword.setText("");
        }
    }

    private void showVaultContents() {
        layoutLock.setVisibility(View.GONE);
        layoutNoLock.setVisibility(View.GONE);
        layoutVault.setVisibility(View.VISIBLE);

        VideoAdapter adapter = new VideoAdapter(this, true);
        if (rvVaultVideos != null) {
            rvVaultVideos.setLayoutManager(new GridLayoutManager(this, 2));
            rvVaultVideos.setAdapter(adapter);
        }

        db.vaultDao().getAll().observe(this, vaultVideos -> {
            List<VideoItem> items = new ArrayList<>();
            if (vaultVideos != null) {
                for (VaultVideoEntity v : vaultVideos) {
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
            if (tvEmpty != null)
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        adapter.setOnVideoClickListener((video, pos) -> {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("extra_video_path", video.getPath());
            intent.putExtra("video_title", video.getTitle());
            startActivity(intent);
        });
    }

    public static void moveToVault(android.content.Context context, VideoItem video) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File vaultDir = new File(context.getFilesDir(), ".vault");
                if (!vaultDir.exists()) vaultDir.mkdirs();

                File src = new File(video.getPath());
                String vaultName = System.currentTimeMillis() + "_" + src.getName();
                File dest = new File(vaultDir, vaultName);

                // Copy file
                try (FileInputStream in = new FileInputStream(src);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                }

                // Delete original from MediaStore
                android.content.ContentResolver resolver = context.getContentResolver();
                resolver.delete(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    android.provider.MediaStore.Video.Media.DATA + "=?",
                    new String[]{video.getPath()});

                // Save to vault DB
                AppDatabase db = AppDatabase.getInstance(context);
                VaultVideoEntity entity = new VaultVideoEntity();
                entity.originalPath = video.getPath();
                entity.vaultPath = dest.getAbsolutePath();
                entity.videoTitle = video.getTitle();
                entity.fileSize = video.getSize();
                entity.duration = video.getDuration();
                entity.addedToVault = System.currentTimeMillis();
                entity.originalFolder = video.getFolderName();
                db.vaultDao().insert(entity);

            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    public static void restoreFromVault(android.content.Context context, VaultVideoEntity entity) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File src = new File(entity.vaultPath);
                File downloads = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
                File dest = new File(downloads, src.getName());

                try (FileInputStream in = new FileInputStream(src);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                }

                src.delete();

                // Rescan so it appears in gallery
                MediaScannerConnection.scanFile(context,
                    new String[]{dest.getAbsolutePath()}, null, null);

                // Remove from vault DB
                AppDatabase.getInstance(context).vaultDao()
                    .deleteByOriginalPath(entity.originalPath);

            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        isUnlocked = false;
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
