/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 */

package pw.thedrhax.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import pw.thedrhax.mosmetro.R;

/**
 * Light / dark theme toggle (persisted). Call {@link #apply} before {@code super.onCreate()}.
 */
public final class ThemeUtils {
    public static final String PREF_DARK_THEME = "pref_dark_theme";

    private ThemeUtils() {}

    public static boolean isDark(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_DARK_THEME, false);
    }

    /** Must be called before {@code super.onCreate()}. */
    public static void apply(@NonNull Activity activity) {
        activity.setTheme(isDark(activity) ? R.style.AppBaseTheme_Dark : R.style.AppBaseTheme);
    }

    public static void applyDialog(@NonNull Activity activity) {
        activity.setTheme(isDark(activity)
                ? R.style.AppBaseTheme_Dark_Dialog
                : R.style.AppBaseTheme_Dialog);
    }

    public static boolean toggle(@NonNull Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean next = !prefs.getBoolean(PREF_DARK_THEME, false);
        prefs.edit().putBoolean(PREF_DARK_THEME, next).commit();
        activity.recreate();
        return next;
    }

    /** Adds / refreshes the ActionBar theme toggle (top-right). */
    public static void addToggleToMenu(@NonNull Activity activity, @NonNull Menu menu) {
        MenuItem existing = menu.findItem(R.id.action_theme_toggle);
        if (existing != null) {
            menu.removeItem(R.id.action_theme_toggle);
        }
        boolean dark = isDark(activity);
        MenuItem item = menu.add(Menu.NONE, R.id.action_theme_toggle, Menu.NONE,
                dark ? R.string.theme_switch_light : R.string.theme_switch_dark);
        item.setIcon(dark ? R.drawable.ic_theme_sun : R.drawable.ic_theme_moon);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    public static boolean handleMenuItem(@NonNull Activity activity, @NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_theme_toggle) {
            toggle(activity);
            return true;
        }
        return false;
    }
}
