/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 */

package pw.thedrhax.mosmetro.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import io.sentry.Sentry;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.fragments.AboutFragment;
import pw.thedrhax.mosmetro.activities.fragments.ConnectionSettingsFragment;
import pw.thedrhax.mosmetro.activities.fragments.DebugSettingsFragment;
import pw.thedrhax.mosmetro.activities.fragments.HistoryFragment;
import pw.thedrhax.mosmetro.activities.fragments.NestedFragment;
import pw.thedrhax.mosmetro.activities.fragments.NetworksFragment;
import pw.thedrhax.mosmetro.activities.fragments.NotificationSettingsFragment;
import pw.thedrhax.mosmetro.activities.fragments.SettingsFragment;
import pw.thedrhax.mosmetro.services.AutoConnect;
import pw.thedrhax.mosmetro.services.ConnectionService;
import pw.thedrhax.mosmetro.updater.UpdateChecker;
import pw.thedrhax.util.OemAutostart;
import pw.thedrhax.util.PermissionUtils;
import pw.thedrhax.util.SystemBarUtils;
import pw.thedrhax.util.ThemeUtils;
import pw.thedrhax.util.Util;
import pw.thedrhax.util.Version;

public class SettingsActivity extends AppCompatActivity {
    public static final String ACTION_MIDSESSION = "midsession";
    public static final String ACTION_CHECK_UPDATES =
            "pw.thedrhax.mosmetro.activities.SettingsActivity.CHECK_UPDATES";

    private SettingsFragment fragment;
    private PermissionUtils pu;
    private UpdateChecker updater;
    private SharedPreferences settings;
    private CheckBoxPreference pref_autoconnect;
    private CheckBoxPreference pref_autoconnect_service;
    private long permissionRequestTimestamp = 0;

    private void midsession_dialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.midsession_dialog)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    Bundle args = new Bundle();
                    args.putString(NestedFragment.ARG_SCROLL_TO, "pref_captive");
                    ConnectionSettingsFragment next = new ConnectionSettingsFragment();
                    next.setArguments(args);
                    replaceFragment("pref_conn", next);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ThemeUtils.addToggleToMenu(this, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (ThemeUtils.handleMenuItem(this, item)) return true;
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void update_checker_setup() {
        Preference pref_updater_check = fragment.findPreference("pref_updater_check");
        if (pref_updater_check == null) return;

        updater = new UpdateChecker(this);
        updater.init();

        Preference.OnPreferenceClickListener click = preference -> {
            boolean manual = preference != null;
            updater.async_check(new UpdateChecker.Callback() {
                @Override
                public void onStart() {
                    pref_updater_check.setEnabled(false);
                }

                @Override
                public void onResult(@Nullable UpdateChecker.Release result) {
                    pref_updater_check.setEnabled(true);
                    if (result == null) {
                        Toast.makeText(SettingsActivity.this,
                                getString(R.string.error, getString(R.string.auth_error_server)),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (result.isNewer() || manual) {
                        result.showDialog();
                    }
                }
            });
            return false;
        };

        pref_updater_check.setOnPreferenceClickListener(click);

        if (ACTION_CHECK_UPDATES.equals(getIntent() != null ? getIntent().getAction() : null)
                || settings.getBoolean(UpdateChecker.PREF_ENABLED, true)) {
            click.onPreferenceClick(ACTION_CHECK_UPDATES.equals(
                    getIntent() != null ? getIntent().getAction() : null)
                    ? pref_updater_check : null);
        }
    }

    @RequiresApi(23)
    private void energy_saving_setup() {
        if (settings.getBoolean("pref_battery_saving_ignore", false) || pu.isBatterySavingIgnored()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_battery_saving)
                .setMessage(R.string.dialog_battery_saving_summary)
                .setPositiveButton(R.string.permission_request,
                        (d, w) -> pu.requestBatterySavingIgnore())
                .setNeutralButton(R.string.open_settings,
                        (d, w) -> pu.openBatterySavingSettings())
                .setNegativeButton(R.string.ignore, (d, w) -> settings.edit()
                        .putBoolean("pref_battery_saving_ignore", true).apply())
                .show();
    }

    private void changeCheckBox(CheckBoxPreference pref, boolean checked) {
        if (pref == null) return;
        pref.setChecked(checked);
        if (pref.getOnPreferenceChangeListener() != null) {
            pref.getOnPreferenceChangeListener().onPreferenceChange(pref, checked);
        }
    }

    private void openAppSettings() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", getPackageName(), null)));
    }

    @RequiresApi(28)
    private void showLocationDialog(boolean coarse) {
        new AlertDialog.Builder(this).setTitle(R.string.warning)
                .setMessage(coarse ? getString(R.string.coarse_location_permission)
                        : getString(R.string.background_location_permission))
                .setNegativeButton(coarse ? getString(R.string.force_on) : getString(R.string.force_off),
                        (dialogInterface, i) -> {
                            if (coarse) {
                                settings.edit().putBoolean("pref_location_ignore_autoconnect", true).apply();
                                changeCheckBox(pref_autoconnect, true);
                            } else {
                                settings.edit().putBoolean("pref_location_ignore_autoconnect_service", true).apply();
                                changeCheckBox(pref_autoconnect_service, false);
                            }
                        })
                .setPositiveButton(R.string.permission_request, (dialogInterface, i) -> {
                    permissionRequestTimestamp = System.currentTimeMillis();
                    if (coarse && Build.VERSION.SDK_INT >= 33 && !pu.isNearbyWifiGranted()) {
                        pu.requestNearbyWifi(SettingsActivity.this, 4);
                    } else if (coarse || Build.VERSION.SDK_INT < 29) {
                        pu.requestFineLocation(SettingsActivity.this, 1);
                    } else {
                        pu.requestBackgroundLocation(SettingsActivity.this, 2);
                    }
                })
                .setCancelable(true)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 3) return;
        if (permissions.length == 0 || grantResults.length == 0) return;
        if (pref_autoconnect == null || pref_autoconnect_service == null) return;

        boolean granted = requestCode == 2
                ? Build.VERSION.SDK_INT >= 29 && pu.isBackgroundLocationGranted()
                : pu.canReadWifiSsid();

        boolean auto = System.currentTimeMillis() - permissionRequestTimestamp < 200;
        if (auto && !granted && requestCode != 0) {
            openAppSettings();
        }

        if (granted) {
            if (Build.VERSION.SDK_INT >= 29 && pu.isBackgroundLocationGranted()) {
                changeCheckBox(pref_autoconnect_service, false);
            }
            if (requestCode != 2) {
                changeCheckBox(pref_autoconnect, true);
            }
        } else {
            if (requestCode != 2) {
                changeCheckBox(pref_autoconnect, false);
            }
            changeCheckBox(pref_autoconnect_service, true);
        }
    }

    @RequiresApi(28)
    private void location_permission_setup() {
        if (!settings.getBoolean("pref_location_ignore", false) && !pu.canReadWifiSsid()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.location_permission)
                    .setMessage(Build.VERSION.SDK_INT >= 33
                            ? getString(R.string.nearby_wifi_permission_saving)
                            : getString(R.string.location_permission_saving))
                    .setPositiveButton(R.string.permission_request, (d, w) -> {
                        if (Build.VERSION.SDK_INT >= 33) {
                            pu.requestNearbyWifi(SettingsActivity.this, 4);
                        } else {
                            pu.requestFineLocation(SettingsActivity.this, 0);
                        }
                    })
                    .setNeutralButton(R.string.open_settings, (d, w) -> pu.openAppSettings())
                    .setNegativeButton(R.string.ignore, (d, w) -> settings.edit()
                            .putBoolean("pref_location_ignore", true).apply())
                    .show();
        }

        if (!settings.getBoolean("pref_location_ignore_autoconnect", false)
                && !pu.canReadWifiSsid()
                && pref_autoconnect.isChecked()) {
            changeCheckBox(pref_autoconnect, false);
        }

        if (!settings.getBoolean("pref_location_ignore_autoconnect_service", false)
                && Build.VERSION.SDK_INT >= 29
                && !pu.isBackgroundLocationGranted()
                && !pref_autoconnect_service.isChecked()) {
            changeCheckBox(pref_autoconnect_service, true);
        }
    }

    private void replaceFragment(String id, Fragment next) {
        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, next)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(id)
                    .commit();
            getWindow().getDecorView().post(() -> SystemBarUtils.apply(SettingsActivity.this));
        } catch (IllegalStateException ex) {
            Sentry.captureException(ex);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        SystemBarUtils.prepare(this);
        super.onCreate(savedInstanceState);
        pu = new PermissionUtils(this);
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (Build.VERSION.SDK_INT >= 33 && !pu.isNotificationsGranted()) {
            pu.requestNotifications(this, 3);
        }

        FragmentManager fmanager = getSupportFragmentManager();
        fragment = new SettingsFragment();
        fmanager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
        fmanager.executePendingTransactions();
        SystemBarUtils.apply(this);

        fmanager.addOnBackStackChangedListener(() -> {
            boolean root = fmanager.getBackStackEntryCount() == 0;
            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                bar.setDisplayHomeAsUpEnabled(!root);
                if (root) bar.setTitle(R.string.app_name);
            }
            SystemBarUtils.apply(SettingsActivity.this);
        });

        if (Build.VERSION.SDK_INT >= 26) {
            Preference pref_shortcut = fragment.findPreference("pref_shortcut");
            if (pref_shortcut != null) {
                fragment.getPreferenceScreen().removePreference(pref_shortcut);
            }
        }

        pref_autoconnect = fragment.findPreference("pref_autoconnect");
        pref_autoconnect_service = fragment.findPreference("pref_autoconnect_service");

        if (pref_autoconnect != null) {
            pref_autoconnect.setOnPreferenceChangeListener((preference, new_value) -> {
                boolean force = settings.getBoolean("pref_location_ignore_autoconnect", false);
                boolean enabled = (Boolean) new_value;

                if (!force && enabled && Build.VERSION.SDK_INT >= 28 && !pu.canReadWifiSsid()) {
                    showLocationDialog(true);
                    return false;
                }

                if (!enabled) {
                    Util.startService(this, new Intent(this, ConnectionService.class)
                            .setAction(ConnectionService.ACTION_STOP));
                    settings.edit().putBoolean("pref_location_ignore_autoconnect", false).apply();
                }

                boolean persistent = pref_autoconnect_service != null
                        && pref_autoconnect_service.isChecked();
                AutoConnect.apply(this, enabled, persistent);
                if (pref_autoconnect_service != null) {
                    pref_autoconnect_service.setEnabled(enabled);
                }
                return true;
            });
        }

        if (pref_autoconnect_service != null) {
            pref_autoconnect_service.setOnPreferenceChangeListener((preference, new_value) -> {
                boolean force = settings.getBoolean("pref_location_ignore_autoconnect_service", false);
                boolean persistent = (Boolean) new_value;

                if (!force && !persistent && Build.VERSION.SDK_INT >= 29
                        && !pu.isBackgroundLocationGranted()) {
                    showLocationDialog(false);
                    return false;
                }

                if (persistent) {
                    settings.edit().putBoolean("pref_location_ignore_autoconnect_service", false).apply();
                }

                boolean enabled = pref_autoconnect != null && pref_autoconnect.isChecked();
                AutoConnect.apply(this, enabled, persistent);
                return true;
            });
        }

        if (pref_autoconnect != null && !pref_autoconnect.isChecked()
                && pref_autoconnect_service != null) {
            pref_autoconnect_service.setEnabled(false);
        }

        AutoConnect.ensure(this);

        setClick("pref_networks", () -> replaceFragment("networks", new NetworksFragment()));
        setClick("pref_history", () -> replaceFragment("history", new HistoryFragment()));
        setClick("pref_conn", () -> replaceFragment("conn", new ConnectionSettingsFragment()));
        setClick("pref_notify", () -> replaceFragment("notify", new NotificationSettingsFragment()));
        setClick("pref_debug", () -> replaceFragment("debug", new DebugSettingsFragment()));
        setClick("pref_about", () -> replaceFragment("about", new AboutFragment()));
        setClick("pref_oem_autostart", () -> OemAutostart.open(this));

        Preference app_name = fragment.findPreference("app_name");
        if (app_name != null) {
            app_name.setSummary(getString(R.string.app_signature) + "\n"
                    + getString(R.string.version, Version.getFormattedVersion()));
        }

        update_checker_setup();

        if (Build.VERSION.SDK_INT >= 23) energy_saving_setup();
        if (Build.VERSION.SDK_INT >= 28) location_permission_setup();
        if (getIntent() != null && ACTION_MIDSESSION.equals(getIntent().getAction())) {
            midsession_dialog();
        }
    }

    private void setClick(String key, Runnable action) {
        Preference pref = fragment.findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceClickListener(p -> {
                action.run();
                return true;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SystemBarUtils.apply(this);
    }

    @Override
    protected void onDestroy() {
        if (updater != null) updater.deinit();
        super.onDestroy();
    }
}
