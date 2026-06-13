package com.videviewer.utils;

  import android.content.Context;
  import android.content.SharedPreferences;
  import androidx.appcompat.app.AppCompatDelegate;

  public class ThemeHelper {
      public static void applyTheme(Context context) {
          SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
          int theme = prefs.getInt(AppConstants.PREF_THEME, 0);
          switch (theme) {
              case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
              case 2: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
              default: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
          }
      }
  }