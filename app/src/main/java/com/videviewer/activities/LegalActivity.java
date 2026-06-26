package com.videviewer.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.videviewer.R;

public class LegalActivity extends AppCompatActivity {

    public static final String EXTRA_PAGE_TYPE = "page_type";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String pageType = getIntent().getStringExtra(EXTRA_PAGE_TYPE);
        if (pageType == null) pageType = "about";

        String title;
        String html;
        switch (pageType) {
            case "privacy":   title = "Privacy Policy";        html = buildPrivacy();    break;
            case "terms":     title = "Terms & Conditions";    html = buildTerms();      break;
            case "contact":   title = "Contact Us";            html = buildContact();    break;
            case "disclaimer":title = "Disclaimer";            html = buildDisclaimer(); break;
            default:          title = "About VidéViewer";      html = buildAbout();      break;
        }

        setTitle(title);
        toolbar.setTitle(title);

        WebView wv = findViewById(R.id.webview_legal);
        wv.getSettings().setJavaScriptEnabled(false);
        wv.getSettings().setBuiltInZoomControls(false);
        wv.setWebViewClient(new WebViewClient());
        wv.loadDataWithBaseURL(null, wrap(html), "text/html", "utf-8", null);
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    // ── HTML wrapper ────────────────────────────────────────────
    private String wrap(String body) {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>"
            + "body{background:#0e0e1a;color:#e0e0e0;font-family:sans-serif;font-size:15px;line-height:1.7;padding:20px;margin:0}"
            + "h1{color:#c084fc;font-size:22px;margin-top:0}"
            + "h2{color:#a78bfa;font-size:17px;margin-top:24px;border-bottom:1px solid #2a2a3a;padding-bottom:6px}"
            + "h3{color:#7c3aed;font-size:15px;margin-top:18px}"
            + "p,li{color:#cccccc;margin:8px 0}"
            + "ul{padding-left:20px}"
            + "a{color:#818cf8}"
            + ".badge{display:inline-block;background:#1e1e30;border:1px solid #3a3a5a;border-radius:8px;padding:10px 16px;margin:8px 4px;font-size:13px}"
            + ".info-box{background:#13131f;border-left:4px solid #7c3aed;padding:14px;border-radius:0 8px 8px 0;margin:16px 0}"
            + ".contact-card{background:#13131f;border:1px solid #2a2a3a;border-radius:12px;padding:16px;margin:12px 0}"
            + ".version{color:#666;font-size:12px;text-align:right;margin-top:32px}"
            + "</style></head><body>" + body + "</body></html>";
    }

    // ── Privacy Policy ─────────────────────────────────────────
    private String buildPrivacy() {
        return "<h1>Privacy Policy</h1>"
            + "<p><em>Last updated: June 26, 2025 &nbsp;|&nbsp; Effective: July 1, 2025</em></p>"
            + "<div class='info-box'>VidéViewer is built with privacy as a core principle. We collect the minimum data necessary to provide our service.</div>"

            + "<h2>1. Information We Collect</h2>"
            + "<h3>1.1 Information You Provide</h3>"
            + "<ul>"
            + "<li><strong>URLs you enter</strong> — video links you paste or share to the app for downloading or playing</li>"
            + "<li><strong>App preferences</strong> — settings you configure within the app (stored locally on your device only)</li>"
            + "</ul>"
            + "<h3>1.2 Automatically Collected Information</h3>"
            + "<ul>"
            + "<li><strong>Device information</strong> — Android version, device model (for crash reporting only)</li>"
            + "<li><strong>App usage data</strong> — anonymous feature usage statistics (if you opted in)</li>"
            + "<li><strong>Error logs</strong> — crash reports to help us improve stability</li>"
            + "</ul>"
            + "<h3>1.3 Information We Do NOT Collect</h3>"
            + "<ul>"
            + "<li>Your name, email address, or personal identification</li>"
            + "<li>Your location data</li>"
            + "<li>Your contacts, messages, or phone logs</li>"
            + "<li>Biometric data of any kind</li>"
            + "<li>Financial information</li>"
            + "</ul>"

            + "<h2>2. How We Use Your Information</h2>"
            + "<ul>"
            + "<li>To resolve and download video streams from supported platforms</li>"
            + "<li>To display video content locally on your device</li>"
            + "<li>To improve app performance and fix bugs</li>"
            + "<li>To provide technical support when requested</li>"
            + "</ul>"

            + "<h2>3. Data Storage &amp; Security</h2>"
            + "<p>All your downloaded files are stored <strong>locally on your device</strong> in the <code>Downloads/VidViewer/</code> folder. We do not upload your files to any server.</p>"
            + "<ul>"
            + "<li>Video files: stored locally, never uploaded</li>"
            + "<li>App settings: stored in Android SharedPreferences (local)</li>"
            + "<li>Download history: stored locally in app database</li>"
            + "</ul>"

            + "<h2>4. Third-Party Services</h2>"
            + "<p>VidéViewer contacts the following third-party services to resolve video streams:</p>"
            + "<ul>"
            + "<li><strong>Invidious instances</strong> — open-source YouTube frontend APIs (no account needed)</li>"
            + "<li><strong>Piped API</strong> — open-source YouTube API alternative</li>"
            + "<li><strong>YouTube CDN</strong> — for loading video thumbnails only</li>"
            + "</ul>"
            + "<p>These services have their own privacy policies. We recommend reviewing them separately.</p>"

            + "<h2>5. Permissions Explained</h2>"
            + "<ul>"
            + "<li><strong>INTERNET</strong> — to stream and download videos</li>"
            + "<li><strong>READ/WRITE STORAGE</strong> — to save downloaded videos to your device</li>"
            + "<li><strong>READ_MEDIA_VIDEO</strong> — to scan and display videos from your device</li>"
            + "<li><strong>FOREGROUND_SERVICE</strong> — to keep downloads running in background</li>"
            + "<li><strong>POST_NOTIFICATIONS</strong> — to show download progress notifications</li>"
            + "</ul>"

            + "<h2>6. Children's Privacy</h2>"
            + "<p>VidéViewer is not directed to children under the age of 13. We do not knowingly collect personal information from children. If you are a parent or guardian and believe your child has provided us with personal information, please contact us immediately.</p>"

            + "<h2>7. Changes to This Policy</h2>"
            + "<p>We may update this Privacy Policy periodically. We will notify you of significant changes by updating the 'Last updated' date. Continued use of the app after changes constitutes acceptance of the updated policy.</p>"

            + "<h2>8. Contact</h2>"
            + "<p>For privacy-related inquiries, please contact us at <a href='mailto:support@videviewer.app'>support@videviewer.app</a></p>"
            + "<p class='version'>VidéViewer v3.0.0 &nbsp;&middot;&nbsp; &copy; 2025 VidéViewer Team</p>";
    }

    // ── Terms & Conditions ─────────────────────────────────────
    private String buildTerms() {
        return "<h1>Terms &amp; Conditions</h1>"
            + "<p><em>Last updated: June 26, 2025 &nbsp;|&nbsp; Effective: July 1, 2025</em></p>"
            + "<div class='info-box'>Please read these Terms carefully before using VidéViewer. By installing or using the app, you agree to be bound by these terms.</div>"

            + "<h2>1. Acceptance of Terms</h2>"
            + "<p>By downloading, installing, or using VidéViewer ('the App', 'Service'), you agree to comply with and be bound by these Terms and Conditions. If you do not agree with any part of these terms, you must not use the App.</p>"

            + "<h2>2. Description of Service</h2>"
            + "<p>VidéViewer is a video player and downloader application for Android devices. The App allows users to:</p>"
            + "<ul>"
            + "<li>Play video files stored on their device</li>"
            + "<li>Browse the web and discover videos</li>"
            + "<li>Download videos from supported platforms for personal, offline viewing</li>"
            + "<li>Organise and manage their video library</li>"
            + "</ul>"

            + "<h2>3. User Responsibilities</h2>"
            + "<h3>3.1 Lawful Use</h3>"
            + "<p>You agree to use VidéViewer only for lawful purposes and in accordance with these Terms. You are solely responsible for ensuring your use complies with all applicable laws.</p>"
            + "<h3>3.2 Copyright Compliance</h3>"
            + "<p>You acknowledge that downloading videos may be subject to copyright restrictions. You agree to:</p>"
            + "<ul>"
            + "<li>Only download content you have the legal right to download</li>"
            + "<li>Not distribute, sell, or commercially exploit downloaded content</li>"
            + "<li>Respect the intellectual property rights of content creators</li>"
            + "<li>Comply with the terms of service of the platforms you access</li>"
            + "</ul>"
            + "<h3>3.3 Prohibited Uses</h3>"
            + "<p>You must not:</p>"
            + "<ul>"
            + "<li>Use the App for any illegal purpose</li>"
            + "<li>Download, store, or distribute copyrighted material without authorisation</li>"
            + "<li>Attempt to reverse-engineer, modify, or distribute the App</li>"
            + "<li>Use the App to access or download harmful, abusive, or offensive content</li>"
            + "<li>Circumvent any technical measures implemented by content platforms</li>"
            + "</ul>"

            + "<h2>4. Intellectual Property</h2>"
            + "<p>VidéViewer and its original content, features, and functionality are owned by the VidéViewer Team and are protected by international copyright, trademark, and other intellectual property laws.</p>"

            + "<h2>5. Third-Party Content</h2>"
            + "<p>VidéViewer provides tools to access third-party video content. We do not host, own, or control any third-party content. We are not responsible for the content, policies, or practices of third-party platforms.</p>"

            + "<h2>6. Disclaimer of Warranties</h2>"
            + "<p>The App is provided 'AS IS' and 'AS AVAILABLE' without warranties of any kind, either express or implied, including but not limited to implied warranties of merchantability, fitness for a particular purpose, or non-infringement.</p>"

            + "<h2>7. Limitation of Liability</h2>"
            + "<p>To the maximum extent permitted by law, VidéViewer Team shall not be liable for any indirect, incidental, special, consequential, or punitive damages, including loss of profits, data, or goodwill, arising from your use of the App.</p>"

            + "<h2>8. Changes to Terms</h2>"
            + "<p>We reserve the right to modify these Terms at any time. Material changes will be communicated through the App. Continued use after changes constitutes acceptance.</p>"

            + "<h2>9. Governing Law</h2>"
            + "<p>These Terms shall be governed by and construed in accordance with applicable laws, without regard to conflict of law provisions.</p>"

            + "<h2>10. Contact</h2>"
            + "<p>Questions about the Terms should be sent to <a href='mailto:legal@videviewer.app'>legal@videviewer.app</a></p>"
            + "<p class='version'>VidéViewer v3.0.0 &nbsp;&middot;&nbsp; &copy; 2025 VidéViewer Team</p>";
    }

    // ── About Us ───────────────────────────────────────────────
    private String buildAbout() {
        return "<h1>About VidéViewer</h1>"

            + "<div style='text-align:center;padding:24px 0'>"
            + "<div style='font-size:60px'>🎬</div>"
            + "<div style='font-size:26px;color:#c084fc;font-weight:bold;margin:8px 0'>VidéViewer</div>"
            + "<div style='color:#888'>The Ultimate Video Experience</div>"
            + "</div>"

            + "<div style='display:flex;flex-wrap:wrap;justify-content:center'>"
            + "<span class='badge'>📦 Version 3.0.0</span>"
            + "<span class='badge'>🤖 Android 5.0+</span>"
            + "<span class='badge'>🔓 Free &amp; Open</span>"
            + "</div>"

            + "<h2>Our Mission</h2>"
            + "<p>VidéViewer was created with one goal: to give you complete control over your video experience. We believe you should be able to watch your favourite videos anytime, anywhere — even without an internet connection.</p>"

            + "<h2>Key Features</h2>"
            + "<ul>"
            + "<li>🎬 <strong>Powerful Player</strong> — supports MP4, MKV, WebM, AVI, MOV and more via ExoPlayer</li>"
            + "<li>📥 <strong>Smart Downloads</strong> — download YouTube and other videos for offline viewing</li>"
            + "<li>🌐 <strong>Built-in Browser</strong> — discover and download videos from any website</li>"
            + "<li>🗂 <strong>Video Library</strong> — beautiful organised view of all your videos</li>"
            + "<li>🔒 <strong>Private Vault</strong> — password-protected folder for sensitive content</li>"
            + "<li>📋 <strong>Playlists</strong> — create and manage custom playlists</li>"
            + "<li>🌙 <strong>Dark Theme</strong> — easy on the eyes, day and night</li>"
            + "<li>🚫 <strong>No Ads (coming soon)</strong> — clean, distraction-free experience</li>"
            + "</ul>"

            + "<h2>Technology</h2>"
            + "<p>VidéViewer is built with modern Android technologies:</p>"
            + "<ul>"
            + "<li><strong>ExoPlayer / Media3</strong> — Google's professional media player library</li>"
            + "<li><strong>Android DownloadManager</strong> — reliable system-level downloads</li>"
            + "<li><strong>Glide</strong> — fast &amp; efficient image loading</li>"
            + "<li><strong>Room Database</strong> — local data persistence</li>"
            + "<li><strong>Invidious &amp; Piped APIs</strong> — open-source YouTube stream resolution</li>"
            + "</ul>"

            + "<h2>Version History</h2>"
            + "<ul>"
            + "<li><strong>v3.0.0</strong> — Download system rewrite, Invidious/Piped API, Legal pages, Stability fixes</li>"
            + "<li><strong>v2.0.0</strong> — YouTube thumbnail support, Delete downloads, Browser FAB</li>"
            + "<li><strong>v1.0.0</strong> — Initial release</li>"
            + "</ul>"

            + "<h2>Open Source Credits</h2>"
            + "<p>VidéViewer benefits from the following open-source projects: ExoPlayer, Glide, Room, OkHttp, Invidious, and the Piped project.</p>"

            + "<p class='version'>VidéViewer v3.0.0 &nbsp;&middot;&nbsp; &copy; 2025 VidéViewer Team &nbsp;&middot;&nbsp; Made with ❤️</p>";
    }

    // ── Contact Us ─────────────────────────────────────────────
    private String buildContact() {
        return "<h1>Contact Us</h1>"
            + "<p>We'd love to hear from you! Whether you have a question, found a bug, or just want to say hello — reach out through any of the channels below.</p>"

            + "<div class='contact-card'>"
            + "<strong>📧 General Support</strong><br>"
            + "<a href='mailto:support@videviewer.app'>support@videviewer.app</a><br>"
            + "<span style='color:#888;font-size:13px'>Response within 1-3 business days</span>"
            + "</div>"

            + "<div class='contact-card'>"
            + "<strong>🐛 Bug Reports</strong><br>"
            + "<a href='mailto:bugs@videviewer.app'>bugs@videviewer.app</a><br>"
            + "<span style='color:#888;font-size:13px'>Please include your Android version and a description of the issue</span>"
            + "</div>"

            + "<div class='contact-card'>"
            + "<strong>💡 Feature Requests</strong><br>"
            + "<a href='mailto:feedback@videviewer.app'>feedback@videviewer.app</a><br>"
            + "<span style='color:#888;font-size:13px'>We read every suggestion!</span>"
            + "</div>"

            + "<div class='contact-card'>"
            + "<strong>⚖️ Legal &amp; Privacy</strong><br>"
            + "<a href='mailto:legal@videviewer.app'>legal@videviewer.app</a><br>"
            + "<span style='color:#888;font-size:13px'>For DMCA notices, privacy concerns, and legal inquiries</span>"
            + "</div>"

            + "<h2>How to Report a Bug</h2>"
            + "<p>To help us fix your issue quickly, please include:</p>"
            + "<ul>"
            + "<li>Your Android version (e.g. Android 13)</li>"
            + "<li>Your device model (e.g. Samsung Galaxy S21)</li>"
            + "<li>VidéViewer version (v3.0.0)</li>"
            + "<li>Step-by-step description of what happened</li>"
            + "<li>What you expected to happen</li>"
            + "<li>Screenshot or screen recording (if possible)</li>"
            + "</ul>"

            + "<h2>Response Times</h2>"
            + "<ul>"
            + "<li>🟢 <strong>Bug reports:</strong> 24-48 hours</li>"
            + "<li>🟡 <strong>General support:</strong> 1-3 business days</li>"
            + "<li>🔵 <strong>Feature requests:</strong> 5-7 business days</li>"
            + "<li>🔴 <strong>Legal inquiries:</strong> 3-5 business days</li>"
            + "</ul>"

            + "<p class='version'>VidéViewer v3.0.0 &nbsp;&middot;&nbsp; &copy; 2025 VidéViewer Team</p>";
    }

    // ── Disclaimer ─────────────────────────────────────────────
    private String buildDisclaimer() {
        return "<h1>Disclaimer</h1>"
            + "<p><em>Last updated: June 26, 2025</em></p>"
            + "<div class='info-box'><strong>Important:</strong> Please read this disclaimer carefully before using VidéViewer to download videos.</div>"

            + "<h2>1. General Disclaimer</h2>"
            + "<p>VidéViewer ('the App') is provided for personal, educational, and informational purposes only. The developers and contributors of VidéViewer make no representations or warranties of any kind, express or implied, about the completeness, accuracy, reliability, suitability, or availability of the App.</p>"

            + "<h2>2. Copyright &amp; Intellectual Property</h2>"
            + "<p>VidéViewer is a tool that facilitates access to video content. <strong>We do not host, store, or distribute any copyrighted video content</strong>. All videos remain on their original hosting platforms or your own device.</p>"
            + "<p>Users are solely responsible for ensuring that any content they download complies with:</p>"
            + "<ul>"
            + "<li>The copyright laws of their country</li>"
            + "<li>The terms of service of the content platform</li>"
            + "<li>The licensing terms of the specific content</li>"
            + "</ul>"
            + "<p>Downloading copyrighted content without authorisation from the copyright holder may be illegal in your jurisdiction. <strong>VidéViewer expressly prohibits using the App to infringe copyright.</strong></p>"

            + "<h2>3. Platform Terms of Service</h2>"
            + "<p>Downloading content from platforms like YouTube may violate those platforms' Terms of Service. VidéViewer is not affiliated with, endorsed by, or connected to YouTube, Google, or any other video platform. Use of VidéViewer to access third-party platforms is at your own risk.</p>"

            + "<h2>4. No Warranty</h2>"
            + "<p>THE APP IS PROVIDED 'AS IS' WITHOUT WARRANTY OF ANY KIND. THE DEVELOPERS EXPRESSLY DISCLAIM ALL WARRANTIES, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, INCLUDING ANY WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.</p>"
            + "<p>We do not warrant that:</p>"
            + "<ul>"
            + "<li>The App will function without interruption or errors</li>"
            + "<li>Video stream resolution will always succeed</li>"
            + "<li>Third-party APIs (Invidious, Piped) will remain available</li>"
            + "<li>Downloaded content will be of any specific quality</li>"
            + "</ul>"

            + "<h2>5. Limitation of Liability</h2>"
            + "<p>To the fullest extent permitted by applicable law, the developers of VidéViewer shall not be liable for any:</p>"
            + "<ul>"
            + "<li>Direct, indirect, incidental, or consequential damages</li>"
            + "<li>Loss of data, profits, or business opportunities</li>"
            + "<li>Legal consequences arising from your use of the App</li>"
            + "<li>Third-party claims against you related to your use of the App</li>"
            + "</ul>"

            + "<h2>6. Third-Party Services</h2>"
            + "<p>VidéViewer uses third-party APIs to resolve video streams. These include Invidious and Piped, which are independent open-source projects. We have no control over these services and are not responsible for their availability, accuracy, or compliance with third-party platform terms.</p>"

            + "<h2>7. User Accountability</h2>"
            + "<p>By using VidéViewer, you accept full responsibility for your actions, including any legal consequences that may arise from downloading or sharing video content. VidéViewer and its developers cannot be held liable for your use of the App.</p>"

            + "<h2>8. Changes</h2>"
            + "<p>This disclaimer may be updated at any time. Continued use of the App after changes constitutes acceptance of the updated disclaimer.</p>"

            + "<p class='version'>VidéViewer v3.0.0 &nbsp;&middot;&nbsp; &copy; 2025 VidéViewer Team</p>";
    }
}
