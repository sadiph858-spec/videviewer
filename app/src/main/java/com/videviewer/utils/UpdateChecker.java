package com.videviewer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * UpdateChecker - Checks Play Store for a newer version
 * Uses a simple server-side version endpoint (configurable)
 * For production: replace with your actual version API
 */
public class UpdateChecker {

    private static final long CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L; // 24 hours

    /**
     * Check for updates if enough time has passed since last check.
     * In production, fetch latest version from your API endpoint.
     */
    public static void checkForUpdate(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(
            AppConstants.PREFS_NAME, Context.MODE_PRIVATE);

        long lastCheck = prefs.getLong(AppConstants.PREF_LAST_UPDATE_CHECK, 0);
        if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) return;

        prefs.edit().putLong(AppConstants.PREF_LAST_UPDATE_CHECK,
            System.currentTimeMillis()).apply();

        // In production: perform an HTTP GET to your version API.
        // For now, this is a stub that always assumes up-to-date.
        // Example API response: {"latestVersion": "1.1.0", "forceUpdate": false}
    }

    /**
     * Show update available dialog
     */
    public static void showUpdateDialog(Activity activity, String latestVersion,
                                        boolean forceUpdate) {
        if (activity.isFinishing()) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
            .setTitle("Update Available")
            .setMessage("Version " + latestVersion + " is available. Update now for the best experience.")
            .setPositiveButton("Update", (d, w) -> {
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + activity.getPackageName())));
                } catch (Exception e) {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(AppConstants.PLAY_STORE_URL)));
                }
            });

        if (!forceUpdate) {
            builder.setNegativeButton("Later", null);
        }

        builder.show();
    }

    private static String getCurrentVersion(Activity activity) {
        try {
            PackageInfo pInfo = activity.getPackageManager()
                .getPackageInfo(activity.getPackageName(), 0);
            return pInfo.versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }
}
