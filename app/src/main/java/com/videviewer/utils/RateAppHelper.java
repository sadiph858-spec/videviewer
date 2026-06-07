package com.videviewer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.videviewer.R;

/**
 * RateAppHelper - Shows a rate app dialog after enough usage
 * Respects "don't ask again" preference
 */
public class RateAppHelper {

    private static final String PREF_LAUNCH_COUNT = "pref_launch_count";
    private static final String PREF_DONT_SHOW_RATE = "pref_dont_show_rate";
    private static final int LAUNCHES_BEFORE_PROMPT = 5;

    /**
     * Call this on every app launch. Shows dialog when threshold is reached.
     */
    public static void onAppLaunch(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(
            AppConstants.PREFS_NAME, Context.MODE_PRIVATE);

        if (prefs.getBoolean(PREF_DONT_SHOW_RATE, false)) return;

        int count = prefs.getInt(PREF_LAUNCH_COUNT, 0) + 1;
        prefs.edit().putInt(PREF_LAUNCH_COUNT, count).apply();

        if (count == LAUNCHES_BEFORE_PROMPT) {
            showRateDialog(activity, prefs);
        }
    }

    private static void showRateDialog(Activity activity, SharedPreferences prefs) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        new MaterialAlertDialogBuilder(activity)
            .setTitle("Enjoying " + activity.getString(R.string.app_name) + "?")
            .setMessage("If you enjoy the app, please take a moment to rate it. It really helps!")
            .setPositiveButton("Rate Now", (dialog, which) -> {
                prefs.edit().putBoolean(PREF_DONT_SHOW_RATE, true).apply();
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + activity.getPackageName())));
                } catch (Exception e) {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(AppConstants.PLAY_STORE_URL)));
                }
            })
            .setNeutralButton("Later", null)
            .setNegativeButton("Never", (dialog, which) ->
                prefs.edit().putBoolean(PREF_DONT_SHOW_RATE, true).apply())
            .show();
    }
}
