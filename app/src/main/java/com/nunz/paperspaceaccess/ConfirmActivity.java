package com.nunz.paperspaceaccess;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

// Asks before a widget tap shuts down or restarts the machine
public class ConfirmActivity extends AppCompatActivity {
    static final String EXTRA_ACTION = "action";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getStringExtra(EXTRA_ACTION);
        if (!"stop".equals(action) && !"restart".equals(action)) {
            finish();
            return;
        }
        boolean stop = action.equals("stop");

        new MaterialAlertDialogBuilder(this)
                .setTitle(stop ? R.string.confirm_stop : R.string.confirm_restart)
                .setPositiveButton(stop ? R.string.shutdown_machine : R.string.restart_machine,
                        (dialog, which) -> WidgetWorker.send(this, action))
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(dialog -> finish())
                .show();
    }
}
