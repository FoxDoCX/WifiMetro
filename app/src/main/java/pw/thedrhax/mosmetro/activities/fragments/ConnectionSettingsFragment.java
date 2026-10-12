package pw.thedrhax.mosmetro.activities.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.mosmetro.activities.SafeViewActivity;
import pw.thedrhax.util.CaptivePortalFix;
import pw.thedrhax.util.Randomizer;

public class ConnectionSettingsFragment extends NestedFragment {
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_conn, rootKey);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Randomizer(requireContext()).cached_useragent();
        setTitle(getString(R.string.pref_category_connection));

        final CheckBoxPreference mmv2 = findPreference("pref_mosmetro_v2_wv");
        if (mmv2 != null) {
            mmv2.setOnPreferenceClickListener(preference -> {
                if (mmv2.isChecked()) {
                    mmv2.setChecked(false);
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.warning)
                            .setMessage(R.string.pref_mosmetro_v2_wv_warning)
                            .setPositiveButton("Включить", (d, i) -> mmv2.setChecked(true))
                            .setNegativeButton("Отмена", (d, i) -> d.cancel())
                            .setOnCancelListener(d -> mmv2.setChecked(false))
                            .setCancelable(true)
                            .show();
                    return false;
                }
                return true;
            });
        }

        CaptivePortalFix cpf = new CaptivePortalFix(requireContext());
        CheckBoxPreference pref_captive_fix = findPreference("pref_captive_fix");
        CheckBoxPreference pref_internet_midsession = findPreference("pref_internet_midsession");
        if (pref_captive_fix == null || pref_internet_midsession == null) return;

        boolean isApplied = cpf.isApplied();
        pref_captive_fix.setChecked(isApplied);
        if (isApplied) {
            pref_internet_midsession.setChecked(false);
            pref_internet_midsession.setEnabled(false);
        }
        if (cpf.hasSecureSettingsAccess()) {
            pref_captive_fix.setSummary(R.string.pref_captive_fix_summary);
        }

        pref_captive_fix.setOnPreferenceClickListener(preference -> {
            cpf.toggle(success -> main.post(() -> {
                boolean applied = cpf.isApplied();
                if (applied) pref_internet_midsession.setChecked(false);
                pref_internet_midsession.setEnabled(!applied);
                pref_captive_fix.setChecked(applied);
                if (!success) showCaptivePortalFixDialog();
            }));
            return true;
        });
    }

    private void showCaptivePortalFixDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.root_unavailable)
                .setMessage(getString(R.string.captive_fix_message) + "\n\n"
                        + getString(R.string.captive_fix_adb))
                .setPositiveButton(R.string.captive_fix_open_manual, (dialog, which) ->
                        startActivity(new Intent(requireContext(), SafeViewActivity.class)
                                .putExtra("data", getString(R.string.captive_fix_manual_url))))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
