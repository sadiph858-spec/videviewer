package com.videviewer.activities;

  import android.content.SharedPreferences;
  import android.os.Bundle;
  import android.widget.Toast;
  import androidx.appcompat.app.AppCompatActivity;
  import com.videviewer.databinding.ActivitySettingsBinding;
  import com.videviewer.utils.AppConstants;
  import com.videviewer.utils.ThemeHelper;

  public class SettingsActivity extends AppCompatActivity {
      private ActivitySettingsBinding binding;
      private SharedPreferences prefs;

      @Override
      protected void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          binding = ActivitySettingsBinding.inflate(getLayoutInflater());
          setContentView(binding.getRoot());
          prefs = getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE);

          binding.btnBack.setOnClickListener(v -> onBackPressed());
          setupTheme();
          setupLanguage();
      }

      private void setupTheme() {
          int theme = prefs.getInt(AppConstants.PREF_THEME, 0);
          binding.rgTheme.check(theme == 1 ? binding.rbLight.getId() : theme == 2 ? binding.rbDark.getId() : binding.rbSystem.getId());
          binding.rgTheme.setOnCheckedChangeListener((group, id) -> {
              int val = id == binding.rbLight.getId() ? 1 : id == binding.rbDark.getId() ? 2 : 0;
              prefs.edit().putInt(AppConstants.PREF_THEME, val).apply();
              ThemeHelper.applyTheme(this);
          });
      }

      private void setupLanguage() {
          binding.btnClearHistory.setOnClickListener(v -> {
              new com.videviewer.database.AppDatabase.ClearHistoryTask(this).execute();
              Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
          });
      }
  }