package com.videviewer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * VaultManager - Handles secure vault operations
 * Videos are moved to a hidden .vault folder in app's private storage
 */
public class VaultManager {

    private static final String TAG = "VaultManager";
    private static VaultManager instance;

    private final Context context;
    private final SharedPreferences prefs;
    private final File vaultDir;

    private VaultManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
        // Store vault in private app storage (inaccessible to other apps)
        this.vaultDir = new File(context.getFilesDir(), AppConstants.VAULT_FOLDER);
        if (!vaultDir.exists()) vaultDir.mkdirs();
    }

    public static VaultManager getInstance(Context context) {
        if (instance == null) instance = new VaultManager(context);
        return instance;
    }

    // ── Lock Types ───────────────────────────────────────────────────────────
    public String getLockType() {
        return prefs.getString(AppConstants.PREF_VAULT_LOCK_TYPE, AppConstants.LOCK_NONE);
    }

    public boolean isLockSet() {
        return !getLockType().equals(AppConstants.LOCK_NONE);
    }

    public void setPin(String pin) {
        prefs.edit()
            .putString(AppConstants.PREF_VAULT_LOCK_TYPE, AppConstants.LOCK_PIN)
            .putString(AppConstants.PREF_VAULT_PIN, HashUtils.sha256(pin))
            .apply();
    }

    public void setPassword(String password) {
        prefs.edit()
            .putString(AppConstants.PREF_VAULT_LOCK_TYPE, AppConstants.LOCK_PASSWORD)
            .putString(AppConstants.PREF_VAULT_PASSWORD, HashUtils.sha256(password))
            .apply();
    }

    public void setPattern(String patternKey) {
        prefs.edit()
            .putString(AppConstants.PREF_VAULT_LOCK_TYPE, AppConstants.LOCK_PATTERN)
            .putString(AppConstants.PREF_VAULT_PATTERN, HashUtils.sha256(patternKey))
            .apply();
    }

    public boolean verifyPin(String pin) {
        String stored = prefs.getString(AppConstants.PREF_VAULT_PIN, "");
        return stored.equals(HashUtils.sha256(pin));
    }

    public boolean verifyPassword(String password) {
        String stored = prefs.getString(AppConstants.PREF_VAULT_PASSWORD, "");
        return stored.equals(HashUtils.sha256(password));
    }

    public boolean verifyPattern(String patternKey) {
        String stored = prefs.getString(AppConstants.PREF_VAULT_PATTERN, "");
        return stored.equals(HashUtils.sha256(patternKey));
    }

    public void removeLock() {
        prefs.edit()
            .putString(AppConstants.PREF_VAULT_LOCK_TYPE, AppConstants.LOCK_NONE)
            .remove(AppConstants.PREF_VAULT_PIN)
            .remove(AppConstants.PREF_VAULT_PASSWORD)
            .remove(AppConstants.PREF_VAULT_PATTERN)
            .apply();
    }

    // ── Vault File Operations ────────────────────────────────────────────────
    public File getVaultDir() { return vaultDir; }

    /**
     * Move a video file INTO the vault (hides it from MediaStore)
     * Returns the new vault path, or null on failure
     */
    public String moveToVault(String originalPath) {
        try {
            File source = new File(originalPath);
            if (!source.exists()) return null;

            // Create unique vault filename to avoid collisions
            String vaultFileName = System.currentTimeMillis() + "_" + source.getName();
            File dest = new File(vaultDir, vaultFileName);

            if (copyFile(source, dest)) {
                source.delete(); // Remove from original location
                // Create .nomedia to hide from gallery scanners
                createNoMedia();
                return dest.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error moving to vault", e);
        }
        return null;
    }

    /**
     * Restore a video FROM the vault back to its original location
     */
    public boolean restoreFromVault(String vaultPath, String originalPath) {
        try {
            File source = new File(vaultPath);
            if (!source.exists()) return false;

            File dest = new File(originalPath);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }

            if (copyFile(source, dest)) {
                source.delete();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring from vault", e);
        }
        return false;
    }

    /**
     * Permanently delete a file from vault
     */
    public boolean deleteFromVault(String vaultPath) {
        File file = new File(vaultPath);
        return file.exists() && file.delete();
    }

    // ── Utilities ────────────────────────────────────────────────────────────
    private boolean copyFile(File source, File dest) {
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "File copy failed", e);
            return false;
        }
    }

    private void createNoMedia() {
        File noMedia = new File(vaultDir, ".nomedia");
        if (!noMedia.exists()) {
            try { noMedia.createNewFile(); } catch (Exception ignored) {}
        }
    }

    public long getVaultSize() {
        long size = 0;
        File[] files = vaultDir.listFiles();
        if (files != null) {
            for (File f : files) size += f.length();
        }
        return size;
    }
}
