package com.videviewer.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.videviewer.R;
import com.videviewer.utils.AppConstants;

/**
 * ContactActivity - Email/feedback/bug report form
 */
public class ContactActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etSubject, etMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.contact_us);
        }

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etSubject = findViewById(R.id.et_subject);
        etMessage = findViewById(R.id.et_message);

        MaterialButton btnSend = findViewById(R.id.btn_send);
        MaterialButton btnBugReport = findViewById(R.id.btn_bug_report);
        MaterialButton btnFeedback = findViewById(R.id.btn_feedback);

        if (btnSend != null) btnSend.setOnClickListener(v -> sendEmail("Support Request"));
        if (btnBugReport != null) btnBugReport.setOnClickListener(v -> {
            if (etSubject != null) etSubject.setText(getString(R.string.bug_report_prefix));
            sendEmail("Bug Report");
        });
        if (btnFeedback != null) btnFeedback.setOnClickListener(v -> {
            if (etSubject != null) etSubject.setText(getString(R.string.feedback_prefix));
            sendEmail("Feedback");
        });
    }

    private void sendEmail(String type) {
        String name = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String subject = etSubject != null && etSubject.getText() != null ? etSubject.getText().toString().trim() : type;
        String message = etMessage != null && etMessage.getText() != null ? etMessage.getText().toString().trim() : "";

        if (message.isEmpty()) {
            if (etMessage != null) etMessage.setError(getString(R.string.message_required));
            return;
        }

        String emailBody = "Name: " + name + "\nEmail: " + email + "\n\n" + message;

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{AppConstants.DEV_EMAIL});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[VidéViewer] " + subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);

        try {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.no_email_app, Toast.LENGTH_SHORT).show();
        }
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
