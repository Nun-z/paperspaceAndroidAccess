package com.nunz.paperspaceaccess;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
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
}
