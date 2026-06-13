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
  import com.google.android.material.bottomnavigation.BottomNavigationView;
  import com.videviewer.R;
  import com.videviewer.fragments.*;
  import com.videviewer.databinding.ActivityMainBinding;

  public class MainActivity extends AppCompatActivity {

      private ActivityMainBinding binding;
      private static final int PERMISSION_REQUEST_CODE = 100;

      @Override
      protected void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          binding = ActivityMainBinding.inflate(getLayoutInflater());
          setContentView(binding.getRoot());

          checkPermissions();
          setupBottomNav();

          if (savedInstanceState == null) {
              loadFragment(new VideosFragment());
          }
      }

      private void setupBottomNav() {
          binding.bottomNav.setOnItemSelectedListener(item -> {
              Fragment fragment = null;
              int id = item.getItemId();
              if (id == R.id.nav_videos) fragment = new VideosFragment();
              else if (id == R.id.nav_browser) fragment = new BrowserFragment();
              else if (id == R.id.nav_downloads) fragment = new DownloadsFragment();
              else if (id == R.id.nav_storage) fragment = new StorageFragment();
              else if (id == R.id.nav_more) fragment = new MoreFragment();
              if (fragment != null) { loadFragment(fragment); return true; }
              return false;
          });
      }

      private void loadFragment(Fragment fragment) {
          getSupportFragmentManager().beginTransaction()
              .replace(R.id.fragment_container, fragment)
              .commit();
      }

      private void checkPermissions() {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                  ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
              }
          } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                  ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
              }
          }
      }

      @Override
      public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
          super.onRequestPermissionsResult(requestCode, permissions, grantResults);
          if (requestCode == PERMISSION_REQUEST_CODE) {
              Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
              if (current instanceof VideosFragment) ((VideosFragment) current).loadVideos();
          }
      }
  }