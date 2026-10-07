/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.thedrhax.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

public class Notify extends NotificationCompat.Builder {
    public static final String CHANNEL_ID = "MosMetroWIFIServiceChannel";
    private static final String CHANNEL_NAME = "WifiMetro Service";

    private final Context context;
    private final NotificationManager nm;
    private final SharedPreferences settings;

    private int id = 2000;
    private boolean enabled = true;
    private boolean locked = false;
    private boolean big_text = true;
    private boolean foreground_denied = false;

    public Notify(Context context) {
        super(context, CHANNEL_ID);
        this.context = context;
        this.nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);

        if (nm != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("WifiMetro foreground service");
            channel.setShowBadge(false);
            nm.createNotificationChannel(channel);
        }

        priority(Util.getIntPreference(context, "pref_notify_priority", 0));
    }

    public Notify title(String title) {
        setContentTitle(title); return this;
    }

    public Notify text(String text) {
        setContentText(text);
        if (big_text) style(new NotificationCompat.BigTextStyle().bigText(text));
        return this;
    }

    public Notify onClick(PendingIntent intent) {
        setContentIntent(intent); return this;
    }

    public Notify onDelete(PendingIntent intent) {
        setDeleteIntent(intent); return this;
    }

    public Notify id(int id) {
        this.id = id; return this;
    }

    public Notify priority(int priority) {
        if (priority < -2) priority = -2;
        if (priority > 2) priority = 2;
        setPriority(priority); return this;
    }

    public Notify icon(int colored, int white) {
        boolean pref_colored = (Build.VERSION.SDK_INT <= 26) ^
                settings.getBoolean("pref_notify_alternative", false);

        setSmallIcon(pref_colored ? colored : white); return this;
    }

    public Notify style(NotificationCompat.Style style) {
        big_text = style instanceof NotificationCompat.BigTextStyle;
        setStyle(style); return this;
    }

    public Notify progress(int progress, boolean indeterminate) {
        setProgress(100, progress, indeterminate); return this;
    }

    public Notify progress(int progress) {
        return progress(progress, false);
    }

    public Notify hideProgress() {
        setProgress(0, 0, false); return this;
    }

    public Notify addAction(CharSequence title, PendingIntent intent) {
        super.addAction(0, title, intent); return this;
    }

    public Notify enabled(boolean enabled) {
        this.enabled = enabled; return this;
    }

    public Notify locked(boolean locked) {
        this.locked = locked; return this;
    }

    public Notify cancelOnClick(boolean cancel) {
        setAutoCancel(cancel); return this;
    }

    public Notify show() {
        if (!enabled) return this;

        Notification notification = build();

        if (locked && context instanceof Service) {
            try {
                ServiceCompat.startForeground(
                        (Service) context,
                        id,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                );
                foreground_denied = false;
                return this;
            } catch (IllegalStateException | SecurityException ex) {
                // Android 14+: missing FGS type permission; Android 15+: dataSync from
                // BOOT_COMPLETED or exhausted 6h dataSync quota.
                foreground_denied = true;
                Logger.log(Logger.LEVEL.DEBUG, ex);
            }
        }

        if (locked) {
            notification.flags |= Notification.FLAG_NO_CLEAR;
        }

        if (nm != null) {
            nm.notify(id, notification);
        }
        return this;
    }

    public boolean isForegroundDenied() {
        return foreground_denied;
    }

    public Notify hide() {
        if (locked && context instanceof Service) {
            ServiceCompat.stopForeground(
                    (Service) context, ServiceCompat.STOP_FOREGROUND_REMOVE);
        } else if (nm != null) {
            nm.cancel(id);
        }
        return this;
    }
}
