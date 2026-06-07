package com.videviewer.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.videviewer.BuildConfig;
import com.videviewer.R;
import com.videviewer.utils.AppConstants;

/**
 * TermsActivity - Terms and Conditions
 */
public class TermsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.terms_conditions);
        }
        WebView webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        webView.loadData(getTermsHtml(), "text/html", "UTF-8");
    }

    private String getTermsHtml() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<style>body{font-family:sans-serif;padding:16px;line-height:1.7;color:#212121}"
            + "h1{font-size:22px;color:#1565C0}h2{font-size:18px;color:#1976D2}</style></head><body>"
            + "<h1>Terms &amp; Conditions</h1>"
            + "<p><strong>Last updated:</strong> January 1, 2025</p>"
            + "<p>By using VidéViewer, you agree to these Terms and Conditions. Please read them carefully.</p>"
            + "<h2>1. Use of the App</h2>"
            + "<p>VidéViewer is a personal video player app. You may use it to play, organize, and manage "
            + "video files stored on your device. You must not use the app for any unlawful purposes.</p>"
            + "<h2>2. Intellectual Property</h2>"
            + "<p>All app content, design, and code are owned by VidéViewer. You may not copy, reverse-engineer, "
            + "or redistribute any part of the app without written permission.</p>"
            + "<h2>3. User Content</h2>"
            + "<p>You are solely responsible for the video content you play or store using this app. "
            + "We do not access, monitor, or control the content of your videos.</p>"
            + "<h2>4. Private Vault</h2>"
            + "<p>The Private Vault is provided for personal privacy. You are responsible for remembering "
            + "your lock credentials. We cannot recover lost PINs or passwords.</p>"
            + "<h2>5. Advertisements</h2>"
            + "<p>The free version of VidéViewer displays advertisements. By using the app, you consent "
            + "to ad display in accordance with Google AdMob's policies.</p>"
            + "<h2>6. Disclaimer</h2>"
            + "<p>The app is provided \"as is\" without warranties of any kind. We are not liable for "
            + "any data loss, device damage, or other issues arising from use of the app.</p>"
            + "<h2>7. Changes</h2>"
            + "<p>We may modify these terms at any time. Continued use of the app constitutes acceptance.</p>"
            + "<h2>8. Contact</h2>"
            + "<p>Questions? Email us at <a href='mailto:support@videviewer.com'>support@videviewer.com</a></p>"
            + "</body></html>";
    }
    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
