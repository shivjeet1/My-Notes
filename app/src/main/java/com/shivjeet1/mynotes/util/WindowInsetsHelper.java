package com.shivjeet1.mynotes.util;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.shivjeet1.mynotes.R;

/** Keeps both screens above system bars and, on modern Android, the keyboard. */
public final class WindowInsetsHelper {

    private WindowInsetsHelper() {
    }

    public static void apply(Activity activity, View root) {
        Window window = activity.getWindow();
        boolean night = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(!night);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Modern edge-to-edge. IME insets are reliable from API 30 on.
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            controller.setAppearanceLightNavigationBars(!night);
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);

            ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
                Insets bars = windowInsets.getInsets(
                        WindowInsetsCompat.Type.systemBars()
                                | WindowInsetsCompat.Type.displayCutout());
                Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right,
                        Math.max(bars.bottom, ime.bottom));
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(root);
        } else {
            // Older devices rely on the platform's fitsSystemWindows/adjustResize handling.
            WindowCompat.setDecorFitsSystemWindows(window, true);
            window.setStatusBarColor(ContextCompat.getColor(activity, R.color.surface_canvas));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.setNavigationBarColor(
                        ContextCompat.getColor(activity, R.color.surface_canvas));
                controller.setAppearanceLightNavigationBars(!night);
            } else {
                window.setNavigationBarColor(ContextCompat.getColor(activity,
                        night ? R.color.surface_canvas : R.color.surface_inverse));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.setNavigationBarDividerColor(Color.TRANSPARENT);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.setStatusBarContrastEnforced(false);
                window.setNavigationBarContrastEnforced(false);
            }
        }
    }
}
