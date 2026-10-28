package com.nunz.paperspaceaccess;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    // The console redirects these to the signed-in user's own team pages
    private static final String MACHINES_URL = "https://console.paperspace.com/machines";
    private static final String API_KEYS_URL = "https://console.paperspace.com/settings/apikeys";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Settings settings = new Settings(this);
        EditText machineIdInput = findViewById(R.id.machineIdInput);
        EditText apiKeyInput = findViewById(R.id.apiKeyInput);

        if (savedInstanceState == null) {
            machineIdInput.setText(settings.getMachineId());
            apiKeyInput.setText(settings.getApiKey());
        }

        findViewById(R.id.machinesLink).setOnClickListener(v -> openUrl(MACHINES_URL));
        findViewById(R.id.apiKeysLink).setOnClickListener(v -> openUrl(API_KEYS_URL));

        findViewById(R.id.saveButton).setOnClickListener(v -> {
            String machineId = machineIdInput.getText().toString().trim();
            String apiKey = apiKeyInput.getText().toString().trim();
            if (machineId.isEmpty() || apiKey.isEmpty()) {
                Toast.makeText(this, R.string.fields_required, Toast.LENGTH_SHORT).show();
                return;
            }
            settings.save(machineId, apiKey);
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_SHORT).show();
        }
    }
}
