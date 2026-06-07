package com.videviewer.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * PermissionHelper - Handles runtime permissions for Android 6+
 * Scoped Storage aware for Android 10+
 */
public class PermissionHelper {

    /**
     * Returns the required storage permission(s) based on Android version
     */
    public static String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: granular media permissions
            return new String[]{Manifest.permission.READ_MEDIA_VIDEO};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12: scoped storage, READ_EXTERNAL_STORAGE is enough
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        } else {
            // Android 8-9: need both
            return new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    /**
     * Check if all storage permissions are granted
     */
    public static boolean hasStoragePermission(Context context) {
        for (String permission : getStoragePermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Request storage permissions
     */
    public static void requestStoragePermissions(Activity activity, int requestCode) {
        String[] permissions = getStoragePermissions();
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    /**
     * Check if user permanently denied permission (should show settings dialog)
     */
    public static boolean isPermanentlyDenied(Activity activity) {
        for (String permission : getStoragePermissions()) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                    && ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Open App Settings so user can manually grant permissions
     */
    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Check notification permission (Android 13+)
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not required below Android 13
    }

    /**
     * Request notification permission (Android 13+)
     */
    public static void requestNotificationPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, requestCode);
        }
    }
}
