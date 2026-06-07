package com.videviewer.activities;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.videviewer.R;

/**
 * DisclaimerActivity - App usage disclaimer
 */
public class DisclaimerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.disclaimer);
        }

        WebView webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        webView.loadData(getDisclaimerHtml(), "text/html", "UTF-8");
    }

    private String getDisclaimerHtml() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<style>body{font-family:sans-serif;padding:16px;line-height:1.7;color:#212121}"
            + "h1{font-size:22px;color:#B71C1C}h2{font-size:17px;color:#C62828}</style></head><body>"
            + "<h1>Disclaimer</h1>"
            + "<p><strong>Last updated:</strong> January 1, 2025</p>"
            + "<h2>General</h2>"
            + "<p>VidéViewer is a local video player application. It does not host, distribute, "
            + "or facilitate access to any copyrighted content. All videos played through the app "
            + "are stored locally on the user's device.</p>"
            + "<h2>No Liability for Content</h2>"
            + "<p>We are not responsible for any video content accessed through this app. Users are "
            + "solely responsible for ensuring they have the right to play or manage any video files "
            + "on their devices.</p>"
            + "<h2>Private Vault</h2>"
            + "<p>The Private Vault feature is intended for protecting personal, lawfully owned content. "
            + "Misuse of the vault feature to hide illegal content is strictly prohibited.</p>"
            + "<h2>Data Loss</h2>"
            + "<p>While we take care to protect your data, we are not liable for any data loss, "
            + "file corruption, or device issues arising from using this app. Always maintain "
            + "backups of important files.</p>"
            + "<h2>Vault Credentials</h2>"
            + "<p>We cannot recover your vault PIN, password, or pattern if forgotten. "
            + "Please keep your credentials safe.</p>"
            + "<h2>Third-Party Ads</h2>"
            + "<p>This app uses Google AdMob for advertising. We are not responsible for the content "
            + "of third-party advertisements displayed within the app.</p>"
            + "<h2>Accuracy</h2>"
            + "<p>Video metadata (duration, resolution, size) is sourced from your device's media library. "
            + "We do not guarantee the accuracy of this information.</p>"
            + "<h2>Contact</h2>"
            + "<p>For concerns, contact <a href='mailto:support@videviewer.com'>support@videviewer.com</a></p>"
            + "</body></html>";
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
