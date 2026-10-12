/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 */

package pw.thedrhax.mosmetro.activities.fragments;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import pw.thedrhax.util.ThemeUtils;

public class NestedFragment extends PreferenceFragmentCompat {
    public static final String ARG_SCROLL_TO = "scrollTo";

    protected void setTitle(String title) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;
        ActionBar bar = activity.getSupportActionBar();
        if (bar != null) bar.setTitle(title);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // subclasses load resources
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        if (getActivity() != null) {
            ThemeUtils.addToggleToMenu(getActivity(), menu);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_SCROLL_TO)) {
            Preference target = findPreference(args.getString(ARG_SCROLL_TO));
            if (target != null) {
                scrollToPreference(target);
            }
        }
    }
}
