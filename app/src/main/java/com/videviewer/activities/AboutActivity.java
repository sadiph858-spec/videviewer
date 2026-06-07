package com.videviewer.activities;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.videviewer.R;
import com.videviewer.utils.AppConstants;

/**
 * AboutActivity - App info, version, developer details
 */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.about_us);
        }

        // Version info
        TextView tvVersion = findViewById(R.id.tv_version);
        if (tvVersion != null) {
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                tvVersion.setText(getString(R.string.version_format, pInfo.versionName));
            } catch (PackageManager.NameNotFoundException e) {
                tvVersion.setText(getString(R.string.version_format, "1.0.0"));
            }
        }

        // Rate App
        MaterialButton btnRate = findViewById(R.id.btn_rate_app);
        if (btnRate != null) {
            btnRate.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + getPackageName())));
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(AppConstants.PLAY_STORE_URL)));
                }
            });
        }

        // Share App
        MaterialButton btnShare = findViewById(R.id.btn_share_app);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT,
                    getString(R.string.share_app_text) + "\n" + AppConstants.PLAY_STORE_URL);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app)));
            });
        }

        // Website
        MaterialButton btnWebsite = findViewById(R.id.btn_website);
        if (btnWebsite != null) {
            btnWebsite.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.APP_WEBSITE))));
        }

        // Privacy Policy
        MaterialButton btnPrivacy = findViewById(R.id.btn_privacy_about);
        if (btnPrivacy != null) {
            btnPrivacy.setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyPolicyActivity.class)));
        }
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
