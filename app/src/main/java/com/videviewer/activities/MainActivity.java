package com.videviewer.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
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

    private long lastNavClickTime = 0;
    private static final long NAV_DEBOUNCE_MS = 300;

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
            // Handle share intent on cold start
            String sharedUrl = extractSharedUrl(getIntent());
            if (sharedUrl != null) {
                showFragment(TAG_DOWNLOADS, makeDownloadsFragmentWithUrl(sharedUrl));
                currentNavId = R.id.nav_downloads;
                bottomNav.setSelectedItemId(R.id.nav_downloads);
            } else {
                showFragment(TAG_VIDEOS, new VideosFragment());
                currentNavId = R.id.nav_videos;
            }
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

            String tag; Fragment fragment;
            if      (id == R.id.nav_videos)    { tag = TAG_VIDEOS;    fragment = new VideosFragment(); }
            else if (id == R.id.nav_browser)   { tag = TAG_BROWSER;   fragment = new BrowserFragment(); }
            else if (id == R.id.nav_downloads) { tag = TAG_DOWNLOADS; fragment = new DownloadsFragment(); }
            else if (id == R.id.nav_storage)   { tag = TAG_STORAGE;   fragment = new StorageFragment(); }
            else if (id == R.id.nav_more)      { tag = TAG_MORE;      fragment = new MoreFragment(); }
            else return false;

            currentNavId = id;
            showFragment(tag, fragment);
            return true;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle share intent while app is already open
        String sharedUrl = extractSharedUrl(intent);
        if (sharedUrl != null && bottomNav != null) {
            showFragment(TAG_DOWNLOADS, makeDownloadsFragmentWithUrl(sharedUrl));
            currentNavId = R.id.nav_downloads;
            bottomNav.setSelectedItemId(R.id.nav_downloads);
        }
    }

    private String extractSharedUrl(Intent intent) {
        if (intent == null) return null;
        String action = intent.getAction();
        String type   = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null && (text.startsWith("http://") || text.startsWith("https://"))) {
                return text.trim();
            }
        }
        return null;
    }

    private DownloadsFragment makeDownloadsFragmentWithUrl(String url) {
        DownloadsFragment frag = new DownloadsFragment();
        Bundle args = new Bundle();
        args.putString("share_url", url);
        frag.setArguments(args);
        return frag;
    }

    private void showFragment(String tag, Fragment newInstance) {
        try {
            androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
            Fragment existing = fm.findFragmentByTag(tag);
            androidx.fragment.app.FragmentTransaction tx = fm.beginTransaction();
            tx.setReorderingAllowed(true);
            for (Fragment f : fm.getFragments())
                if (f != null && f.isAdded()) tx.hide(f);
            if (existing != null) tx.show(existing);
            else                  tx.add(R.id.fragment_container, newInstance, tag);
            tx.commitAllowingStateLoss();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("current_nav", currentNavId);
    }

    @Override public void onBackPressed() {
        if (bottomNav != null && currentNavId != R.id.nav_videos) {
            currentNavId = R.id.nav_videos;
            bottomNav.setSelectedItemId(R.id.nav_videos);
        } else {
            super.onBackPressed();
        }
    }
}
