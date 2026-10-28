package com.nunz.paperspaceaccess;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

// Home-screen widget with Start, Shutdown and Restart buttons and the machine status
public class PaperspaceWidget extends AppWidgetProvider {
    private static final String ACTION_START = "com.nunz.paperspaceaccess.widget.START";
    private static final String ACTION_REFRESH = "com.nunz.paperspaceaccess.widget.REFRESH";
    private static final int FLAGS = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

    private static final String PREFS = "widget";
    private static final String KEY_STATUS = "status";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        // Draw the last known status right away, then check the real one in the background
        String status = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_STATUS, context.getString(R.string.machine_status,
                        context.getString(R.string.getting_status)));
        manager.updateAppWidget(appWidgetIds, buildViews(context, status));
        WidgetWorker.refresh(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_START.equals(intent.getAction())) {
            WidgetWorker.send(context, "start");
        } else if (ACTION_REFRESH.equals(intent.getAction())) {
            WidgetWorker.refresh(context);
        }
    }

    static boolean hasWidgets(Context context) {
        return AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, PaperspaceWidget.class)).length > 0;
    }

    // Redraw every widget with the given status line, and remember it for later redraws
    static void update(Context context, String status) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_STATUS, status).apply();
        AppWidgetManager.getInstance(context).updateAppWidget(
                new ComponentName(context, PaperspaceWidget.class), buildViews(context, status));
    }

    private static RemoteViews buildViews(Context context, String status) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_paperspace);

        if (new Settings(context).isConfigured()) {
            views.setTextViewText(R.id.widgetStatus, status);
            views.setOnClickPendingIntent(R.id.widgetStart, broadcast(context, ACTION_START, 1));
            views.setOnClickPendingIntent(R.id.widgetStop, confirm(context, "stop", 2));
            views.setOnClickPendingIntent(R.id.widgetRestart, confirm(context, "restart", 3));
            views.setOnClickPendingIntent(R.id.widgetStatus, broadcast(context, ACTION_REFRESH, 4));
        } else {
            // Every tap goes to the settings screen until the machine ID and API key are saved
            views.setTextViewText(R.id.widgetStatus, context.getString(R.string.machine_status,
                    context.getString(R.string.not_configured)));
            PendingIntent settings = PendingIntent.getActivity(context, 5,
                    new Intent(context, SettingsActivity.class), FLAGS);
            views.setOnClickPendingIntent(R.id.widgetStart, settings);
            views.setOnClickPendingIntent(R.id.widgetStop, settings);
            views.setOnClickPendingIntent(R.id.widgetRestart, settings);
            views.setOnClickPendingIntent(R.id.widgetStatus, settings);
        }
        return views;
    }

    private static PendingIntent broadcast(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, PaperspaceWidget.class).setAction(action);
        return PendingIntent.getBroadcast(context, requestCode, intent, FLAGS);
    }

    private static PendingIntent confirm(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, ConfirmActivity.class)
                .putExtra(ConfirmActivity.EXTRA_ACTION, action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, requestCode, intent, FLAGS);
    }
}
