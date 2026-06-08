package com.videviewer;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.gms.ads.MobileAds;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.LocaleHelper;
import com.videviewer.utils.ThemeHelper;

/**
 * VidViewerApp - Application class
 * Initializes global components: AdMob, theme, locale
 */
public class VidViewerApp extends Application {

    private static final String TAG = "VidViewerApp";
    private static VidViewerApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        try {
            ThemeHelper.applyTheme(this);
        } catch (Exception e) {
            Log.e(TAG, "Theme init failed", e);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        try {
            LocaleHelper.applyLocale(this);
        } catch (Exception e) {
            Log.e(TAG, "Locale init failed", e);
        }

        try {
            SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
            boolean adsEnabled = prefs.getBoolean(AppConstants.PREF_ADS_ENABLED, true);
            if (adsEnabled) {
                MobileAds.initialize(this, initializationStatus -> {
                    Log.d(TAG, "AdMob initialized: " + initializationStatus.getAdapterStatusMap());
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "AdMob init failed — ads disabled", e);
        }
    }

    public static VidViewerApp getInstance() {
        return instance;
    }
}
