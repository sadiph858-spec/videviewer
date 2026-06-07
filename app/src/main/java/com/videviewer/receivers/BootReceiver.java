package com.videviewer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BootReceiver - Handles device boot to reinitialize app state if needed
 * (e.g., re-registering any alarms or clearing session state)
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed received");
            // Vault auto-locks on device restart by design (session state clears)
            // No additional action required currently
        }
    }
}
