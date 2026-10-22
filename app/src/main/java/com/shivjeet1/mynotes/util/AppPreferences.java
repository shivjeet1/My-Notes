package com.shivjeet1.mynotes.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

/** Small, typed wrapper around the app's local preferences. */
public final class AppPreferences {

    private static final String PREFS_NAME = "mynotes_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_REMINDER_OFFSET = "reminder_offset_minutes";
    private static final String KEY_DEFAULT_REMINDER = "default_reminder_enabled";
    private static final String KEY_NOTIFICATION_REQUESTED = "notification_permission_requested";

    private static final String VALUE_SYSTEM = "system";
    private static final String VALUE_LIGHT = "light";
    private static final String VALUE_DARK = "dark";

    public enum ThemeMode {
        SYSTEM, LIGHT, DARK
    }

    private AppPreferences() {
    }

    public static ThemeMode getThemeMode(Context context) {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = preferences.getString(KEY_THEME_MODE, VALUE_SYSTEM);
        if (VALUE_LIGHT.equals(stored)) {
            return ThemeMode.LIGHT;
        }
        if (VALUE_DARK.equals(stored)) {
            return ThemeMode.DARK;
        }
        return ThemeMode.SYSTEM;
    }

    public static void setThemeMode(Context context, ThemeMode mode) {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_THEME_MODE, toStoredValue(mode)).apply();
    }

    /** Offset in minutes before the due time for default reminders. */
    public static int getReminderOffsetMinutes(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_REMINDER_OFFSET, 0);
    }

    public static void setReminderOffsetMinutes(Context context, int minutes) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_REMINDER_OFFSET, Math.max(0, minutes)).apply();
    }

    public static boolean isDefaultReminderEnabled(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_DEFAULT_REMINDER, false);
    }

    public static void setDefaultReminderEnabled(Context context, boolean enabled) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_DEFAULT_REMINDER, enabled).apply();
    }

    public static boolean hasRequestedNotificationPermission(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTIFICATION_REQUESTED, false);
    }

    public static void setNotificationPermissionRequested(Context context) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_NOTIFICATION_REQUESTED, true).apply();
    }

    public static int toNightMode(ThemeMode mode) {
        switch (mode) {
            case LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case SYSTEM:
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    /** Whether the configuration currently in effect is the dark theme. */
    public static boolean isNightMode(Context context) {
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static String toStoredValue(ThemeMode mode) {
        switch (mode) {
            case LIGHT:
                return VALUE_LIGHT;
            case DARK:
                return VALUE_DARK;
            case SYSTEM:
            default:
                return VALUE_SYSTEM;
        }
    }
}
