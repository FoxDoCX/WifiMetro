package pw.thedrhax.mosmetro.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.DateFormat;
import java.util.Date;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.DebugActivity;
import pw.thedrhax.util.ConnectionHistory;
import pw.thedrhax.util.Util;

public class StatusWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        RemoteViews views = build(context);
        for (int id : ids) {
            manager.updateAppWidget(id, views);
        }
    }

    public static void updateAll(Context context) {
        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, StatusWidget.class);
        int[] ids = wm.getAppWidgetIds(cn);
        if (ids.length == 0) return;
        new StatusWidget().onUpdate(context, wm, ids);
    }

    private static RemoteViews build(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.status_widget);
        ConnectionHistory.Entry last = new ConnectionHistory(context).last();

        if (last == null) {
            views.setTextViewText(R.id.status_text, context.getString(R.string.widget_status_idle));
        } else {
            String when = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(last.timestamp));
            String line = last.isSuccess()
                    ? context.getString(R.string.widget_status_ok, last.ssid)
                    : context.getString(R.string.widget_status_fail, last.ssid);
            views.setTextViewText(R.id.status_text, line + " · " + when);
        }

        PendingIntent open = PendingIntent.getActivity(context, 10,
                new Intent(context, DebugActivity.class)
                        .putExtra(DebugActivity.INTENT_VIEW_ONLY, true),
                Util.pendingFlags(PendingIntent.FLAG_UPDATE_CURRENT));
        views.setOnClickPendingIntent(R.id.status_title, open);
        views.setOnClickPendingIntent(R.id.status_text, open);
        return views;
    }
}
