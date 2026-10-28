package com.nunz.paperspaceaccess;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final long STATUS_INTERVAL_MS = 5000;

    private final PaperspaceClient client = new PaperspaceClient();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Settings settings;
    private TextView statusText;

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
        client.send(settings.getMachineId(), settings.getApiKey(), action,
                new PaperspaceClient.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, R.string.command_sent, Toast.LENGTH_SHORT).show();
                        refreshStatus();
                        WidgetWorker.follow(MainActivity.this, action);
                    }

                    @Override
                    public void onFailure(String reason) {
                        Toast.makeText(MainActivity.this,
                                getString(R.string.command_failed, reason), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void refreshStatus() {
        client.getStatus(settings.getMachineId(), settings.getApiKey(),
                new PaperspaceClient.StatusCallback() {
                    @Override
                    public void onState(String state) {
                        setStatus(state);
                    }

                    @Override
                    public void onHttpError(int code) {
                        setStatus(getString(R.string.status_error, code));
                    }

                    @Override
                    public void onUnreachable() {
                        setStatus(getString(R.string.unreachable));
                    }
                });
    }

    private void setStatus(String status) {
        statusText.setText(getString(R.string.machine_status, status));
    }
}
