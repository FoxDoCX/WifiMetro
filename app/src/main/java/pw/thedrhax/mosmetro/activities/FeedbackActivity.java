/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 */

package pw.thedrhax.mosmetro.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.sentry.Sentry;
import io.sentry.UserFeedback;
import io.sentry.protocol.SentryId;
import pw.thedrhax.mosmetro.MosMetroApp;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.ThemeUtils;

public class FeedbackActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.applyDialog(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_activity);
    }

    public void buttonCancel(View view) {
        finish();
    }

    public void buttonSend(View view) {
        EditText text_message = findViewById(R.id.text_message);
        String message = text_message.getText().toString();

        if (!MosMetroApp.isCrashReportingAvailable()) {
            Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Sentry.setTag("manual", "true");
        SentryId id = Sentry.captureMessage("Manual report");
        Sentry.removeTag("manual");

        if (message.length() > 0) {
            UserFeedback feedback = new UserFeedback(id);
            feedback.setComments(message);
            Sentry.captureUserFeedback(feedback);
        }

        Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show();
        finish();
    }
}
