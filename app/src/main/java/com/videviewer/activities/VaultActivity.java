package com.videviewer.activities;

  import android.content.SharedPreferences;
  import android.os.Bundle;
  import android.view.View;
  import android.widget.Toast;
  import androidx.appcompat.app.AppCompatActivity;
  import androidx.recyclerview.widget.GridLayoutManager;
  import com.videviewer.R;
  import com.videviewer.databinding.ActivityVaultBinding;
  import com.videviewer.utils.AppConstants;
  import java.io.File;
  import java.util.ArrayList;

  public class VaultActivity extends AppCompatActivity {
      private ActivityVaultBinding binding;
      private boolean isUnlocked = false;
      private String savedPin;

      @Override
      protected void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          binding = ActivityVaultBinding.inflate(getLayoutInflater());
          setContentView(binding.getRoot());

          SharedPreferences prefs = getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE);
          savedPin = prefs.getString(AppConstants.PREF_VAULT_PIN, null);

          if (savedPin == null) setupNewPin();
          else showPinEntry();

          binding.btnBack.setOnClickListener(v -> onBackPressed());
      }

      private void setupNewPin() {
          binding.tvPrompt.setText("Set a new PIN for your vault");
          binding.etPin.setHint("Enter 4-digit PIN");
          binding.btnConfirm.setText("Set PIN");
          binding.btnConfirm.setOnClickListener(v -> {
              String pin = binding.etPin.getText().toString();
              if (pin.length() < 4) { Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show(); return; }
              getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE).edit().putString(AppConstants.PREF_VAULT_PIN, pin).apply();
              savedPin = pin;
              Toast.makeText(this, "Vault PIN set!", Toast.LENGTH_SHORT).show();
              unlockVault();
          });
      }

      private void showPinEntry() {
          binding.tvPrompt.setText("Enter your vault PIN");
          binding.btnConfirm.setText("Unlock");
          binding.btnConfirm.setOnClickListener(v -> {
              String entered = binding.etPin.getText().toString();
              if (entered.equals(savedPin)) unlockVault();
              else {
                  binding.etPin.setText("");
                  Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show();
                  binding.pinLayout.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake));
              }
          });
      }

      private void unlockVault() {
          isUnlocked = true;
          binding.lockLayout.setVisibility(View.GONE);
          binding.vaultContent.setVisibility(View.VISIBLE);
          loadVaultVideos();
      }

      private void loadVaultVideos() {
          File vaultDir = new File(getFilesDir(), AppConstants.VAULT_DIR);
          vaultDir.mkdirs();
          File[] files = vaultDir.listFiles();
          if (files == null || files.length == 0) {
              binding.tvEmpty.setVisibility(View.VISIBLE);
          }
      }

      @Override
      protected void onPause() {
          super.onPause();
          if (isUnlocked) {
              isUnlocked = false;
              binding.lockLayout.setVisibility(View.VISIBLE);
              binding.vaultContent.setVisibility(View.GONE);
              binding.etPin.setText("");
          }
      }
  }