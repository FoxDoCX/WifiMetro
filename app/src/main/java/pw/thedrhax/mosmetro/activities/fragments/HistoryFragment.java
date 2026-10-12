package pw.thedrhax.mosmetro.activities.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.ConnectionHistory;

public class HistoryFragment extends NestedFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(screen);
        rebuild();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.pref_history));
    }

    private void rebuild() {
        PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        ConnectionHistory history = new ConnectionHistory(requireContext());
        List<ConnectionHistory.Entry> entries = history.read();

        Preference export = new Preference(requireContext());
        export.setTitle(R.string.pref_history_export);
        export.setOnPreferenceClickListener(p -> {
            exportCsv(history);
            return true;
        });
        screen.addPreference(export);

        Preference clear = new Preference(requireContext());
        clear.setTitle(R.string.pref_history_clear);
        clear.setOnPreferenceClickListener(p -> {
            history.clear();
            rebuild();
            return true;
        });
        screen.addPreference(clear);

        PreferenceCategory list = new PreferenceCategory(requireContext());
        list.setTitle(R.string.pref_history);
        screen.addPreference(list);

        if (entries.isEmpty()) {
            Preference empty = new Preference(requireContext());
            empty.setTitle(R.string.pref_history_empty);
            empty.setSelectable(false);
            list.addPreference(empty);
            return;
        }

        DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (ConnectionHistory.Entry e : entries) {
            Preference item = new Preference(requireContext());
            item.setTitle(getString(R.string.pref_history_item, e.ssid, e.result, e.provider));
            item.setSummary(fmt.format(new Date(e.timestamp)) + " · " + e.duration + " ms");
            item.setSelectable(false);
            list.addPreference(item);
        }
    }

    private void exportCsv(ConnectionHistory history) {
        try {
            File file = new File(requireContext().getCacheDir(), "wifimetro-history.csv");
            try (OutputStreamWriter w = new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8)) {
                w.write(history.toCsv());
            }
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    "pw.thedrhax.mosmetro.provider", file);
            Intent share = new Intent(Intent.ACTION_SEND)
                    .setType("text/csv")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.pref_history_export)));
        } catch (Exception ex) {
            Toast.makeText(requireContext(),
                    getString(R.string.error, ex.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
}
