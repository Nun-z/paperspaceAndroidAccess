package com.nunz.paperspaceaccess;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    private static final long STATUS_INTERVAL_MS = 5000;

    private final PaperspaceClient client = new PaperspaceClient();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Settings settings;
    private TextView statusText;
    private String lastState; // from the most recent status check, or null if it failed

    // Get the status every few seconds, while the app is in the foreground
    private final Runnable statusPoll = new Runnable() {
        @Override
        public void run() {
            refreshStatus();
            handler.postDelayed(this, STATUS_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = new Settings(this);
        statusText = findViewById(R.id.statusText);

        findViewById(R.id.startButton).setOnClickListener(v -> sendAction("start"));
        findViewById(R.id.stopButton).setOnClickListener(v -> sendAction("stop"));
        findViewById(R.id.restartButton).setOnClickListener(v -> sendAction("restart"));
        findViewById(R.id.settingsButton).setOnClickListener(v -> openSettings());

        if (savedInstanceState == null && !settings.isConfigured()) {
            openSettings();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (settings.isConfigured()) {
            setStatus(getString(R.string.getting_status));
            handler.post(statusPoll);
        } else {
            setStatus(getString(R.string.not_configured));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(statusPoll);
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void sendAction(String action) {
        if (!settings.isConfigured()) {
            openSettings();
            return;
        }
        int skipReason = PaperspaceClient.skipReason(action, lastState);
        if (skipReason != 0) {
            showMessage(getString(skipReason));
            return;
        }
        client.send(settings.getMachineId(), settings.getApiKey(), action,
                new PaperspaceClient.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        showMessage(getString(R.string.command_sent));
                        refreshStatus();
                        WidgetWorker.follow(MainActivity.this, action);
                    }

                    @Override
                    public void onFailure(String reason) {
                        showError(getString(R.string.command_failed, reason));
                    }
                });
    }

    private void showMessage(String message) {
        Snackbar.make(statusText, message, Snackbar.LENGTH_SHORT).show();
    }

    // Unlike a toast, a Snackbar can show a long error from the API in full; it stays until dismissed
    private void showError(String message) {
        Snackbar snackbar = Snackbar.make(statusText, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.setTextMaxLines(6);
        snackbar.setAction(android.R.string.ok, v -> { });
        snackbar.show();
    }

    private void refreshStatus() {
        client.getStatus(settings.getMachineId(), settings.getApiKey(),
                new PaperspaceClient.StatusCallback() {
                    @Override
                    public void onState(String state) {
                        lastState = state;
                        setStatus(state);
                    }

                    @Override
                    public void onHttpError(int code) {
                        lastState = null;
                        int credentialsError = PaperspaceClient.credentialsError(code);
                        if (credentialsError != 0) {
                            statusText.setText(credentialsError);
                        } else {
                            setStatus(getString(R.string.status_error, code));
                        }
                    }

                    @Override
                    public void onUnreachable() {
                        lastState = null;
                        setStatus(getString(R.string.unreachable));
                    }
                });
    }

    private void setStatus(String status) {
        statusText.setText(getString(R.string.machine_status, status));
    }
}
