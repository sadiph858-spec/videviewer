package com.videviewer.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.videviewer.R;
import com.videviewer.database.AppDatabase;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.LocaleHelper;
import com.videviewer.utils.ThemeHelper;
import java.util.concurrent.Executors;

/**
 * SettingsActivity - Theme, Playback, Ads, Language, Storage settings
 */
public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }

        setupThemeSettings();
        setupPlaybackSettings();
        setupLanguageSettings();
        setupAdSettings();
        setupStorageSettings();
    }

    private void setupThemeSettings() {
        MaterialButton btnLight = findViewById(R.id.btn_theme_light);
        MaterialButton btnDark = findViewById(R.id.btn_theme_dark);
        MaterialButton btnSystem = findViewById(R.id.btn_theme_system);
        MaterialButton btnDynamic = findViewById(R.id.btn_theme_dynamic);

        if (btnLight != null) btnLight.setOnClickListener(v -> applyTheme(AppConstants.THEME_LIGHT));
        if (btnDark != null) btnDark.setOnClickListener(v -> applyTheme(AppConstants.THEME_DARK));
        if (btnSystem != null) btnSystem.setOnClickListener(v -> applyTheme(AppConstants.THEME_SYSTEM));
        if (btnDynamic != null) btnDynamic.setOnClickListener(v -> applyTheme(AppConstants.THEME_DYNAMIC));
    }

    private void applyTheme(String theme) {
        ThemeHelper.saveTheme(this, theme);
        ThemeHelper.applyTheme(this);
        recreate();
    }

    private void setupPlaybackSettings() {
        SwitchMaterial switchResume = findViewById(R.id.switch_resume_playback);
        SwitchMaterial switchAutoPlay = findViewById(R.id.switch_auto_play);

        if (switchResume != null) {
            switchResume.setChecked(prefs.getBoolean(AppConstants.PREF_RESUME_PLAYBACK, true));
            switchResume.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(AppConstants.PREF_RESUME_PLAYBACK, checked).apply());
        }

        if (switchAutoPlay != null) {
            switchAutoPlay.setChecked(prefs.getBoolean(AppConstants.PREF_AUTO_PLAY, false));
            switchAutoPlay.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(AppConstants.PREF_AUTO_PLAY, checked).apply());
        }
    }

    private void setupLanguageSettings() {
        MaterialButton btnEnglish = findViewById(R.id.btn_lang_english);
        MaterialButton btnBangla = findViewById(R.id.btn_lang_bangla);

        if (btnEnglish != null) {
            btnEnglish.setOnClickListener(v -> {
                LocaleHelper.saveLanguage(this, AppConstants.LANG_ENGLISH);
                recreate();
            });
        }
        if (btnBangla != null) {
            btnBangla.setOnClickListener(v -> {
                LocaleHelper.saveLanguage(this, AppConstants.LANG_BANGLA);
                recreate();
            });
        }
    }

    private void setupAdSettings() {
        SwitchMaterial switchAds = findViewById(R.id.switch_ads_enabled);
        if (switchAds != null) {
            switchAds.setChecked(prefs.getBoolean(AppConstants.PREF_ADS_ENABLED, true));
            switchAds.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(AppConstants.PREF_ADS_ENABLED, checked).apply());
        }
    }

    private void setupStorageSettings() {
        MaterialButton btnClearHistory = findViewById(R.id.btn_clear_history);
        if (btnClearHistory != null) {
            btnClearHistory.setOnClickListener(v -> {
                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase.getInstance(this).historyDao().clearAll();
                    runOnUiThread(() ->
                        Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show());
                });
            });
        }

        MaterialButton btnVaultLock = findViewById(R.id.btn_vault_lock);
        if (btnVaultLock != null) {
            btnVaultLock.setOnClickListener(v ->
                startActivity(new Intent(this, LockSetupActivity.class)));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
