package com.videviewer;

  import android.app.Application;
  import android.content.Context;
  import android.content.SharedPreferences;
  import com.google.android.gms.ads.MobileAds;
  import com.videviewer.utils.AppConstants;
  import com.videviewer.utils.LocaleHelper;
  import com.videviewer.utils.ThemeHelper;

  public class VidViewerApp extends Application {

      private static VidViewerApp instance;

      @Override
      public void onCreate() {
          super.onCreate();
          instance = this;
          ThemeHelper.applyTheme(this);
          MobileAds.initialize(this, initializationStatus -> {});
      }

      @Override
      protected void attachBaseContext(Context base) {
          super.attachBaseContext(LocaleHelper.setLocale(base));
      }

      public static VidViewerApp getInstance() { return instance; }
  }