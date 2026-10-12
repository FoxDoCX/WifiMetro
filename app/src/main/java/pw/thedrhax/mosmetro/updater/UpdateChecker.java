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

package pw.thedrhax.mosmetro.updater;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.jayway.jsonpath.DocumentContext;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import pw.thedrhax.mosmetro.BuildConfig;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SafeViewActivity;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Util;
import pw.thedrhax.util.Version;

/**
 * Checks GitHub Releases of {@link BuildConfig#UPDATE_REPO} for a newer signed APK.
 */
public class UpdateChecker {
    public static final String PREF_ENABLED = "pref_updater_enabled";
    private static final String PREF_IGNORE = "pref_updater_ignore_version";

    private final Context context;
    private final DownloadManager dm;
    private final SharedPreferences settings;
    private long last_download = 0;
    private boolean receiver_registered = false;

    public UpdateChecker(Context context) {
        this.context = context;
        this.dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private final BroadcastReceiver onComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != last_download) return;

            Uri uri = dm.getUriForDownloadedFile(id);
            if (uri == null) return;

            Intent install = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                context.startActivity(install);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(context, context.getString(R.string.error,
                        context.getString(R.string.update_error)
                ), Toast.LENGTH_LONG).show();
            }
        }
    };

    public void init() {
        Util.registerReceiver(context, onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), true);
        receiver_registered = true;
    }

    public void deinit() {
        if (!receiver_registered) return;
        receiver_registered = false;
        try {
            context.unregisterReceiver(onComplete);
        } catch (IllegalArgumentException ignored) {}
    }

    /** Blocking network call. */
    @Nullable
    public Release check() {
        String url = "https://api.github.com/repos/" + BuildConfig.UPDATE_REPO + "/releases/latest";
        DocumentContext data;
        try {
            data = new OkHttp(context).get(url).execute().jsonpath();
        } catch (IOException | RuntimeException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            return null;
        }
        if (data == Util.JSONPATH_EMPTY) return null;

        try {
            String tag = data.read("$.tag_name");
            String page = data.read("$.html_url");
            String notes = data.read("$.body");
            List<Map<String, Object>> assets = data.read("$.assets");
            if (tag == null || assets == null) return null;

            Map<String, Object> asset = pickAsset(assets);
            return new Release(
                    tag.replaceFirst("^[vV]", ""),
                    notes != null ? notes : "",
                    page,
                    asset != null ? (String) asset.get("browser_download_url") : null,
                    asset != null ? (String) asset.get("name") : null
            );
        } catch (ClassCastException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            return null;
        }
    }

    /** Prefer a signed release APK for the primary ABI of this device. */
    @Nullable
    private static Map<String, Object> pickAsset(List<Map<String, Object>> assets) {
        String abi = Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "";
        Map<String, Object> fallback = null;
        for (Map<String, Object> asset : assets) {
            String name = String.valueOf(asset.get("name")).toLowerCase(Locale.ROOT);
            if (!name.endsWith(".apk")) continue;
            if (name.contains(abi.toLowerCase(Locale.ROOT))) {
                if (!name.contains("debug")) return asset;
                if (fallback == null) fallback = asset;
            }
        }
        return fallback;
    }

    public void async_check(@NonNull Callback callback) {
        Handler main = new Handler(Looper.getMainLooper());
        callback.onStart();
        Executors.newSingleThreadExecutor().execute(() -> {
            Release result = check();
            main.post(() -> callback.onResult(result));
        });
    }

    public interface Callback {
        void onStart();
        void onResult(@Nullable Release result);
    }

    /** Compares dotted numeric versions: "2.10.0" > "2.9.1". */
    static int compareVersions(String a, String b) {
        String[] pa = a.split("[^0-9]+");
        String[] pb = b.split("[^0-9]+");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            int va = i < pa.length && !pa[i].isEmpty() ? Integer.parseInt(pa[i]) : 0;
            int vb = i < pb.length && !pb[i].isEmpty() ? Integer.parseInt(pb[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    public class Release {
        public final String version;
        public final String notes;
        public final String page;
        @Nullable public final String apk_url;
        @Nullable public final String filename;

        Release(String version, String notes, String page,
                @Nullable String apk_url, @Nullable String filename) {
            this.version = version;
            this.notes = notes;
            this.page = page;
            this.apk_url = apk_url;
            this.filename = filename;
        }

        public boolean isNewer() {
            return compareVersions(version, Version.getVersionName()) > 0;
        }

        public boolean isIgnored() {
            return version.equals(settings.getString(PREF_IGNORE, ""));
        }

        public void ignore() {
            settings.edit().putString(PREF_IGNORE, version).apply();
        }

        public void install() {
            if (apk_url == null) {
                openPage();
                return;
            }

            File apk = new File(context.getExternalFilesDir(null), "update.apk");
            if (apk.exists()) {
                apk.delete();
            }

            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(apk_url))
                    .setTitle(filename)
                    .setMimeType("application/vnd.android.package-archive")
                    .setDestinationUri(Uri.fromFile(apk))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            last_download = dm.enqueue(req);
            Toast.makeText(context, R.string.update_downloading, Toast.LENGTH_SHORT).show();
        }

        public void openPage() {
            context.startActivity(new Intent(context, SafeViewActivity.class).putExtra("data", page));
        }

        public void showDialog() {
            AlertDialog.Builder dialog;

            if (isNewer()) {
                String message = context.getString(R.string.update_message, version, Version.getVersionName());
                if (!notes.isEmpty()) {
                    message += "\n\n" + notes.replaceAll("[#*`>|]", "").trim();
                }
                dialog = new AlertDialog.Builder(context)
                        .setTitle(R.string.update_available)
                        .setMessage(message)
                        .setPositiveButton(R.string.install, (d, w) -> install())
                        .setNeutralButton(R.string.update_open_page, (d, w) -> openPage())
                        .setNegativeButton(R.string.ignore_short, (d, w) -> ignore());
            } else {
                dialog = new AlertDialog.Builder(context)
                        .setTitle(R.string.update_not_available)
                        .setMessage(context.getString(R.string.update_not_available_message))
                        .setNegativeButton(R.string.ok, null);
            }

            try {
                dialog.show();
            } catch (RuntimeException ignored) {}
        }
    }
}
