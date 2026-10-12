package pw.thedrhax.mosmetro.activities.fragments;

import android.os.Bundle;

import pw.thedrhax.mosmetro.R;

public class SettingsFragment extends NestedFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
