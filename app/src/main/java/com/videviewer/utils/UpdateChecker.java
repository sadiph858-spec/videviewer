package com.videviewer.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.videviewer.BuildConfig;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

/**
 * UpdateChecker — checks GitHub releases for a newer version.
 * Throttled to once per 24 hours.
 */
public class UpdateChecker {

    private static final String TAG = "UpdateChecker";
    private static final long CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L; // 24h
    private static final String RELEASES_API =
        "https://api.github.com/repos/sadiph858-spec/videviewer/releases/latest";
    private static final String PREF_LAST_CHECK = "pref_last_update_check";

    public static void check(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        try {
            SharedPreferences prefs = activity.getSharedPreferences(
                AppConstants.PREFS_NAME, Activity.MODE_PRIVATE);
            long lastCheck = prefs.getLong(PREF_LAST_CHECK, 0);
            if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) return;
            prefs.edit().putLong(PREF_LAST_CHECK, System.currentTimeMillis()).apply();

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    String latestTag = fetchLatestTag();
                    if (latestTag == null) return;

                    String current = BuildConfig.VERSION_NAME;
                    if (isNewer(latestTag, current)) {
                        String finalTag = latestTag;
                        activity.runOnUiThread(() -> showUpdateDialog(activity, finalTag));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Update check failed: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "check() error", e);
        }
    }

    /** Returns the tag_name from the latest GitHub release (e.g. "v3.1.0"), or null. */
    private static String fetchLatestTag() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(RELEASES_API);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("User-Agent", "VidViewer-Android/" + BuildConfig.VERSION_NAME);

            int code = conn.getResponseCode();
            if (code != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            // Parse "tag_name" from JSON without a library
            String json = sb.toString();
            int idx = json.indexOf("\"tag_name\"");
            if (idx < 0) return null;
            int start = json.indexOf('"', idx + 11) + 1;
            int end   = json.indexOf('"', start);
            if (start <= 0 || end <= start) return null;
            return json.substring(start, end); // e.g. "v3.1.0"
        } catch (Exception e) {
            Log.w(TAG, "fetchLatestTag: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Returns true if latestTag represents a version higher than current.
     * Strips leading 'v' and compares numeric components.
     */
    private static boolean isNewer(String latestTag, String current) {
        try {
            int[] latest  = parseVersion(latestTag);
            int[] cur     = parseVersion(current);
            for (int i = 0; i < Math.max(latest.length, cur.length); i++) {
                int l = i < latest.length ? latest[i] : 0;
                int c = i < cur.length   ? cur[i]    : 0;
                if (l > c) return true;
                if (l < c) return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static int[] parseVersion(String v) {
        if (v == null) return new int[]{0};
        v = v.replaceAll("[^0-9.]", "");
        String[] parts = v.split("\\.");
        int[] nums = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { nums[i] = Integer.parseInt(parts[i]); } catch (Exception e) { nums[i] = 0; }
        }
        return nums;
    }

    private static void showUpdateDialog(Activity activity, String latestVersion) {
        if (activity == null || activity.isFinishing()) return;
        try {
            new MaterialAlertDialogBuilder(activity)
                .setTitle("Update Available")
                .setMessage("Version " + latestVersion + " is available!\n\nUpdate for the latest features and bug fixes.")
                .setPositiveButton("Download APK", (d, w) -> {
                    try {
                        String url = "https://github.com/sadiph858-spec/videviewer/releases/latest";
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) {
                        Log.e(TAG, "Open release page failed", e);
                    }
                })
                .setNegativeButton("Later", null)
                .show();
        } catch (Exception e) {
            Log.e(TAG, "showUpdateDialog failed", e);
        }
    }
}
