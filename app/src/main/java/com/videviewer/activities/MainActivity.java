package com.videviewer.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.videviewer.R;
import com.videviewer.fragments.FavoritesFragment;
import com.videviewer.fragments.FoldersFragment;
import com.videviewer.fragments.MoreFragment;
import com.videviewer.fragments.RecentFragment;
import com.videviewer.fragments.VideosFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) return;

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment f = createFragment(item.getItemId());
            if (f == null) return false;
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, f)
                .commitAllowingStateLoss();
            return true;
        });

        // Restore tab selection on configuration change only.
        // On first launch, user taps a tab — no automatic fragment load
        // that could crash before the screen is even visible.
        if (savedInstanceState != null) {
            int selectedId = bottomNav.getSelectedItemId();
            Fragment f = createFragment(selectedId);
            if (f != null) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, f)
                    .commitAllowingStateLoss();
            }
        }
    }

    private Fragment createFragment(int itemId) {
        if      (itemId == R.id.nav_videos)    return new VideosFragment();
        else if (itemId == R.id.nav_folders)   return new FoldersFragment();
        else if (itemId == R.id.nav_favorites) return new FavoritesFragment();
        else if (itemId == R.id.nav_recent)    return new RecentFragment();
        else if (itemId == R.id.nav_more)      return new MoreFragment();
        return null;
    }
}
