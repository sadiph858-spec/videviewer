package com.videviewer.activities;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.videviewer.R;

/**
 * PrivacyPolicyActivity - GDPR-friendly Privacy Policy
 */
public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.privacy_policy);
        }

        WebView webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(false); // No JS needed for static HTML
        webView.loadData(getPrivacyPolicyHtml(), "text/html", "UTF-8");
    }

    private String getPrivacyPolicyHtml() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<style>"
            + "body { font-family: sans-serif; padding: 16px; line-height: 1.7; color: #212121; }"
            + "h1 { font-size: 22px; color: #1565C0; } h2 { font-size: 18px; color: #1976D2; }"
            + "p { font-size: 14px; } ul { padding-left: 20px; }"
            + "</style></head><body>"
            + "<h1>Privacy Policy</h1>"
            + "<p><strong>Effective Date:</strong> January 1, 2025</p>"
            + "<p>VidéViewer (\"we\", \"our\", \"the app\") is committed to protecting your privacy. "
            + "This policy explains how we handle your data.</p>"

            + "<h2>1. Information We Access</h2>"
            + "<p>VidéViewer requests access to your device's video files solely for the purpose of "
            + "displaying and playing your personal video library. We do <strong>not</strong> upload, "
            + "transmit, or share any of your video files with external servers.</p>"

            + "<h2>2. Storage Access</h2>"
            + "<p>We use Android's MediaStore API to scan your device storage for video files. "
            + "This access is read-only for browsing and playing. Write access is only used for "
            + "rename and delete operations performed explicitly by you.</p>"

            + "<h2>3. Locally Stored Data</h2>"
            + "<p>The following data is stored locally on your device only:</p>"
            + "<ul>"
            + "<li><strong>Favorites:</strong> Video paths you mark as favorite.</li>"
            + "<li><strong>Watch History:</strong> Recently watched videos and resume positions.</li>"
            + "<li><strong>Playlists:</strong> Custom playlists you create.</li>"
            + "<li><strong>Private Vault:</strong> Videos you choose to hide, stored in encrypted app-private storage.</li>"
            + "<li><strong>Settings:</strong> Your preferences such as theme, language, and playback settings.</li>"
            + "</ul>"
            + "<p>None of this data is transmitted to any server or third party.</p>"

            + "<h2>4. Advertising</h2>"
            + "<p>VidéViewer uses Google AdMob to display advertisements. AdMob may collect certain "
            + "device information such as advertising ID, IP address, and usage data to serve "
            + "personalized ads. This is governed by "
            + "<a href='https://policies.google.com/privacy'>Google's Privacy Policy</a>.</p>"
            + "<p>You can opt out of personalized ads via your device's Google Settings.</p>"

            + "<h2>5. Private Vault</h2>"
            + "<p>Videos moved to the Private Vault are stored in your device's private app storage, "
            + "which is inaccessible to other apps. The vault is protected by your chosen lock "
            + "(PIN, password, or pattern). We do not have access to your lock credentials.</p>"

            + "<h2>6. Children's Privacy</h2>"
            + "<p>This app is not directed at children under 13. We do not knowingly collect personal "
            + "information from children.</p>"

            + "<h2>7. Your Rights (GDPR)</h2>"
            + "<p>As all data is stored locally on your device, you have full control. You can:</p>"
            + "<ul>"
            + "<li>Clear watch history from Settings → Storage → Clear History</li>"
            + "<li>Remove favorites at any time</li>"
            + "<li>Restore vault videos to delete the vault data</li>"
            + "<li>Uninstall the app to remove all associated data</li>"
            + "</ul>"

            + "<h2>8. Changes to This Policy</h2>"
            + "<p>We may update this Privacy Policy from time to time. We will notify you of "
            + "significant changes via an in-app notification or updated app version.</p>"

            + "<h2>9. Contact Us</h2>"
            + "<p>If you have questions about this Privacy Policy, contact us at:<br>"
            + "<a href='mailto:support@videviewer.com'>support@videviewer.com</a></p>"
            + "</body></html>";
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
