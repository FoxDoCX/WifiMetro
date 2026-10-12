package pw.thedrhax.mosmetro.activities.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;

import pw.thedrhax.mosmetro.R;

public class AboutFragment extends NestedFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.about, rootKey);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.about));
    }
}
