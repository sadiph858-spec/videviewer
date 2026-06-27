package com.videviewer.utils;

  import android.content.Context;
  import android.content.SharedPreferences;
  import android.content.res.Configuration;
  import java.util.Locale;

  public class LocaleHelper {
      public static Context setLocale(Context context) {
          SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
          String lang = prefs.getString(AppConstants.PREF_LANGUAGE, "en");
          Locale locale = new Locale(lang);
          Locale.setDefault(locale);
          Configuration config = new Configuration(context.getResources().getConfiguration());
          config.setLocale(locale);
          return context.createConfigurationContext(config);
      }
  }