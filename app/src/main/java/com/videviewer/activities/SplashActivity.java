package com.videviewer.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.videviewer.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 2200L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        try {
            View glowRing   = findViewById(R.id.glow_ring);
            ImageView logo  = findViewById(R.id.iv_logo);
            TextView name   = findViewById(R.id.tv_app_name);
            TextView tagline = findViewById(R.id.tv_tagline);
            CircularProgressIndicator spinner = findViewById(R.id.loading_spinner);
            TextView credit = findViewById(R.id.tv_credit);

            // --- Logo: scale from 0.5 → 1.0 and fade in (overshoot bounce) ---
            ObjectAnimator logoFade  = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f);
            ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.5f, 1f);
            ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.5f, 1f);
            AnimatorSet logoAnim = new AnimatorSet();
            logoAnim.playTogether(logoFade, logoScaleX, logoScaleY);
            logoAnim.setDuration(700);
            logoAnim.setStartDelay(100);
            logoAnim.setInterpolator(new OvershootInterpolator(1.2f));

            // --- Glow ring: fade in slowly ---
            ObjectAnimator glowFade = ObjectAnimator.ofFloat(glowRing, View.ALPHA, 0f, 1f);
            glowFade.setDuration(900);
            glowFade.setStartDelay(200);
            glowFade.setInterpolator(new DecelerateInterpolator());

            // --- Text: slide up + fade in ---
            if (name != null) {
                name.setTranslationY(24f);
                ObjectAnimator nameFade = ObjectAnimator.ofFloat(name, View.ALPHA, 0f, 1f);
                ObjectAnimator nameSlide = ObjectAnimator.ofFloat(name, View.TRANSLATION_Y, 24f, 0f);
                AnimatorSet nameAnim = new AnimatorSet();
                nameAnim.playTogether(nameFade, nameSlide);
                nameAnim.setDuration(500);
                nameAnim.setStartDelay(650);
                nameAnim.setInterpolator(new DecelerateInterpolator());
                nameAnim.start();
            }

            if (tagline != null) {
                tagline.setTranslationY(16f);
                ObjectAnimator tagFade = ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f);
                ObjectAnimator tagSlide = ObjectAnimator.ofFloat(tagline, View.TRANSLATION_Y, 16f, 0f);
                AnimatorSet tagAnim = new AnimatorSet();
                tagAnim.playTogether(tagFade, tagSlide);
                tagAnim.setDuration(500);
                tagAnim.setStartDelay(800);
                tagAnim.setInterpolator(new DecelerateInterpolator());
                tagAnim.start();
            }

            // --- Spinner + credit: fade in after logo settles ---
            if (spinner != null) {
                ObjectAnimator spinFade = ObjectAnimator.ofFloat(spinner, View.ALPHA, 0f, 1f);
                spinFade.setDuration(400);
                spinFade.setStartDelay(1000);
                spinFade.start();
            }
            if (credit != null) {
                ObjectAnimator creditFade = ObjectAnimator.ofFloat(credit, View.ALPHA, 0f, 0.8f);
                creditFade.setDuration(400);
                creditFade.setStartDelay(1100);
                creditFade.start();
            }

            logoAnim.start();
            glowFade.start();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Navigate to MainActivity after splash duration
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, SPLASH_DURATION_MS);
    }
}
