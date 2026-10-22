package com.shivjeet1.mynotes.util;

import android.content.Context;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.MenuItemCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.shivjeet1.mynotes.R;

/** Shared theme and settings actions for every app bar in the shell. */
public final class AppBarActions {

    private AppBarActions() {
    }

    public static void bind(Context context, MaterialToolbar toolbar, Runnable openSettings) {
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_theme) {
                toggleTheme(context);
                return true;
            }
            if (id == R.id.action_settings) {
                openSettings.run();
                return true;
            }
            return false;
        });

        MenuItem settingsItem = toolbar.getMenu().findItem(R.id.action_settings);
        if (settingsItem != null) {
            MenuItemCompat.setContentDescription(settingsItem, context.getString(R.string.settings));
            MenuItemCompat.setTooltipText(settingsItem, context.getString(R.string.settings));
        }
        updateThemeAction(context, toolbar);
    }

    public static void updateThemeAction(Context context, MaterialToolbar toolbar) {
        if (toolbar == null) {
            return;
        }
        MenuItem themeItem = toolbar.getMenu().findItem(R.id.action_theme);
        if (themeItem == null) {
            return;
        }
        boolean night = AppPreferences.isNightMode(context);
        int titleRes = night ? R.string.switch_to_light : R.string.switch_to_dark;
        themeItem.setIcon(night
                ? R.drawable.ic_theme_light_24
                : R.drawable.ic_theme_dark_24);
        themeItem.setTitle(titleRes);
        MenuItemCompat.setContentDescription(themeItem, context.getString(titleRes));
        MenuItemCompat.setTooltipText(themeItem, context.getString(titleRes));
    }

    private static void toggleTheme(Context context) {
        AppPreferences.ThemeMode next = AppPreferences.isNightMode(context)
                ? AppPreferences.ThemeMode.LIGHT
                : AppPreferences.ThemeMode.DARK;
        AppPreferences.setThemeMode(context, next);
        AppCompatDelegate.setDefaultNightMode(AppPreferences.toNightMode(next));
    }
}
