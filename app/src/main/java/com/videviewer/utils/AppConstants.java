package com.videviewer.utils;

/**
 * AppConstants - Central constants for the entire app
 */
public final class AppConstants {

    private AppConstants() {}

    // Shared Preferences
    public static final String PREFS_NAME = "videviewer_prefs";
    public static final String PREF_THEME = "pref_theme";
    public static final String PREF_LANGUAGE = "pref_language";
    public static final String PREF_ADS_ENABLED = "pref_ads_enabled";
    public static final String PREF_BANNER_AD_ID = "pref_banner_ad_id";
    public static final String PREF_INTERSTITIAL_AD_ID = "pref_interstitial_ad_id";
    public static final String PREF_REWARDED_AD_ID = "pref_rewarded_ad_id";
    public static final String PREF_VIEW_MODE = "pref_view_mode"; // grid or list
    public static final String PREF_SORT_ORDER = "pref_sort_order";
    public static final String PREF_AUTO_PLAY = "pref_auto_play";
    public static final String PREF_RESUME_PLAYBACK = "pref_resume_playback";
    public static final String PREF_PLAYBACK_SPEED = "pref_playback_speed";
    public static final String PREF_REPEAT_MODE = "pref_repeat_mode";
    public static final String PREF_SUBTITLE_ENABLED = "pref_subtitle_enabled";
    public static final String PREF_SLEEP_TIMER = "pref_sleep_timer";
    public static final String PREF_VAULT_LOCK_TYPE = "pref_vault_lock_type";
    public static final String PREF_VAULT_PIN = "pref_vault_pin";
    public static final String PREF_VAULT_PASSWORD = "pref_vault_password";
    public static final String PREF_VAULT_PATTERN = "pref_vault_pattern";
    public static final String PREF_APP_LOCK_ENABLED = "pref_app_lock_enabled";
    public static final String PREF_FIRST_LAUNCH = "pref_first_launch";
    public static final String PREF_LAST_UPDATE_CHECK = "pref_last_update_check";
    public static final String PREF_BATTERY_OPTIMIZATION = "pref_battery_optimization";

    // View Modes
    public static final String VIEW_GRID = "grid";
    public static final String VIEW_LIST = "list";

    // Sort Orders
    public static final String SORT_NAME_ASC = "name_asc";
    public static final String SORT_NAME_DESC = "name_desc";
    public static final String SORT_DATE_NEW = "date_new";
    public static final String SORT_DATE_OLD = "date_old";
    public static final String SORT_SIZE_LARGE = "size_large";
    public static final String SORT_SIZE_SMALL = "size_small";
    public static final String SORT_DURATION_LONG = "duration_long";
    public static final String SORT_DURATION_SHORT = "duration_short";

    // Theme Options
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_DYNAMIC = "dynamic"; // Material You

    // Lock Types
    public static final String LOCK_NONE = "none";
    public static final String LOCK_PIN = "pin";
    public static final String LOCK_PASSWORD = "password";
    public static final String LOCK_PATTERN = "pattern";

    // Languages
    public static final String LANG_ENGLISH = "en";
    public static final String LANG_BANGLA = "bn";

    // Intent Extras
    public static final String EXTRA_VIDEO_PATH = "extra_video_path";
    public static final String EXTRA_VIDEO_ID = "extra_video_id";
    public static final String EXTRA_FOLDER_PATH = "extra_folder_path";
    public static final String EXTRA_PLAYLIST_ID = "extra_playlist_id";
    public static final String EXTRA_VIDEO_LIST = "extra_video_list";
    public static final String EXTRA_POSITION = "extra_position";
    public static final String EXTRA_FROM_VAULT = "extra_from_vault";

    // Database
    public static final String DB_NAME = "videviewer_db";
    public static final int DB_VERSION = 1;

    // AdMob Test IDs (replace with real IDs in production)
    public static final String TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String TEST_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917";

    // Playback
    public static final float[] PLAYBACK_SPEEDS = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
    public static final int MAX_HISTORY_SIZE = 100;
    public static final int MAX_RECENT_SIZE = 20;

    // Vault
    public static final String VAULT_FOLDER = ".vault";
    public static final int PIN_MIN_LENGTH = 4;
    public static final int PIN_MAX_LENGTH = 8;
    public static final int PATTERN_MIN_POINTS = 4;

    // Notifications
    public static final int NOTIFICATION_ID_PLAYBACK = 1001;
    public static final String NOTIFICATION_CHANNEL_PLAYBACK = "playback_channel";

    // Request Codes
    public static final int REQUEST_PERMISSION_STORAGE = 100;
    public static final int REQUEST_PERMISSION_NOTIFICATION = 101;
    public static final int REQUEST_PIP = 102;
    public static final int REQUEST_VAULT_LOCK = 103;

    // Thumbnail Cache
    public static final int THUMBNAIL_CACHE_SIZE_MB = 50;

    // App Info
    public static final String DEV_EMAIL = "support@videviewer.com";
    public static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.videviewer";
    public static final String APP_WEBSITE = "https://videviewer.com";
}
