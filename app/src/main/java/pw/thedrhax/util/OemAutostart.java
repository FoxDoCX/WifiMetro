package pw.thedrhax.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;

import pw.thedrhax.mosmetro.R;

/** Opens vendor-specific autostart / battery screens (Xiaomi HyperOS, etc.). */
public final class OemAutostart {
    private OemAutostart() {}

    public static void open(@NonNull Context context) {
        String[][] candidates = {
                {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
                {"com.miui.securitycenter", "com.miui.powercenter.PowerSettings"},
                {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
                {"com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"},
                {"com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"},
                {"com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"},
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
                {"com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"},
        };

        PackageManager pm = context.getPackageManager();
        for (String[] c : candidates) {
            Intent intent = new Intent().setComponent(new ComponentName(c[0], c[1]));
            if (intent.resolveActivity(pm) != null) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return;
                } catch (RuntimeException ignored) {}
            }
        }

        try {
            Intent app = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.parse("package:" + context.getPackageName()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(app);
            if (Build.MANUFACTURER != null
                    && Build.MANUFACTURER.toLowerCase(java.util.Locale.ROOT).contains("xiaomi")) {
                Toast.makeText(context, R.string.pref_oem_autostart_summary, Toast.LENGTH_LONG).show();
            }
        } catch (RuntimeException ex) {
            Toast.makeText(context, R.string.toast_unsupported_function, Toast.LENGTH_LONG).show();
        }
    }
}
