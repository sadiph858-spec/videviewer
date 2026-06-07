package com.videviewer;

import android.app.Application;
import android.content.SharedPreferences;
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

    private static VidViewerApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Apply saved theme
        ThemeHelper.applyTheme(this);

        // Apply saved language
        LocaleHelper.applyLocale(this);

        // Initialize AdMob only if ads enabled
        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        boolean adsEnabled = prefs.getBoolean(AppConstants.PREF_ADS_ENABLED, true);
        if (adsEnabled) {
            MobileAds.initialize(this, initializationStatus -> {});
        }
    }

    public static VidViewerApp getInstance() {
        return instance;
    }
}
