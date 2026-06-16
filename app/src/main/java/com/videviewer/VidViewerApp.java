package com.videviewer;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.videviewer.utils.AppConstants;

public class VidViewerApp extends Application {

    private static VidViewerApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        try {
            applyTheme();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyTheme() {
        try {
            SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
            String theme = prefs.getString(AppConstants.PREF_THEME, AppConstants.THEME_SYSTEM);
            switch (theme) {
                case AppConstants.THEME_LIGHT:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case AppConstants.THEME_DARK:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                default:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static VidViewerApp getInstance() {
        return instance;
    }
}