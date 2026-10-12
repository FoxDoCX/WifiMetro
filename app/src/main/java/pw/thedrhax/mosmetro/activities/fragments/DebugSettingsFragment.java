package pw.thedrhax.mosmetro.activities.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import pw.thedrhax.mosmetro.MosMetroApp;
import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Logger;

public class DebugSettingsFragment extends NestedFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_debug, rootKey);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.pref_debug));

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(requireContext());

        Preference.OnPreferenceChangeListener reload_logger = (pref, new_value) -> {
            settings.edit().putBoolean(pref.getKey(), (Boolean) new_value).apply();
            Logger.configure(requireContext());
            return true;
        };

        CheckBoxPreference pref_debug_acra = findPreference("acra.enable");
        CheckBoxPreference pref_debug_last_log = findPreference("pref_debug_last_log");
        CheckBoxPreference pref_debug_testing = findPreference("pref_debug_testing");
        CheckBoxPreference pref_debug_logcat = findPreference("pref_debug_logcat");
        if (pref_debug_acra == null || pref_debug_last_log == null
                || pref_debug_testing == null || pref_debug_logcat == null) {
            return;
        }

        if (!MosMetroApp.isCrashReportingAvailable()) {
            pref_debug_acra.setEnabled(false);
            pref_debug_acra.setChecked(false);
            pref_debug_acra.setSummary(R.string.pref_debug_acra_summary);
            pref_debug_last_log.setEnabled(false);
            pref_debug_testing.setEnabled(false);
        } else {
            pref_debug_last_log.setEnabled(pref_debug_acra.isChecked());
            pref_debug_testing.setEnabled(pref_debug_last_log.isChecked());
        }

        pref_debug_acra.setOnPreferenceChangeListener((pref, new_value) -> {
            if (!(Boolean) new_value) {
                pref_debug_last_log.setChecked(false);
                pref_debug_testing.setChecked(false);
                pref_debug_testing.setEnabled(false);
            }
            pref_debug_last_log.setEnabled((Boolean) new_value);
            return true;
        });

        pref_debug_last_log.setOnPreferenceChangeListener((pref, new_value) -> {
            if (!(Boolean) new_value) {
                pref_debug_testing.setChecked(false);
            }
            pref_debug_testing.setEnabled((Boolean) new_value);
            return true;
        });

        pref_debug_testing.setOnPreferenceChangeListener(reload_logger);
        pref_debug_logcat.setOnPreferenceChangeListener(reload_logger);
    }
}
