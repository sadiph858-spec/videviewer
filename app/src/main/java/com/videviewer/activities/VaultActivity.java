package com.videviewer.activities;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
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
import com.videviewer.utils.VaultManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class VaultActivity extends AppCompatActivity {

    private static final String TAG = "VaultActivity";

    private VaultManager vaultManager;
    private AppDatabase db;
    private View layoutLock, layoutVault, layoutNoLock;
    private TextInputEditText etPinPassword;
    private RecyclerView rvVaultVideos;
    private View tvEmpty;
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

            layoutLock    = findViewById(R.id.layout_lock);
            layoutVault   = findViewById(R.id.layout_vault);
            layoutNoLock  = findViewById(R.id.layout_no_lock);
            etPinPassword = findViewById(R.id.et_pin_password);
            rvVaultVideos = findViewById(R.id.rv_vault_videos);
            tvEmpty       = findViewById(R.id.tv_empty);

            if (layoutLock == null || layoutVault == null || layoutNoLock == null) {
                Log.e(TAG, "Critical views null – layout mismatch");
                Toast.makeText(this, "Layout error, please reinstall", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            MaterialButton btnSetupLock = findViewById(R.id.btn_setup_lock);
            if (btnSetupLock != null) {
                btnSetupLock.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(this, LockSetupActivity.class));
                    } catch (Exception e) {
                        Log.e(TAG, "LockSetupActivity error", e);
                        Toast.makeText(this, "Cannot open lock setup", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            MaterialButton btnUnlock = findViewById(R.id.btn_unlock);
            if (btnUnlock != null) btnUnlock.setOnClickListener(v -> attemptUnlock());

            File vaultDir = new File(getFilesDir(), ".vault");
            if (!vaultDir.exists()) vaultDir.mkdirs();
            try {
                File noMedia = new File(vaultDir, ".nomedia");
                if (!noMedia.exists()) noMedia.createNewFile();
            } catch (Exception ignored) {}

            determineState();
        } catch (Exception e) {
            Log.e(TAG, "onCreate crashed", e);
            Toast.makeText(this, "Error opening vault", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try { determineState(); } catch (Exception e) { Log.e(TAG, "onResume", e); }
    }

    private void determineState() {
        try {
            if (vaultManager == null) return;
            if (!vaultManager.isLockSet()) {
                setVisibility(layoutLock, View.GONE);
                setVisibility(layoutVault, View.GONE);
                setVisibility(layoutNoLock, View.VISIBLE);
            } else if (isUnlocked) {
                showVaultContents();
            } else {
                setVisibility(layoutLock, View.VISIBLE);
                setVisibility(layoutVault, View.GONE);
                setVisibility(layoutNoLock, View.GONE);
            }
        } catch (Exception e) { Log.e(TAG, "determineState", e); }
    }

    private void setVisibility(View v, int visibility) {
        if (v != null) v.setVisibility(visibility);
    }

    private void attemptUnlock() {
        try {
            if (etPinPassword == null) return;
            String input = etPinPassword.getText() != null
                ? etPinPassword.getText().toString().trim() : "";
            if (input.isEmpty()) {
                etPinPassword.setError("Enter PIN or Password");
                return;
            }
            String lockType = vaultManager.getLockType();
            boolean ok = false;
            if (AppConstants.LOCK_PIN.equals(lockType))      ok = vaultManager.verifyPin(input);
            else if (AppConstants.LOCK_PASSWORD.equals(lockType)) ok = vaultManager.verifyPassword(input);

            if (ok) {
                isUnlocked = true;
                showVaultContents();
            } else {
                etPinPassword.setError("Incorrect credentials");
                etPinPassword.setText("");
            }
        } catch (Exception e) { Log.e(TAG, "attemptUnlock", e); }
    }

    private void showVaultContents() {
        try {
            setVisibility(layoutLock, View.GONE);
            setVisibility(layoutNoLock, View.GONE);
            setVisibility(layoutVault, View.VISIBLE);

            VideoAdapter adapter = new VideoAdapter(this, true);
            if (rvVaultVideos != null) {
                rvVaultVideos.setLayoutManager(new GridLayoutManager(this, 2));
                rvVaultVideos.setAdapter(adapter);
            }

            db.vaultDao().getAll().observe(this, vaultVideos -> {
                try {
                    List<VideoItem> items = new ArrayList<>();
                    if (vaultVideos != null) {
                        for (VaultVideoEntity v : vaultVideos) {
                            VideoItem item = new VideoItem();
                            item.setPath(v.vaultPath);
                            item.setTitle(v.videoTitle != null ? v.videoTitle : "Unknown");
                            item.setDuration(v.duration);
                            item.setSize(v.fileSize);
                            item.setInVault(true);
                            items.add(item);
                        }
                    }
                    adapter.submitList(items);
                    if (tvEmpty != null)
                        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                } catch (Exception e) { Log.e(TAG, "vault observer", e); }
            });

            adapter.setOnVideoClickListener((video, pos) -> {
                try {
                    Intent intent = new Intent(this, PlayerActivity.class);
                    intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
                    intent.putExtra(AppConstants.EXTRA_VIDEO_TITLE, video.getTitle());
                    startActivity(intent);
                } catch (Exception e) { Log.e(TAG, "vault play", e); }
            });
        } catch (Exception e) { Log.e(TAG, "showVaultContents", e); }
    }

    public static void moveToVault(android.content.Context context, VideoItem video) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File vaultDir = new File(context.getFilesDir(), ".vault");
                if (!vaultDir.exists()) vaultDir.mkdirs();
                File src = new File(video.getPath());
                String vaultName = System.currentTimeMillis() + "_" + src.getName();
                File dest = new File(vaultDir, vaultName);
                try (FileInputStream in = new FileInputStream(src);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192]; int read;
                    while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                }
                android.content.ContentResolver resolver = context.getContentResolver();
                resolver.delete(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    android.provider.MediaStore.Video.Media.DATA + "=?",
                    new String[]{video.getPath()});
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
            } catch (Exception e) { Log.e(TAG, "moveToVault", e); }
        });
    }

    public static void restoreFromVault(android.content.Context context, VaultVideoEntity entity) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File src = new File(entity.vaultPath);
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File dest = new File(downloads, src.getName());
                try (FileInputStream in = new FileInputStream(src);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192]; int read;
                    while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                }
                src.delete();
                MediaScannerConnection.scanFile(context, new String[]{dest.getAbsolutePath()}, null, null);
                AppDatabase.getInstance(context).vaultDao().deleteByOriginalPath(entity.originalPath);
            } catch (Exception e) { Log.e(TAG, "restoreFromVault", e); }
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
