package com.videviewer.utils;

import android.app.Activity;
import android.widget.FrameLayout;

/**
 * AdManager stub — AdMob removed until a valid google-services.json is available.
 * All methods are no-ops so the rest of the codebase compiles without changes.
 */
public class AdManager {

    public AdManager(android.content.Context context) {}

    public boolean isAdsEnabled()         { return false; }
    public boolean isInterstitialAdReady(){ return false; }
    public boolean isRewardedAdReady()    { return false; }

    public void loadBannerAd(FrameLayout container) {
        if (container != null) container.setVisibility(android.view.View.GONE);
    }

    public void loadInterstitialAd() {}
    public void loadRewardedAd()     {}

    public void showInterstitialAd(Activity activity, Runnable afterAdClosed) {
        if (afterAdClosed != null) afterAdClosed.run();
    }

    public interface RewardListener { void onRewarded(); }

    public void showRewardedAd(Activity activity, Object rewardListener, Runnable onFailed) {
        if (onFailed != null) onFailed.run();
    }
}
