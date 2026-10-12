package pw.thedrhax.mosmetro.activities.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import java.util.HashSet;
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

public class NetworksFragment extends NestedFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(screen);
        rebuild();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.pref_networks));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getPreferenceScreen() != null) rebuild();
    }

    private void rebuild() {
        PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        WifiUtils wifi = new WifiUtils(requireContext());
        PermissionUtils pu = new PermissionUtils(requireContext());
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String current = wifi.getSSID();
        boolean known = Provider.isSSIDSupported(requireContext(), current);
        boolean canRead = pu.canReadWifiSsid();

        Preference currentPref = new Preference(requireContext());
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
            Preference fix = new Preference(requireContext());
            fix.setTitle(R.string.pref_networks_fix_permission);
            fix.setSummary(R.string.pref_networks_fix_permission_summary);
            fix.setOnPreferenceClickListener(p -> {
                pu.openAppSettings();
                return true;
            });
            screen.addPreference(fix);
        }

        EditTextPreference custom = new EditTextPreference(requireContext());
        custom.setKey(Provider.PREF_SSID_CUSTOM);
        custom.setTitle(R.string.pref_ssid_custom);
        custom.setSummary(R.string.pref_ssid_custom_summary);
        custom.setDialogTitle(R.string.pref_ssid_custom);
        custom.setOnBindEditTextListener(edit -> edit.setMinLines(3));
        screen.addPreference(custom);

        Preference auto = new Preference(requireContext());
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
            if (u.startsWith("MTS")) mts.add(ssid);
            else if (u.contains("MOSCOW") || u.equals("WI-FI_FREE") || u.equals("WIFI_FREE")) city.add(ssid);
            else if (u.contains("METRO") || u.startsWith("MT") || u.contains("AURA")
                    || u.contains("GORTRANS") || u.contains("CPPK") || u.contains("AIR")
                    || u.contains("AEROEXPRESS")) transport.add(ssid);
            else if (u.contains("BELTELECOM") || u.contains("BYFLY")
                    || u.contains("MAINET") || u.contains("BMSTU")) other.add(ssid);
            else city.add(ssid);
        }
        for (String ssid : Provider.getCustomSSIDs(requireContext())) {
            if (seen.add(ssid.toLowerCase(Locale.ROOT))) other.add(ssid);
        }

        Set<String> disabled = settings.getStringSet(Provider.PREF_SSID_DISABLED, new HashSet<>());
        addCategory(screen, R.string.pref_networks_cat_transport, transport, current, false, disabled);
        addCategory(screen, R.string.pref_networks_cat_city, city, current, true, disabled);
        addCategory(screen, R.string.pref_networks_cat_mts, mts, current, true, disabled);
        addCategory(screen, R.string.pref_networks_cat_other, other, current, false, disabled);
    }

    private void addCategory(PreferenceScreen screen, int titleRes,
                             LinkedHashSet<String> ssids, String current, boolean limited,
                             Set<String> disabled) {
        if (ssids.isEmpty()) return;
        PreferenceCategory cat = new PreferenceCategory(requireContext());
        cat.setTitle(titleRes);
        screen.addPreference(cat);

        for (String ssid : ssids) {
            Preference pref = new Preference(requireContext());
            pref.setTitle(ssid);
            boolean match = current != null && ssid.equalsIgnoreCase(current);
            boolean off = isDisabled(disabled, ssid);
            if (off) pref.setSummary(R.string.pref_ssid_disabled_summary);
            else if (match) pref.setSummary(R.string.pref_networks_item_connected);
            else if (limited) pref.setSummary(R.string.pref_networks_item_limited);
            else pref.setSummary(R.string.pref_networks_item_summary);

            final String target = ssid;
            pref.setOnPreferenceClickListener(p -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(target)
                        .setItems(new CharSequence[]{
                                getString(R.string.connect),
                                off ? getString(R.string.pref_ssid_enable)
                                        : getString(R.string.pref_ssid_disable)
                        }, (d, which) -> {
                            if (which == 0) startManual(target);
                            else toggleDisabled(target);
                        })
                        .show();
                return true;
            });
            cat.addPreference(pref);
        }
    }

    private static boolean isDisabled(Set<String> disabled, String ssid) {
        for (String d : disabled) {
            if (d.equalsIgnoreCase(ssid)) return true;
        }
        return false;
    }

    private void toggleDisabled(String ssid) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Set<String> next = new HashSet<>(settings.getStringSet(Provider.PREF_SSID_DISABLED, new HashSet<>()));
        String match = null;
        for (String d : next) {
            if (d.equalsIgnoreCase(ssid)) {
                match = d;
                break;
            }
        }
        if (match != null) next.remove(match);
        else next.add(ssid);
        settings.edit().putStringSet(Provider.PREF_SSID_DISABLED, next).apply();
        rebuild();
    }

    private void startManual(@Nullable String ssid) {
        Intent service = new Intent(requireContext(), ConnectionService.class)
                .putExtra(ConnectionService.EXTRA_FORCE, true)
                .putExtra(ConnectionService.EXTRA_DEBUG, true);
        Intent debug = new Intent(requireContext(), DebugActivity.class);
        if (ssid != null) {
            service.putExtra(ConnectionService.EXTRA_SSID, ssid);
            debug.putExtra(ConnectionService.EXTRA_SSID, ssid);
        }
        Util.startService(requireContext(), service);
        startActivity(debug);
    }
}
