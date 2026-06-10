package com.videviewer.activities;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) return;

        if (savedInstanceState == null) {
            loadFragment(new VideosFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_videos)    return loadFragment(new VideosFragment());
            else if (id == R.id.nav_folders)   return loadFragment(new FoldersFragment());
            else if (id == R.id.nav_favorites) return loadFragment(new FavoritesFragment());
            else if (id == R.id.nav_recent)    return loadFragment(new RecentFragment());
            else if (id == R.id.nav_more)      return loadFragment(new MoreFragment());
            return false;
        });
    }

    private boolean loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitAllowingStateLoss();
        return true;
    }
}
