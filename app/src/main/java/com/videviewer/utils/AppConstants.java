package com.videviewer.utils;

public class AppConstants {
    // Intent extras
    public static final String EXTRA_VIDEO_PATH = "extra_video_path";
    public static final String EXTRA_VIDEO_TITLE = "extra_video_title";
    public static final String EXTRA_VIDEO_ID = "extra_video_id";
    public static final String EXTRA_FOLDER_PATH = "extra_folder_path";
    public static final String EXTRA_RESUME_POSITION = "extra_resume_position";

    // Shared prefs
    public static final String PREF_NAME = "vidviewer_prefs";
    public static final String PREFS_NAME = "vidviewer_prefs";
    public static final String PREF_THEME = "pref_theme";
    public static final String PREF_LANGUAGE = "pref_language";
    public static final String PREF_LAST_UPDATE_CHECK = "pref_last_update_check";

    // Theme values
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT  = "light";
    public static final String THEME_DARK   = "dark";

    // Vault prefs
    public static final String PREF_VAULT_PIN = "pref_vault_pin";
    public static final String PREF_VAULT_TYPE = "pref_vault_type";
    public static final String PREF_VAULT_LOCK_TYPE = "pref_vault_lock_type";
    public static final String PREF_VAULT_PASSWORD = "pref_vault_password";
    public static final String PREF_VAULT_PATTERN = "pref_vault_pattern";

    // Lock types
    public static final String LOCK_NONE = "none";
    public static final String LOCK_PIN = "pin";
    public static final String LOCK_PASSWORD = "password";
    public static final String LOCK_PATTERN = "pattern";
    public static final int PIN_MIN_LENGTH = 4;

    // Sort orders
    public static final String SORT_DATE_NEW = "date_new";
    public static final String SORT_DATE_OLD = "date_old";
    public static final String SORT_NAME_ASC = "name_asc";
    public static final String SORT_NAME_DESC = "name_desc";
    public static final String SORT_SIZE_LARGE = "size_large";
    public static final String SORT_SIZE_SMALL = "size_small";
    public static final String SORT_DURATION_LONG = "duration_long";
    public static final String SORT_DURATION_SHORT = "duration_short";

    // Database
    public static final String DB_NAME = "vidviewer_db";
    public static final int DB_VERSION = 4;

    // Paths
    public static final String DOWNLOAD_DIR = "VidViewer";
    public static final String VAULT_DIR = ".vault";
    public static final String VAULT_FOLDER = ".vault";

    // Notifications
    public static final int NOTIFICATION_DOWNLOAD_ID = 1001;
    public static final String CHANNEL_DOWNLOAD = "channel_download";

    // URLs / contact
    public static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.videviewer";
    public static final String APP_WEBSITE = "https://vidviewer.app";
    public static final String DEV_EMAIL = "support@vidviewer.app";

    // Misc
    public static final int THUMBNAIL_CACHE_SIZE_MB = 50;
    public static final int MAX_RECENT_SIZE = 50;
    public static final int NOTIFICATION_ID_PLAYBACK = 2001;
    public static final String NOTIFICATION_CHANNEL_PLAYBACK = "channel_playback";

    // AdMob test IDs
    public static final String ADMOB_BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String ADMOB_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String ADMOB_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917";
}
