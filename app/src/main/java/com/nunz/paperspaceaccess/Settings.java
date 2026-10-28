package com.nunz.paperspaceaccess;

import android.content.Context;
import android.content.SharedPreferences;

// Machine ID and API key, kept in app-private storage
public final class Settings {
    private static final String PREFS = "paperspace";
    private static final String KEY_MACHINE_ID = "machineId";
    private static final String KEY_API_KEY = "apiKey";

    private final SharedPreferences prefs;

    public Settings(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getMachineId() {
        return prefs.getString(KEY_MACHINE_ID, "");
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public boolean isConfigured() {
        return !getMachineId().isEmpty() && !getApiKey().isEmpty();
    }

    public void save(String machineId, String apiKey) {
        prefs.edit()
                .putString(KEY_MACHINE_ID, machineId)
                .putString(KEY_API_KEY, apiKey)
                .apply();
    }
}
