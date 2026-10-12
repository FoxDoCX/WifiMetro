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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.service.quicksettings.TileService;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import pw.thedrhax.mosmetro.widgets.StatusWidget;
import pw.thedrhax.mosmetro.widgets.ToggleWidget;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Util;

/**
 * Single entry point for enabling / disabling automatic login.
 *
 * Default mode: a {@link PendingIntent}-based network callback held by the system
 * ({@link WifiNetworkReceiver}), no persistent process required.
 * Legacy mode ("persistent service"): {@link ReceiverService} keeps a dynamic receiver alive.
 */
public final class AutoConnect {
    public static final String PREF_ENABLED = "pref_autoconnect";
    public static final String PREF_PERSISTENT = "pref_autoconnect_service";

    private AutoConnect() {}

    public static boolean isEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(PREF_ENABLED, true);
    }

    public static boolean isPersistent(@NonNull Context context) {
        return prefs(context).getBoolean(PREF_PERSISTENT, false);
    }

    public static void setEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit().putBoolean(PREF_ENABLED, enabled).apply();
        apply(context, enabled, isPersistent(context));
    }

    /**
     * Restore state after app start, boot or package update. Registration fires the callback
     * for the current network, so it is skipped while the system still holds our PendingIntent.
     */
    public static void ensure(@NonNull Context context) {
        Context app = context.getApplicationContext();
        boolean enabled = isEnabled(app);

        if (enabled && callbackIntent(app, PendingIntent.FLAG_NO_CREATE) == null) {
            registerCallback(app);
        }

        if (enabled && isPersistent(app)) {
            Util.startService(app, new Intent(app, ReceiverService.class));
        }
    }

    /** Apply explicit values (preference listeners run before the value is persisted). */
    public static void apply(@NonNull Context context, boolean enabled, boolean persistent) {
        Context app = context.getApplicationContext();

        if (enabled) {
            registerCallback(app);
        } else {
            unregisterCallback(app);
        }

        Intent receiver_service = new Intent(app, ReceiverService.class);
        if (enabled && persistent) {
            Util.startService(app, receiver_service);
        } else {
            app.stopService(receiver_service);
        }

        if (!enabled && ConnectionService.isRunning()) {
            Util.startService(app, new Intent(app, ConnectionService.class)
                    .setAction(ConnectionService.ACTION_STOP));
        }

        refreshUi(app);
    }

    public static void refreshUi(@NonNull Context context) {
        ToggleWidget.updateAll(context);
        StatusWidget.updateAll(context);
        try {
            TileService.requestListeningState(context,
                    new ComponentName(context, AutoConnectTileService.class));
        } catch (RuntimeException ignored) {}
    }

    private static PendingIntent callbackIntent(Context context, int flags) {
        Intent intent = new Intent(context, WifiNetworkReceiver.class)
                .setAction(WifiNetworkReceiver.ACTION_NETWORK_AVAILABLE);
        // The system fills in EXTRA_NETWORK, so the PendingIntent has to stay mutable
        if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
        return PendingIntent.getBroadcast(context, 0, intent, flags);
    }

    private static PendingIntent callbackIntent(Context context) {
        return callbackIntent(context, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void registerCallback(Context context) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build();

        PendingIntent pi = callbackIntent(context);
        try {
            // Re-registering an equal PendingIntent replaces the previous request
            cm.registerNetworkCallback(request, pi);
        } catch (RuntimeException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
        }
    }

    private static void unregisterCallback(Context context) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        if (cm == null) return;
        PendingIntent pi = callbackIntent(context, PendingIntent.FLAG_NO_CREATE);
        if (pi == null) return;
        try {
            cm.unregisterNetworkCallback(pi);
        } catch (RuntimeException ignored) {}
        pi.cancel();
    }

    private static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
