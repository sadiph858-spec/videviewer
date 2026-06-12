package com.videviewer.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.videviewer.R;
import com.videviewer.fragments.*;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 100;
    private BottomNavigationView bottomNav;
    private VideosFragment videosFragment;
    private FoldersFragment foldersFragment;
    private FavoritesFragment favoritesFragment;
    private RecentFragment recentFragment;
    private MoreFragment moreFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) setSupportActionBar(toolbar);
            bottomNav = findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setOnItemSelectedListener(this::onNavSelected);
            }
            requestPermissions();
            if (savedInstanceState == null) {
                loadFragment(getVideosFragment(), "Videos");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestPermissions() {
        try {
            String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_VIDEO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{permission}, PERMISSION_CODE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean onNavSelected(MenuItem item) {
        try {
            int id = item.getItemId();
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
            e.printStackTrace();
        }
        return false;
    }

    private void loadFragment(Fragment fragment, String title) {
        try {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (videosFragment != null) videosFragment.onPermissionResult();
        }
    }

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
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.menu_main, menu);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            int id = item.getItemId();
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
            e.printStackTrace();
        }
        return super.onOptionsItemSelected(item);
    }
}
