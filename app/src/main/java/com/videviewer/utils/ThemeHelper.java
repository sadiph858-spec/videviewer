package com.videviewer.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.color.DynamicColors;

/**
 * ThemeHelper - Manages app theme (Light/Dark/System/Dynamic).
 * All operations are null-safe and exception-guarded.
 */
public class ThemeHelper {

    private static final String TAG = "ThemeHelper";

    public static void applyTheme(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
            String theme = prefs.getString(AppConstants.PREF_THEME, AppConstants.THEME_SYSTEM);

            switch (theme) {
                case AppConstants.THEME_LIGHT:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case AppConstants.THEME_DARK:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                case AppConstants.THEME_DYNAMIC:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Context appCtx = context.getApplicationContext();
                        if (appCtx instanceof Application) {
                            try {
                                DynamicColors.applyToActivitiesIfAvailable((Application) appCtx);
                            } catch (Exception e) {
                                Log.w(TAG, "DynamicColors not available", e);
                            }
                        }
                    }
                    break;
                default:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        } catch (Exception e) {
            Log.e(TAG, "applyTheme failed", e);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public static void saveTheme(Context context, String theme) {
        try {
            context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(AppConstants.PREF_THEME, theme).apply();
        } catch (Exception ignored) {}
    }

    public static String getCurrentTheme(Context context) {
        try {
            return context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(AppConstants.PREF_THEME, AppConstants.THEME_SYSTEM);
        } catch (Exception e) {
            return AppConstants.THEME_SYSTEM;
        }
    }
}
