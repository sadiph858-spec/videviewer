package com.videviewer;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.videviewer.utils.AppConstants;

public class VidViewerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Force dark theme always for this app's design
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
