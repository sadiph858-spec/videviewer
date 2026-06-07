package com.videviewer.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.videviewer.R;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.VaultManager;

/**
 * LockSetupActivity - Configure PIN, Password, or Pattern for vault
 */
public class LockSetupActivity extends AppCompatActivity {

    private VaultManager vaultManager;
    private RadioGroup rgLockType;
    private TextInputEditText etCredential, etConfirm;
    private MaterialButton btnSave, btnRemoveLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_setup);

        vaultManager = VaultManager.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.lock_setup);
        }

        rgLockType = findViewById(R.id.rg_lock_type);
        etCredential = findViewById(R.id.et_credential);
        etConfirm = findViewById(R.id.et_confirm);
        btnSave = findViewById(R.id.btn_save_lock);
        btnRemoveLock = findViewById(R.id.btn_remove_lock);

        // Show remove button if lock already set
        btnRemoveLock.setVisibility(vaultManager.isLockSet() ? View.VISIBLE : View.GONE);

        btnSave.setOnClickListener(v -> saveLock());
        btnRemoveLock.setOnClickListener(v -> {
            vaultManager.removeLock();
            Toast.makeText(this, R.string.lock_removed, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void saveLock() {
        String cred = etCredential.getText() != null ? etCredential.getText().toString().trim() : "";
        String confirm = etConfirm.getText() != null ? etConfirm.getText().toString().trim() : "";

        if (cred.isEmpty()) {
            etCredential.setError(getString(R.string.enter_credentials));
            return;
        }
        if (!cred.equals(confirm)) {
            etConfirm.setError(getString(R.string.credentials_do_not_match));
            return;
        }

        int selectedId = rgLockType.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_pin) {
            if (cred.length() < AppConstants.PIN_MIN_LENGTH) {
                etCredential.setError(getString(R.string.pin_too_short));
                return;
            }
            vaultManager.setPin(cred);
        } else if (selectedId == R.id.rb_password) {
            vaultManager.setPassword(cred);
        } else {
            // Pattern - simplified as text pattern for now
            vaultManager.setPattern(cred);
        }

        Toast.makeText(this, R.string.lock_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
