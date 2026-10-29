package com.nunz.paperspaceaccess;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;

// Sends widget commands and keeps the widget's status line current, in the background
public class WidgetWorker extends Worker {
    private static final String WORK_NAME = "paperspaceWidget";
    private static final String KEY_ACTION = "action";
    private static final String KEY_SEND = "send";
    private static final long POLL_INTERVAL_MS = 5000;
    private static final long POLL_TIMEOUT_MS = 3 * 60 * 1000;

    // Send a command, then follow the machine until it gets where the command leads
    public static void send(Context context, String action) {
        enqueue(context, action, true, ExistingWorkPolicy.REPLACE);
    }

    // Follow a command the app has already sent
    public static void follow(Context context, String action) {
        enqueue(context, action, false, ExistingWorkPolicy.REPLACE);
    }

    // Check the status once; a command being followed already does that, so leave it running
    public static void refresh(Context context) {
        enqueue(context, null, false, ExistingWorkPolicy.KEEP);
    }

    private static void enqueue(Context context, String action, boolean send, ExistingWorkPolicy policy) {
        if (!PaperspaceWidget.hasWidgets(context)) {
            return;
        }
        Data.Builder data = new Data.Builder().putBoolean(KEY_SEND, send);
        if (action != null) {
            data.putString(KEY_ACTION, action);
        }
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WidgetWorker.class)
                .setInputData(data.build())
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request);
    }

    public WidgetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Settings settings = new Settings(context);
        if (!settings.isConfigured()) {
            // Redraws the widget so its buttons lead to the settings screen
            PaperspaceWidget.update(context, null);
            return Result.success();
        }

        PaperspaceClient client = new PaperspaceClient();
        String action = getInputData().getString(KEY_ACTION);

        if (action != null && getInputData().getBoolean(KEY_SEND, false)) {
            // The widget's status may be old, so check the machine's state now before sending
            String state = refreshStatus(client, settings);
            int skipReason = PaperspaceClient.skipReason(action, state);
            if (skipReason != 0) {
                PaperspaceWidget.update(context, context.getString(skipReason));
                return Result.success();
            }

            PaperspaceWidget.update(context, context.getString(R.string.sending_command));
            try {
                client.sendBlocking(settings.getMachineId(), settings.getApiKey(), action);
            } catch (IOException e) {
                String reason = e instanceof PaperspaceClient.HttpException
                        ? e.getMessage() : context.getString(R.string.unreachable);
                PaperspaceWidget.update(context, context.getString(R.string.command_failed, reason));
                return Result.success();
            }
        }

        if (action == null) {
            refreshStatus(client, settings);
            return Result.success();
        }

        // Keep checking until the machine is off after a shutdown, or ready after a start or restart
        String target = action.equals("stop") ? "off" : "ready";
        boolean leftTarget = !action.equals("restart"); // a restart begins and ends at "ready"
        long deadline = SystemClock.elapsedRealtime() + POLL_TIMEOUT_MS;
        while (!isStopped() && SystemClock.elapsedRealtime() < deadline) {
            String state = refreshStatus(client, settings);
            if (!target.equals(state)) {
                leftTarget = true;
            } else if (leftTarget) {
                break;
            }
            SystemClock.sleep(POLL_INTERVAL_MS);
        }
        return Result.success();
    }

    // Show the machine's state on the widget; returns it, or null if it couldn't be read
    private String refreshStatus(PaperspaceClient client, Settings settings) {
        Context context = getApplicationContext();
        String state = null;
        String shown;
        try {
            state = client.getStateBlocking(settings.getMachineId(), settings.getApiKey());
            shown = state;
        } catch (PaperspaceClient.HttpException e) {
            shown = context.getString(R.string.status_error, e.code);
        } catch (IOException e) {
            shown = context.getString(R.string.unreachable);
        }
        PaperspaceWidget.update(context, context.getString(R.string.machine_status, shown));
        return state;
    }
}
