package com.videviewer.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.videviewer.R;
import com.videviewer.fragments.BrowserFragment;
import com.videviewer.fragments.DownloadsFragment;
import com.videviewer.fragments.MoreFragment;
import com.videviewer.fragments.StorageFragment;
import com.videviewer.fragments.VideosFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private int currentNavId = R.id.nav_videos;

    // Debounce: ignore clicks within 300ms of each other
    private long lastNavClickTime = 0;
    private static final long NAV_DEBOUNCE_MS = 300;

    // Tags for fragment back-stack caching
    private static final String TAG_VIDEOS    = "tag_videos";
    private static final String TAG_BROWSER   = "tag_browser";
    private static final String TAG_DOWNLOADS = "tag_downloads";
    private static final String TAG_STORAGE   = "tag_storage";
    private static final String TAG_MORE      = "tag_more";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) return;

        if (savedInstanceState == null) {
            showFragment(TAG_VIDEOS, new VideosFragment());
            currentNavId = R.id.nav_videos;
        } else {
            currentNavId = savedInstanceState.getInt("current_nav", R.id.nav_videos);
            bottomNav.setSelectedItemId(currentNavId);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            long now = System.currentTimeMillis();
            if (now - lastNavClickTime < NAV_DEBOUNCE_MS) return false;
            lastNavClickTime = now;

            int id = item.getItemId();
            if (id == currentNavId) return true;

            String tag;
            Fragment fragment;

            if (id == R.id.nav_videos) {
                tag = TAG_VIDEOS; fragment = new VideosFragment();
            } else if (id == R.id.nav_browser) {
                tag = TAG_BROWSER; fragment = new BrowserFragment();
            } else if (id == R.id.nav_downloads) {
                tag = TAG_DOWNLOADS; fragment = new DownloadsFragment();
            } else if (id == R.id.nav_storage) {
                tag = TAG_STORAGE; fragment = new StorageFragment();
            } else if (id == R.id.nav_more) {
                tag = TAG_MORE; fragment = new MoreFragment();
            } else {
                return false;
            }

            currentNavId = id;
            showFragment(tag, fragment);
            return true;
        });
    }

    /**
     * Show a fragment by tag, reusing an existing instance if already added.
     * Uses commitAllowingStateLoss() to prevent IllegalStateException on rapid switching.
     */
    private void showFragment(String tag, Fragment newInstance) {
        try {
            FragmentManager fm = getSupportFragmentManager();

            // If already in back stack, pop to it (avoids duplicate transactions)
            Fragment existing = fm.findFragmentByTag(tag);

            androidx.fragment.app.FragmentTransaction tx = fm.beginTransaction();
            tx.setReorderingAllowed(true);

            // Hide all currently added fragments
            for (Fragment f : fm.getFragments()) {
                if (f != null && f.isAdded()) tx.hide(f);
            }

            if (existing != null) {
                tx.show(existing);
            } else {
                tx.add(R.id.fragment_container, newInstance, tag);
            }

            tx.commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_nav", currentNavId);
    }

    @Override
    public void onBackPressed() {
        if (bottomNav != null && currentNavId != R.id.nav_videos) {
            currentNavId = R.id.nav_videos;
            bottomNav.setSelectedItemId(R.id.nav_videos);
        } else {
            super.onBackPressed();
        }
    }
}
