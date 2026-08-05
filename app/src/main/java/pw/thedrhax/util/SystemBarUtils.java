/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 */

package pw.thedrhax.util;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import pw.thedrhax.mosmetro.R;

/**
 * Keeps ActionBar / content clear of status bar and navigation bar
 * (3-button nav and gesture nav) on all API levels with edge-to-edge.
 */
public final class SystemBarUtils {
    private SystemBarUtils() {}

    /** Call before {@code super.onCreate()}. */
    public static void prepare(@NonNull Activity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        if (Build.VERSION.SDK_INT >= 21) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int bar = barColor(activity);
            window.setStatusBarColor(bar);
            window.setNavigationBarColor(bar);
        }
    }

    /** Call after setContentView / PreferenceFragment attach. Idempotent. */
    public static void apply(@NonNull Activity activity) {
        final Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        if (Build.VERSION.SDK_INT >= 21) {
            int bar = barColor(activity);
            window.setStatusBarColor(bar);
            window.setNavigationBarColor(bar);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(true);
        }

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }

        final View decor = window.getDecorView();
        decor.setBackgroundColor(barColor(activity));
        ViewCompat.setOnApplyWindowInsetsListener(decor, (v, insets) -> {
            applyInsets(activity, decor, insets);
            // Consume so children don't double-apply; we pad content ourselves.
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(decor);

        decor.post(() -> {
            WindowInsetsCompat current = ViewCompat.getRootWindowInsets(decor);
            if (current != null) {
                applyInsets(activity, decor, current);
            } else {
                applyInsetsFallback(activity);
            }
            decor.post(() -> {
                WindowInsetsCompat again = ViewCompat.getRootWindowInsets(decor);
                if (again != null) {
                    applyInsets(activity, decor, again);
                }
            });
        });
    }

    private static int barColor(@NonNull Activity activity) {
        if (ThemeUtils.isDark(activity)) {
            return ContextCompat.getColor(activity, R.color.colorPrimaryDarkDarkTheme);
        }
        return ContextCompat.getColor(activity, R.color.colorPrimaryDark);
    }

    private static int contentBg(@NonNull Activity activity) {
        if (ThemeUtils.isDark(activity)) {
            return ContextCompat.getColor(activity, R.color.bgSecondaryDarkTheme);
        }
        return ContextCompat.getColor(activity, R.color.bgSecondary);
    }

    private static int actionBarBg(@NonNull Activity activity) {
        return ContextCompat.getColor(activity, R.color.colorPrimary);
    }

    private static void applyInsets(@NonNull Activity activity, @NonNull View decor,
                                    @NonNull WindowInsetsCompat insets) {
        Insets sys = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
        Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
        Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

        int top = Math.max(sys.top, statusBarHeight(activity.getResources()));
        int bottom = Math.max(nav.bottom, Math.max(sys.bottom, navigationBarHeight(activity.getResources())));
        bottom = Math.max(bottom, ime.bottom);
        int gap = dp(activity, 8);
        int bottomPad = bottom > 0 ? bottom + gap : gap;

        View actionBarContainer = findActionBarContainer(activity, decor);
        if (actionBarContainer != null
                && actionBarContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) actionBarContainer.getLayoutParams();
            if (lp.topMargin != top) {
                lp.topMargin = top;
                actionBarContainer.setLayoutParams(lp);
            }
            actionBarContainer.setBackgroundColor(actionBarBg(activity));
        }

        View content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        content.setBackgroundColor(contentBg(activity));

        int neededTop = gap;
        if (actionBarContainer != null && actionBarContainer.getHeight() > 0) {
            int[] abLoc = new int[2];
            int[] contentLoc = new int[2];
            actionBarContainer.getLocationOnScreen(abLoc);
            content.getLocationOnScreen(contentLoc);
            int abBottom = abLoc[1] + actionBarContainer.getHeight();
            if (contentLoc[1] < abBottom - 2) {
                neededTop = (abBottom - contentLoc[1]) + gap;
            }
        } else {
            neededTop = top + actionBarHeight(activity) + gap;
        }

        // Pad whole content: works for Preference lists AND custom layouts
        // (Debug "Try again", Feedback buttons) above 3-button / gesture nav.
        content.setPadding(sys.left, neededTop, sys.right, bottomPad);

        View list = content.findViewById(android.R.id.list);
        if (list != null) {
            // Bottom inset already on content — don't double-pad the list.
            list.setPadding(list.getPaddingLeft(), list.getPaddingTop(),
                    list.getPaddingRight(), 0);
            if (list instanceof ViewGroup) {
                ((ViewGroup) list).setClipToPadding(false);
            }
        }
    }

    private static void applyInsetsFallback(@NonNull Activity activity) {
        int top = statusBarHeight(activity.getResources());
        int bottom = navigationBarHeight(activity.getResources());
        int gap = dp(activity, 8);
        View decor = activity.getWindow().getDecorView();
        View actionBarContainer = findActionBarContainer(activity, decor);
        if (actionBarContainer != null
                && actionBarContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) actionBarContainer.getLayoutParams();
            lp.topMargin = top;
            actionBarContainer.setLayoutParams(lp);
        }
        View content = activity.findViewById(android.R.id.content);
        if (content != null) {
            content.setBackgroundColor(contentBg(activity));
            content.setPadding(0, top + actionBarHeight(activity) + gap, 0,
                    bottom > 0 ? bottom + gap : gap);
        }
    }

    private static View findActionBarContainer(@NonNull Activity activity, @NonNull View decor) {
        int abContainerId = activity.getResources()
                .getIdentifier("action_bar_container", "id", "android");
        if (abContainerId != 0) {
            View v = decor.findViewById(abContainerId);
            if (v != null) return v;
        }
        int abId = activity.getResources().getIdentifier("action_bar", "id", "android");
        if (abId != 0) {
            View ab = decor.findViewById(abId);
            if (ab != null && ab.getParent() instanceof View) {
                return (View) ab.getParent();
            }
        }
        return null;
    }

    private static int statusBarHeight(@NonNull Resources res) {
        int id = res.getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) {
            return res.getDimensionPixelSize(id);
        }
        return Math.round(24 * res.getDisplayMetrics().density);
    }

    private static int navigationBarHeight(@NonNull Resources res) {
        int id = res.getIdentifier("navigation_bar_height", "dimen", "android");
        if (id > 0) {
            return res.getDimensionPixelSize(id);
        }
        return 0;
    }

    private static int actionBarHeight(@NonNull Activity activity) {
        TypedValue tv = new TypedValue();
        if (activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    tv.data, activity.getResources().getDisplayMetrics());
        }
        return dp(activity, 56);
    }

    private static int dp(@NonNull Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
