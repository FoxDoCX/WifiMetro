/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.thedrhax.mosmetro.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.edmodo.rangebar.RangeBar;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.Util;

public class RangeBarPreference extends Preference {

    public RangeBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public RangeBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public RangeBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RangeBarPreference(Context context) {
        super(context);
    }

    private int defaultMin = 1, defaultMax = 10, min = 1, max = 10, step = 1;

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RangeBarPreference, 0, 0);

        defaultMin = ta.getInt(R.styleable.RangeBarPreference_defaultMin, 1);
        defaultMax = ta.getInt(R.styleable.RangeBarPreference_defaultMax, 10);
        min = ta.getInt(R.styleable.RangeBarPreference_min, 1);
        max = ta.getInt(R.styleable.RangeBarPreference_max, 10);
        step = ta.getInt(R.styleable.RangeBarPreference_step, 1);

        ta.recycle();
        setPersistent(false);
    }

    @Override
    protected void onClick() {
        final String key_min = getKey() + "_min";
        final String key_max = getKey() + "_max";

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        final View view = View.inflate(getContext(), R.layout.rangebar_preference, null);
        final RangeBar rangebar = view.findViewById(R.id.rangebar);
        final TextView rangetext = view.findViewById(R.id.rangetext);

        rangebar.setTickCount((max - min) / step + 1);
        rangebar.setOnRangeBarChangeListener((rangeBar, left, right) ->
                rangetext.setText("" + (left * step + min) + " - " + (right * step + min)));

        int current_min = Math.max(min, Util.getIntPreference(getContext(), key_min, defaultMin));
        int current_max = Math.min(max, Util.getIntPreference(getContext(), key_max, defaultMax));

        rangebar.setThumbIndices((current_min - min) / step, (current_max - min) / step);

        new AlertDialog.Builder(getContext())
                .setTitle(getTitle())
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> settings.edit()
                        .putInt(key_min, rangebar.getLeftIndex() * step + min)
                        .putInt(key_max, rangebar.getRightIndex() * step + min)
                        .apply())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
