/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 */

package pw.thedrhax.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import pw.thedrhax.mosmetro.R;

/**
 * Edge-to-edge insets for AppCompat ActionBar + content.
 * Content is a sibling below the ActionBar — do not add ActionBar height as top padding.
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

        final View content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        content.setBackgroundColor(contentBg(activity));

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            applyInsets(activity, decor, content, insets);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(content);

        content.post(() -> {
            WindowInsetsCompat current = ViewCompat.getRootWindowInsets(content);
            if (current != null) {
                applyInsets(activity, decor, content, current);
            }
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

    private static boolean isFloating(@NonNull Activity activity) {
        android.util.TypedValue value = new android.util.TypedValue();
        return activity.getTheme().resolveAttribute(android.R.attr.windowIsFloating, value, true)
                && value.type == android.util.TypedValue.TYPE_INT_BOOLEAN
                && value.data != 0;
    }

    private static void applyInsets(@NonNull Activity activity, @NonNull View decor,
                                    @NonNull View content, @NonNull WindowInsetsCompat insets) {
        Insets bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.displayCutout());
        Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

        int statusTop = bars.top;
        int bottom = Math.max(bars.bottom, ime.bottom);
        int gap = dp(activity, 8);
        boolean floating = isFloating(activity);

        View actionBarContainer = findActionBarContainer(decor);
        if (actionBarContainer != null && !floating) {
            actionBarContainer.setBackgroundColor(actionBarBg(activity));
            if (actionBarContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp =
                        (ViewGroup.MarginLayoutParams) actionBarContainer.getLayoutParams();
                if (lp.topMargin != statusTop) {
                    lp.topMargin = statusTop;
                    actionBarContainer.setLayoutParams(lp);
                }
            } else {
                // Fallback: pad the action bar itself
                actionBarContainer.setPadding(
                        actionBarContainer.getPaddingLeft(),
                        statusTop,
                        actionBarContainer.getPaddingRight(),
                        actionBarContainer.getPaddingBottom());
            }
        }

        // Content sits below ActionBar in AppCompat — never add actionBarHeight again.
        // Dialogs: only small padding; system already sizes the window.
        int topPad;
        if (floating) {
            topPad = gap;
        } else if (actionBarContainer != null) {
            topPad = gap;
        } else {
            topPad = statusTop + gap;
        }

        int bottomPad = bottom > 0 ? bottom + gap : gap;
        content.setPadding(bars.left, topPad, bars.right, bottomPad);

        // PreferenceFragmentCompat uses a RecyclerView, not android.R.id.list
        padScrollable(content.findViewById(androidx.preference.R.id.recycler_view));
        padScrollable(content.findViewById(android.R.id.list));
    }

    private static void padScrollable(@Nullable View scrollable) {
        if (scrollable == null) return;
        // Bottom inset is on content — avoid double bottom padding on the list
        scrollable.setPadding(scrollable.getPaddingLeft(), scrollable.getPaddingTop(),
                scrollable.getPaddingRight(), 0);
        if (scrollable instanceof ViewGroup) {
            ((ViewGroup) scrollable).setClipToPadding(false);
        }
    }

    @Nullable
    private static View findActionBarContainer(@NonNull View decor) {
        // AppCompat
        int appcompatId = androidx.appcompat.R.id.action_bar_container;
        View v = decor.findViewById(appcompatId);
        if (v != null) return v;

        // Platform ActionBar (legacy)
        int platformId = decor.getResources()
                .getIdentifier("action_bar_container", "id", "android");
        if (platformId != 0) {
            v = decor.findViewById(platformId);
            if (v != null) return v;
        }

        int abId = androidx.appcompat.R.id.action_bar;
        View ab = decor.findViewById(abId);
        if (ab != null && ab.getParent() instanceof View) {
            return (View) ab.getParent();
        }
        return null;
    }

    private static int dp(@NonNull Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
