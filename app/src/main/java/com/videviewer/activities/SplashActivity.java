package com.videviewer.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.videviewer.R;
import com.videviewer.utils.LocaleHelper;
import com.videviewer.utils.ThemeHelper;

/**
 * SplashActivity - App launch screen with smooth logo animation
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        LocaleHelper.applyLocale(this);
        setContentView(R.layout.activity_splash);

        ImageView ivLogo = findViewById(R.id.iv_logo);
        TextView tvAppName = findViewById(R.id.tv_app_name);
        TextView tvTagline = findViewById(R.id.tv_tagline);

        // Logo scale-in animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivLogo, View.SCALE_X, 0.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivLogo, View.SCALE_Y, 0.3f, 1f);
        ObjectAnimator alphaLogo = ObjectAnimator.ofFloat(ivLogo, View.ALPHA, 0f, 1f);

        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(scaleX, scaleY, alphaLogo);
        logoAnim.setDuration(700);
        logoAnim.setInterpolator(new DecelerateInterpolator());
        logoAnim.start();

        // Text fade-in with delay
        tvAppName.setAlpha(0f);
        tvTagline.setAlpha(0f);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            tvAppName.animate().alpha(1f).setDuration(500).start();
            tvTagline.animate().alpha(1f).setDuration(500).setStartDelay(200).start();
        }, 500);

        // Navigate to main after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DELAY);
    }
}
