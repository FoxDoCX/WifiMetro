/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 */

package pw.thedrhax.mosmetro.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.services.AutoConnect;
import pw.thedrhax.util.Util;

public class ToggleWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent intent = new Intent(context, ToggleWidget.class);
        intent.putExtra("toggle", "pref_autoconnect");
        PendingIntent pIntent = PendingIntent.getBroadcast(
                context, 0, intent, Util.pendingFlags(PendingIntent.FLAG_UPDATE_CURRENT));

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.toggle_widget);
        views.setOnClickPendingIntent(R.id.imageButton, pIntent);
        views.setImageViewResource(R.id.imageButton, AutoConnect.isEnabled(context)
                ? R.drawable.ic_widget_toggle_on
                : R.drawable.ic_widget_toggle_off);

        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public static void updateAll(Context context) {
        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        ComponentName cm = new ComponentName(context, ToggleWidget.class);
        new ToggleWidget().onUpdate(context, wm, wm.getAppWidgetIds(cm));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Bundle bundle = intent.getExtras();
        if (bundle != null && "pref_autoconnect".equals(bundle.getString("toggle"))) {
            AutoConnect.setEnabled(context, !AutoConnect.isEnabled(context));
        } else {
            updateAll(context);
        }
    }
}
