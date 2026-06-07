package com.videviewer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.color.DynamicColors;

/**
 * ThemeHelper - Manages app theme (Light/Dark/System/Dynamic)
 */
public class ThemeHelper {

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
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
                // Dynamic Colors (Material You) - Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    DynamicColors.applyToActivitiesIfAvailable((android.app.Application) context.getApplicationContext());
                }
                break;
            default: // system
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public static void saveTheme(Context context, String theme) {
        context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(AppConstants.PREF_THEME, theme).apply();
    }

    public static String getCurrentTheme(Context context) {
        return context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(AppConstants.PREF_THEME, AppConstants.THEME_SYSTEM);
    }
}
