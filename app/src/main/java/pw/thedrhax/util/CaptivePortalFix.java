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
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import com.topjohnwu.superuser.CallbackList;
import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import pw.thedrhax.mosmetro.authenticator.Gen204;

public class CaptivePortalFix {
    // Android < 7.1.1
    public static final String CAPTIVE_PORTAL_SERVER = "captive_portal_server";

    // Android >= 7.1.1
    public static final String CAPTIVE_PORTAL_HTTP_URL = "captive_portal_http_url";
    public static final String CAPTIVE_PORTAL_HTTPS_URL = "captive_portal_https_url";

    // Generate_204 URLs
    private static final List<String> GEN204_URLS = Arrays.asList(Gen204.URL_RELIABLE);
    private static final List<String> GEN204_DOMAINS = new LinkedList<String>() {{
        for (String server : GEN204_URLS) {
            add(server.split("/")[0]);
        }
    }};

    private static final CallbackList<String> SHELL_LOG = new CallbackList<String>() {
        @Override
        public void onAddElement(String s) {
            Logger.log(Logger.LEVEL.DEBUG, "CaptivePortalFix.Shell | " + s);
        }
    };

    private final Context context;
    private final Randomizer random;

    public CaptivePortalFix(Context context) {
        this.context = context;
        this.random = new Randomizer(context);
    }

    public boolean isApplied() {
        ContentResolver resolver = context.getContentResolver();

        if (Build.VERSION.SDK_INT >= 17) {
            String server = Settings.Global.getString(resolver, CAPTIVE_PORTAL_SERVER);
            if (server == null || !GEN204_DOMAINS.contains(server)) return false;

            try {
                String http_url = Settings.Global.getString(resolver, CAPTIVE_PORTAL_HTTP_URL);
                if (http_url == null || !GEN204_URLS.contains(http_url.substring(7))) return false;

                String https_url = Settings.Global.getString(resolver, CAPTIVE_PORTAL_HTTPS_URL);
                if (https_url == null || !GEN204_URLS.contains(https_url.substring(8))) return false;
            } catch (StringIndexOutOfBoundsException ex) {
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    public interface Callback {
        void onResult(boolean success);
    }

    /** One-time grant: adb shell pm grant pw.thedrhax.mosmetro android.permission.WRITE_SECURE_SETTINGS */
    public boolean hasSecureSettingsAccess() {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void apply(Callback callback) {
        String url = (String) random.choose(Gen204.URL_RELIABLE);
        String domain = url.split("/")[0];

        if (hasSecureSettingsAccess()) {
            ContentResolver resolver = context.getContentResolver();
            callback.onResult(putGlobal(resolver, CAPTIVE_PORTAL_SERVER, domain)
                    && putGlobal(resolver, CAPTIVE_PORTAL_HTTP_URL, "http://" + url)
                    && putGlobal(resolver, CAPTIVE_PORTAL_HTTPS_URL, "https://" + url));
            return;
        }

        Shell.sh(
                "settings put global " + CAPTIVE_PORTAL_SERVER + " " + domain + " && " +
                "settings put global " + CAPTIVE_PORTAL_HTTP_URL + " http://" + url + " && " +
                "settings put global " + CAPTIVE_PORTAL_HTTPS_URL + " https://" + url
        ).to(SHELL_LOG).submit(result -> callback.onResult(result.isSuccess()));
    }

    public void revert(Callback callback) {
        if (hasSecureSettingsAccess()) {
            ContentResolver resolver = context.getContentResolver();
            callback.onResult(putGlobal(resolver, CAPTIVE_PORTAL_SERVER, null)
                    && putGlobal(resolver, CAPTIVE_PORTAL_HTTP_URL, null)
                    && putGlobal(resolver, CAPTIVE_PORTAL_HTTPS_URL, null));
            return;
        }

        Shell.sh(
                "settings delete global " + CAPTIVE_PORTAL_SERVER + " && " +
                "settings delete global " + CAPTIVE_PORTAL_HTTP_URL + " && " +
                "settings delete global " + CAPTIVE_PORTAL_HTTPS_URL
        ).to(SHELL_LOG).submit(result -> callback.onResult(result.isSuccess()));
    }

    private static boolean putGlobal(ContentResolver resolver, String key, String value) {
        try {
            return Settings.Global.putString(resolver, key, value);
        } catch (SecurityException ex) {
            Logger.log(Logger.LEVEL.DEBUG, ex);
            return false;
        }
    }

    public void toggle(Callback result) {
        if (isApplied()) {
            revert(result);
        } else {
            apply(result);
        }
    }
}
