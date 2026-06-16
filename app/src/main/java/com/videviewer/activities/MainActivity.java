package com.videviewer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.videviewer.R;
import com.videviewer.fragments.VideosFragment;
import com.videviewer.fragments.FoldersFragment;
import com.videviewer.fragments.FavoritesFragment;
import com.videviewer.fragments.RecentFragment;
import com.videviewer.fragments.MoreFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            bottomNav = findViewById(R.id.bottom_nav);
            if (bottomNav == null) return;

            if (savedInstanceState == null) {
                loadFragment(new VideosFragment());
            }

            bottomNav.setOnItemSelectedListener(item -> {
                try {
                    int id = item.getItemId();
                    Fragment fragment = null;
                    if (id == R.id.nav_videos) fragment = new VideosFragment();
                    else if (id == R.id.nav_folders) fragment = new FoldersFragment();
                    else if (id == R.id.nav_favorites) fragment = new FavoritesFragment();
                    else if (id == R.id.nav_recent) fragment = new RecentFragment();
                    else if (id == R.id.nav_more) fragment = new MoreFragment();
                    if (fragment != null) { loadFragment(fragment); return true; }
                } catch (Exception e) { e.printStackTrace(); }
                return false;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFragment(Fragment fragment) {
        try {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}