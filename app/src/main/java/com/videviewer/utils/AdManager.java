package com.videviewer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * AdManager - Centralized AdMob management
 * Automatically hides ad containers when Ad IDs are empty or ads are disabled
 */
public class AdManager {

    private static final String TAG = "AdManager";

    private final Context context;
    private final SharedPreferences prefs;

    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;

    public AdManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Check if ads are enabled ─────────────────────────────────────────────
    public boolean isAdsEnabled() {
        return prefs.getBoolean(AppConstants.PREF_ADS_ENABLED, true);
    }

    // ── Ad ID helpers ────────────────────────────────────────────────────────
    private String getBannerAdId() {
        String id = prefs.getString(AppConstants.PREF_BANNER_AD_ID, "");
        return (id == null || id.isEmpty()) ? AppConstants.TEST_BANNER_AD_ID : id;
    }

    private String getInterstitialAdId() {
        String id = prefs.getString(AppConstants.PREF_INTERSTITIAL_AD_ID, "");
        return (id == null || id.isEmpty()) ? AppConstants.TEST_INTERSTITIAL_AD_ID : id;
    }

    private String getRewardedAdId() {
        String id = prefs.getString(AppConstants.PREF_REWARDED_AD_ID, "");
        return (id == null || id.isEmpty()) ? AppConstants.TEST_REWARDED_AD_ID : id;
    }

    // ── Banner Ad ────────────────────────────────────────────────────────────
    public void loadBannerAd(FrameLayout container) {
        if (!isAdsEnabled() || container == null) {
            if (container != null) container.setVisibility(View.GONE);
            return;
        }

        String adId = getBannerAdId();
        if (adId.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        container.setVisibility(View.VISIBLE);
        AdView adView = new AdView(context);
        adView.setAdUnitId(adId);
        adView.setAdSize(AdSize.BANNER);
        container.removeAllViews();
        container.addView(adView);

        AdRequest request = new AdRequest.Builder().build();
        adView.loadAd(request);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                container.setVisibility(View.GONE);
                Log.w(TAG, "Banner ad failed: " + error.getMessage());
            }
            @Override
            public void onAdLoaded() {
                container.setVisibility(View.VISIBLE);
            }
        });
    }

    // ── Interstitial Ad ──────────────────────────────────────────────────────
    public void loadInterstitialAd() {
        if (!isAdsEnabled()) return;

        String adId = getInterstitialAdId();
        if (adId.isEmpty()) return;

        AdRequest request = new AdRequest.Builder().build();
        InterstitialAd.load(context, adId, request, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                interstitialAd = ad;
            }
            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                interstitialAd = null;
                Log.w(TAG, "Interstitial failed: " + error.getMessage());
            }
        });
    }

    public void showInterstitialAd(Activity activity, Runnable afterAdClosed) {
        if (!isAdsEnabled() || interstitialAd == null) {
            if (afterAdClosed != null) afterAdClosed.run();
            return;
        }

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                loadInterstitialAd(); // preload next
                if (afterAdClosed != null) afterAdClosed.run();
            }
            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                interstitialAd = null;
                if (afterAdClosed != null) afterAdClosed.run();
            }
        });

        interstitialAd.show(activity);
    }

    // ── Rewarded Ad ──────────────────────────────────────────────────────────
    public void loadRewardedAd() {
        if (!isAdsEnabled()) return;

        String adId = getRewardedAdId();
        if (adId.isEmpty()) return;

        AdRequest request = new AdRequest.Builder().build();
        RewardedAd.load(context, adId, request, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
            }
            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                rewardedAd = null;
                Log.w(TAG, "Rewarded ad failed: " + error.getMessage());
            }
        });
    }

    public void showRewardedAd(Activity activity, OnUserEarnedRewardListener rewardListener, Runnable onFailed) {
        if (!isAdsEnabled() || rewardedAd == null) {
            if (onFailed != null) onFailed.run();
            return;
        }

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                loadRewardedAd();
            }
            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                rewardedAd = null;
                if (onFailed != null) onFailed.run();
            }
        });

        rewardedAd.show(activity, rewardListener);
    }

    public boolean isRewardedAdReady() {
        return isAdsEnabled() && rewardedAd != null;
    }

    public boolean isInterstitialAdReady() {
        return isAdsEnabled() && interstitialAd != null;
    }
}
