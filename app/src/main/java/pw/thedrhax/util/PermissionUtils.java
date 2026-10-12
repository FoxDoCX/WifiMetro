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

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.R;

public final class PermissionUtils {
    private final Context context;
    private final PowerManager pm;

    public PermissionUtils(@NonNull Context context) {
        this.context = context;
        this.pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (Build.VERSION.SDK_INT >= 23) {
            REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = new Intent()
                    .setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:" + context.getPackageName()));

            IGNORE_BATTERY_OPTIMIZATION_SETTINGS = new Intent()
                    .setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        }
    }

    public void openAppSettings() {
        try {
            context.startActivity(new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.getPackageName(), null)
            ));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, R.string.toast_unsupported_function, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Location permissions
     */

    public boolean isCoarseLocationGranted() {
        int p = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        return p == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isFineLocationGranted() {
        int p = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        return p == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(29)
    public boolean isBackgroundLocationGranted() {
        int p = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        return p == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(23)
    public void requestCoarseLocation(Activity activity, int requestCode) {
        activity.requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
    }

    @RequiresApi(26)
    public void requestFineLocation(Activity activity, int requestCode) {
        // Android 12+ ignores a FINE-only request; COARSE must be requested together.
        activity.requestPermissions(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, requestCode);
    }
    @RequiresApi(29)
    public void requestBackgroundLocation(Activity activity, int requestCode) {
        activity.requestPermissions(new String[] {Manifest.permission.ACCESS_BACKGROUND_LOCATION}, requestCode);
    }

    public boolean isNotificationsGranted() {
        if (Build.VERSION.SDK_INT < 33) return true;
        int p = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS);
        return p == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(33)
    public void requestNotifications(Activity activity, int requestCode) {
        activity.requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, requestCode);
    }

    /**
     * Android 13+: NEARBY_WIFI_DEVICES is enough to read SSID without location
     * when declared with neverForLocation.
     */
    public boolean isNearbyWifiGranted() {
        if (Build.VERSION.SDK_INT < 33) return true;
        int p = ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES);
        return p == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(33)
    public void requestNearbyWifi(Activity activity, int requestCode) {
        activity.requestPermissions(new String[] {Manifest.permission.NEARBY_WIFI_DEVICES}, requestCode);
    }

    /** True if the app can read the current Wi‑Fi SSID on this Android version. */
    public boolean canReadWifiSsid() {
        if (Build.VERSION.SDK_INT >= 33) {
            return isNearbyWifiGranted() || isFineLocationGranted() || isCoarseLocationGranted();
        }
        if (Build.VERSION.SDK_INT >= 26) {
            return isFineLocationGranted() || isCoarseLocationGranted();
        }
        return true;
    }

    public boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT < 23) return true;
        return Settings.canDrawOverlays(context);
    }

    @RequiresApi(23)
    public void requestOverlayPermission() {
        try {
            context.startActivity(new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName())
            ));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, R.string.toast_unsupported_function, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Battery saving permissions.
     */

    private Intent REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
    private Intent IGNORE_BATTERY_OPTIMIZATION_SETTINGS;

    @RequiresApi(23)
    public boolean isBatterySavingIgnored() {
        return pm.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID);
    }

    @RequiresApi(23)
    public void requestBatterySavingIgnore() {
        try {
            context.startActivity(REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        } catch (ActivityNotFoundException ex) {
            batterySavingException();
        }
    }

    @RequiresApi(23)
    public void openBatterySavingSettings() {
        try {
            context.startActivity(IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        } catch (ActivityNotFoundException ex) {
            batterySavingException();
        }
    }

    private void batterySavingException() {
        Toast.makeText(context, R.string.toast_unsupported_function, Toast.LENGTH_LONG).show();
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("pref_battery_saving_ignore", true)
                .apply();
    }
}
