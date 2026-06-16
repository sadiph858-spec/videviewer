package com.videviewer.activities;

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
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) return;

        if (savedInstanceState == null) {
            loadFragment(new VideosFragment(), R.id.nav_videos);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment = null;
            if (id == R.id.nav_videos)    fragment = new VideosFragment();
            else if (id == R.id.nav_browser)   fragment = new BrowserFragment();
            else if (id == R.id.nav_downloads) fragment = new DownloadsFragment();
            else if (id == R.id.nav_storage)   fragment = new StorageFragment();
            else if (id == R.id.nav_more)      fragment = new MoreFragment();

            if (fragment != null) {
                loadFragment(fragment, id);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment, int navId) {
        try {
            currentFragment = fragment;
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_videos) {
            bottomNav.setSelectedItemId(R.id.nav_videos);
        } else {
            super.onBackPressed();
        }
    }
}
