package com.videviewer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;
import java.util.Locale;

/**
 * LocaleHelper - Handles multi-language support (English & Bangla)
 */
public class LocaleHelper {

    public static void applyLocale(Context context) {
        String lang = getSavedLanguage(context);
        setLocale(context, lang);
    }

    public static Context setLocale(Context context, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLocales(new LocaleList(locale));

        return context.createConfigurationContext(config);
    }

    public static void saveLanguage(Context context, String lang) {
        context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(AppConstants.PREF_LANGUAGE, lang).apply();
    }

    public static String getSavedLanguage(Context context) {
        return context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(AppConstants.PREF_LANGUAGE, AppConstants.LANG_ENGLISH);
    }
}
