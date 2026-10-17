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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import pw.thedrhax.mosmetro.R;

/**
 * Window / system-bar insets for AppCompat ActionBar + content.
 * <p>
 * Android 14 and below: keep {@code decorFitsSystemWindows=true} — the platform
 * lays out ActionBar and content correctly without manual padding.
 * Android 15+: edge-to-edge is enforced (opt-out ignored on 16+), so we pad
 * content ourselves. {@code setPadding} on {@code android.R.id.content} replaces
 * AppCompat's ActionBar inset — recompute top padding when they overlap.
 */
public final class SystemBarUtils {
    private SystemBarUtils() {}

    /** Edge-to-edge padding path (API 35+). Older OS uses classic layout. */
    private static boolean needsEdgeToEdge() {
        return Build.VERSION.SDK_INT >= 35;
    }

    /** Call before {@code super.onCreate()}. */
    public static void prepare(@NonNull Activity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, !needsEdgeToEdge());
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
        if (!needsEdgeToEdge()) {
            applyClassic(activity);
            return;
        }
        applyEdgeToEdge(activity);
    }

    /** Android 14 and below: system manages insets; only colors + small bottom gap. */
    private static void applyClassic(@NonNull Activity activity) {
        final Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);

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

        // Undo any edge-to-edge ActionBar margin left from a previous path / recreate
        View actionBarContainer = findActionBarContainer(decor);
        if (actionBarContainer != null) {
            actionBarContainer.setBackgroundColor(actionBarBg(activity));
            if (actionBarContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp =
                        (ViewGroup.MarginLayoutParams) actionBarContainer.getLayoutParams();
                if (lp.topMargin != 0) {
                    lp.topMargin = 0;
                    actionBarContainer.setLayoutParams(lp);
                }
            }
        }

        final View content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        content.setBackgroundColor(contentBg(activity));
        ViewCompat.setOnApplyWindowInsetsListener(content, null);

        // Small bottom breathing room; top is handled by ActionBar + decorFitsSystemWindows
        int bottomGap = dp(activity, 16);
        content.setPadding(0, 0, 0, bottomGap);

        padScrollable(content.findViewById(androidx.preference.R.id.recycler_view));
        padScrollable(content.findViewById(android.R.id.list));
    }

    private static void applyEdgeToEdge(@NonNull Activity activity) {
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

        // ActionBar height may be 0 on first pass — re-apply after layout.
        content.post(() -> {
            WindowInsetsCompat current = ViewCompat.getRootWindowInsets(content);
            if (current != null) {
                applyInsets(activity, decor, content, current);
            }
            content.post(() -> {
                WindowInsetsCompat again = ViewCompat.getRootWindowInsets(content);
                if (again != null) {
                    applyInsets(activity, decor, content, again);
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

        int statusTop = Math.max(bars.top, statusBarHeight(activity.getResources()));
        int bottom = Math.max(bars.bottom, ime.bottom);
        if (bottom <= 0) {
            bottom = navigationBarHeight(activity.getResources());
        }
        int gap = dp(activity, 8);
        int bottomGap = dp(activity, 16);
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
                actionBarContainer.setPadding(
                        actionBarContainer.getPaddingLeft(),
                        statusTop,
                        actionBarContainer.getPaddingRight(),
                        actionBarContainer.getPaddingBottom());
            }
        }

        int topPad;
        if (floating) {
            topPad = gap;
        } else if (actionBarContainer != null && actionBarContainer.getHeight() > 0) {
            // setPadding replaces AppCompat ActionBar inset — detect real overlap.
            int[] abLoc = new int[2];
            int[] contentLoc = new int[2];
            actionBarContainer.getLocationOnScreen(abLoc);
            content.getLocationOnScreen(contentLoc);
            int abBottom = abLoc[1] + actionBarContainer.getHeight();
            if (contentLoc[1] < abBottom - 2) {
                topPad = (abBottom - contentLoc[1]) + gap;
            } else {
                topPad = gap;
            }
        } else {
            topPad = statusTop + actionBarHeight(activity) + gap;
        }

        int bottomPad = bottom > 0 ? bottom + bottomGap : bottomGap;
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
        int appcompatId = androidx.appcompat.R.id.action_bar_container;
        View v = decor.findViewById(appcompatId);
        if (v != null) return v;

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
        if (activity.getTheme().resolveAttribute(androidx.appcompat.R.attr.actionBarSize, tv, true)
                || activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    tv.data, activity.getResources().getDisplayMetrics());
        }
        return dp(activity, 56);
    }

    private static int dp(@NonNull Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
