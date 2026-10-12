/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package pw.thedrhax.mosmetro.services;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.ConnectionServiceActivity;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Notify;
import pw.thedrhax.util.Util;
import pw.thedrhax.util.WifiUtils;

/**
 * Receives the system network callback ({@link AutoConnect}) when a Wi-Fi network
 * becomes available and starts {@link ConnectionService}.
 */
public class WifiNetworkReceiver extends BroadcastReceiver implements Logger.Metadata {
    public static final String ACTION_NETWORK_AVAILABLE =
            "pw.thedrhax.mosmetro.action.WIFI_AVAILABLE";
    private static final int NOTIFY_ID = 4;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_NETWORK_AVAILABLE.equals(intent.getAction())) return;
        if (!AutoConnect.isEnabled(context)) return;
        if (ConnectionService.isRunning()) return;

        if (!new WifiUtils(context).isEnabled()) return;

        Logger.log(this, "Wi-Fi network available");

        Intent service = new Intent(context, ConnectionService.class);
        if (!Util.startService(context, service)) {
            // Android 12+ refuses background FGS starts unless battery optimization is
            // disabled. Fall back to a notification: a tap is a user interaction.
            Logger.log(this, "Background start denied, asking user to tap");
            new Notify(context)
                    .channel(Notify.CHANNEL_RESULT)
                    .id(NOTIFY_ID)
                    .icon(R.drawable.ic_notification_connecting_colored,
                            R.drawable.ic_notification_connecting)
                    .title(context.getString(R.string.notification_tap_to_connect))
                    .text(context.getString(R.string.notification_tap_to_connect_summary))
                    .cancelOnClick(true)
                    .onClick(PendingIntent.getActivity(context, NOTIFY_ID,
                            new Intent(context, ConnectionServiceActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            Util.pendingFlags(PendingIntent.FLAG_UPDATE_CURRENT)))
                    .show();
        }
    }

    @Override
    public String tag() {
        return "callback";
    }
}
