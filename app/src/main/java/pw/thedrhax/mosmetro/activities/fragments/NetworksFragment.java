/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 */

package pw.thedrhax.mosmetro.activities.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import androidx.annotation.Nullable;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.DebugActivity;
import pw.thedrhax.mosmetro.authenticator.Provider;
import pw.thedrhax.mosmetro.services.ConnectionService;
import pw.thedrhax.util.PermissionUtils;
import pw.thedrhax.util.Util;
import pw.thedrhax.util.WifiUtils;

/**
 * Manual pick of supported SSIDs — starts forced connection + log screen.
 */
public class NetworksFragment extends NestedFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.pref_networks));
        rebuild();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getPreferenceScreen() != null) {
            rebuild();
        }
    }

    private void rebuild() {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(screen);

        WifiUtils wifi = new WifiUtils(getActivity());
        PermissionUtils pu = new PermissionUtils(getActivity());
        String current = wifi.getSSID();
        boolean known = Provider.isSSIDSupported(current);
        boolean canRead = pu.canReadWifiSsid();

        Preference currentPref = new Preference(getActivity());
        currentPref.setKey("pref_networks_current");
        currentPref.setTitle(R.string.pref_networks_current);
        if (!canRead || wifi.isSsidUnknown()) {
            currentPref.setSummary(R.string.pref_networks_current_no_permission);
        } else if (known) {
            currentPref.setSummary(getString(R.string.pref_networks_current_summary, current));
        } else {
            currentPref.setSummary(getString(R.string.pref_networks_current_unknown, current));
        }
        currentPref.setSelectable(false);
        screen.addPreference(currentPref);

        if (!canRead) {
            Preference fix = new Preference(getActivity());
            fix.setKey("pref_networks_fix_perm");
            fix.setTitle(R.string.pref_networks_fix_permission);
            fix.setSummary(R.string.pref_networks_fix_permission_summary);
            fix.setOnPreferenceClickListener(p -> {
                pu.openAppSettings();
                return true;
            });
            screen.addPreference(fix);
        }

        Preference auto = new Preference(getActivity());
        auto.setKey("pref_networks_auto");
        auto.setTitle(R.string.pref_networks_connect_auto);
        auto.setSummary(R.string.pref_networks_connect_auto_summary);
        auto.setOnPreferenceClickListener(p -> {
            startManual(null);
            return true;
        });
        screen.addPreference(auto);

        Set<String> seen = new LinkedHashSet<>();
        LinkedHashSet<String> transport = new LinkedHashSet<>();
        LinkedHashSet<String> city = new LinkedHashSet<>();
        LinkedHashSet<String> mts = new LinkedHashSet<>();
        LinkedHashSet<String> other = new LinkedHashSet<>();

        for (String ssid : Provider.SSIDs) {
            String key = ssid.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) continue;

            String u = ssid.toUpperCase(Locale.ROOT);
            if (u.startsWith("MTS")) {
                mts.add(ssid);
            } else if (u.contains("MOSCOW") || u.equals("WI-FI_FREE") || u.equals("WIFI_FREE")) {
                city.add(ssid);
            } else if (u.contains("METRO") || u.startsWith("MT") || u.contains("AURA")
                    || u.contains("GORTRANS") || u.contains("CPPK") || u.contains("AIR")
                    || u.contains("AEROEXPRESS")) {
                transport.add(ssid);
            } else if (u.contains("BELTELECOM") || u.contains("BYFLY")
                    || u.contains("MAINET") || u.contains("BMSTU")) {
                other.add(ssid);
            } else {
                city.add(ssid);
            }
        }

        addCategory(screen, R.string.pref_networks_cat_transport, transport, current, false);
        addCategory(screen, R.string.pref_networks_cat_city, city, current, true);
        addCategory(screen, R.string.pref_networks_cat_mts, mts, current, true);
        addCategory(screen, R.string.pref_networks_cat_other, other, current, false);
    }

    private void addCategory(PreferenceScreen screen, int titleRes,
                             LinkedHashSet<String> ssids, String current, boolean limited) {
        if (ssids.isEmpty()) return;
        PreferenceCategory cat = new PreferenceCategory(getActivity());
        cat.setKey("cat_" + titleRes);
        cat.setTitle(titleRes);
        screen.addPreference(cat);

        for (String ssid : ssids) {
            Preference pref = new Preference(getActivity());
            pref.setKey("ssid_" + ssid.replace(' ', '_').replace('-', '_'));
            pref.setTitle(ssid);
            boolean match = current != null && ssid.equalsIgnoreCase(current);
            if (match) {
                pref.setSummary(R.string.pref_networks_item_connected);
            } else if (limited) {
                pref.setSummary(R.string.pref_networks_item_limited);
            } else {
                pref.setSummary(R.string.pref_networks_item_summary);
            }
            final String target = ssid;
            pref.setOnPreferenceClickListener(p -> {
                startManual(target);
                return true;
            });
            cat.addPreference(pref);
        }
    }

    private void startManual(@Nullable String ssid) {
        final Intent service = new Intent(getActivity(), ConnectionService.class)
                .putExtra(ConnectionService.EXTRA_FORCE, true)
                .putExtra(ConnectionService.EXTRA_DEBUG, true);
        if (ssid != null) {
            service.putExtra(ConnectionService.EXTRA_SSID, ssid);
        }

        final Intent debug = new Intent(getActivity(), DebugActivity.class);
        if (ssid != null) {
            debug.putExtra(ConnectionService.EXTRA_SSID, ssid);
        }

        Util.startService(getActivity(), service);
        startActivity(debug);
    }
}
