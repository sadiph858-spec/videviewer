package com.videviewer.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.videviewer.R;
import com.videviewer.fragments.*;
import com.videviewer.utils.AdManager;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.PermissionHelper;

/**
 * MainActivity - Central hub with BottomNavigationView.
 * All fragment creation and ad loading are guarded against NPE and
 * unexpected exceptions to prevent launch crashes.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNav;
    private MaterialToolbar toolbar;
    private FrameLayout adContainer;
    private AdManager adManager;
    private SharedPreferences prefs;

    private VideosFragment    videosFragment;
    private FoldersFragment   foldersFragment;
    private FavoritesFragment favoritesFragment;
    private RecentFragment    recentFragment;
    private MoreFragment      moreFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);

        try {
            adManager = new AdManager(this);
        } catch (Exception e) {
            Log.e(TAG, "AdManager init failed", e);
            adManager = null;
        }

        initViews();
        setupToolbar();
        setupBottomNav();
        setupBannerAd();

        if (!PermissionHelper.hasStoragePermission(this)) {
            PermissionHelper.requestStoragePermissions(this,
                AppConstants.REQUEST_PERMISSION_STORAGE);
        }

        if (adManager != null) {
            try { adManager.loadInterstitialAd(); } catch (Exception e) {
                Log.e(TAG, "Interstitial preload failed", e);
            }
        }

        if (savedInstanceState == null) {
            loadFragment(getVideosFragment(), getString(R.string.nav_videos));
        }
    }

    private void initViews() {
        toolbar    = findViewById(R.id.toolbar);
        bottomNav  = findViewById(R.id.bottom_nav);
        adContainer = findViewById(R.id.ad_container_banner);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            try { setSupportActionBar(toolbar); } catch (Exception e) {
                Log.e(TAG, "Toolbar setup failed", e);
            }
        }
    }

    private void setupBottomNav() {
        if (bottomNav == null) return;
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            try {
                if (id == R.id.nav_videos) {
                    loadFragment(getVideosFragment(), getString(R.string.nav_videos));
                    return true;
                } else if (id == R.id.nav_folders) {
                    loadFragment(getFoldersFragment(), getString(R.string.nav_folders));
                    return true;
                } else if (id == R.id.nav_favorites) {
                    loadFragment(getFavoritesFragment(), getString(R.string.nav_favorites));
                    return true;
                } else if (id == R.id.nav_recent) {
                    loadFragment(getRecentFragment(), getString(R.string.nav_recent));
                    return true;
                } else if (id == R.id.nav_more) {
                    loadFragment(getMoreFragment(), getString(R.string.nav_more));
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Fragment switch failed", e);
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment, String title) {
        if (fragment == null) return;
        try {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
            getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_fade_enter, R.anim.fragment_fade_exit)
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();
        } catch (Exception e) {
            Log.e(TAG, "loadFragment failed for " + title, e);
        }
    }

    private void setupBannerAd() {
        if (adManager == null || adContainer == null) return;
        try { adManager.loadBannerAd(adContainer); } catch (Exception e) {
            Log.e(TAG, "Banner ad setup failed", e);
            adContainer.setVisibility(View.GONE);
        }
    }

    // ── Fragment factory (lazy, null-safe) ───────────────────────────────────
    private VideosFragment getVideosFragment() {
        if (videosFragment == null) videosFragment = new VideosFragment();
        return videosFragment;
    }
    private FoldersFragment getFoldersFragment() {
        if (foldersFragment == null) foldersFragment = new FoldersFragment();
        return foldersFragment;
    }
    private FavoritesFragment getFavoritesFragment() {
        if (favoritesFragment == null) favoritesFragment = new FavoritesFragment();
        return favoritesFragment;
    }
    private RecentFragment getRecentFragment() {
        if (recentFragment == null) recentFragment = new RecentFragment();
        return recentFragment;
    }
    private MoreFragment getMoreFragment() {
        if (moreFragment == null) moreFragment = new MoreFragment();
        return moreFragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try { getMenuInflater().inflate(R.menu.menu_main, menu); } catch (Exception ignored) {}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        try {
            if (id == R.id.action_search) {
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            } else if (id == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            } else if (id == R.id.action_vault) {
                startActivity(new Intent(this, VaultActivity.class));
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Menu action failed", e);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppConstants.REQUEST_PERMISSION_STORAGE) {
            if (videosFragment != null) {
                try { videosFragment.onPermissionResult(); } catch (Exception ignored) {}
            }
        }
    }
}
