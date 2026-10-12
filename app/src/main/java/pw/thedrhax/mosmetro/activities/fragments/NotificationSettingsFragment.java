package pw.thedrhax.mosmetro.activities.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;

import pw.thedrhax.mosmetro.R;

public class NotificationSettingsFragment extends NestedFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_notify, rootKey);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.pref_category_notifications));

        final CheckBoxPreference foreground = findPreference("pref_notify_foreground");
        final CheckBoxPreference success = findPreference("pref_notify_success");
        final CheckBoxPreference success_lock = findPreference("pref_notify_success_lock");
        if (foreground == null || success == null || success_lock == null) return;

        Preference.OnPreferenceChangeListener listener = (preference, newValue) -> {
            boolean locked = (Boolean) newValue;
            success.setEnabled(!locked);
            success_lock.setEnabled(!locked);
            return true;
        };
        foreground.setOnPreferenceChangeListener(listener);
        listener.onPreferenceChange(foreground, foreground.isChecked());
    }
}
