package com.shivjeet1.mynotes;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.shivjeet1.mynotes.util.AppPreferences;

/**
 * Applies the saved theme before any activity inflates, so a cold start never
 * flashes the wrong palette before switching to the stored mode.
 */
public class NotesApplication extends Application {

    public static final String REMINDER_CHANNEL_ID = "reminders";

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(
                AppPreferences.toNightMode(AppPreferences.getThemeMode(this)));
        createReminderChannel(this);
    }

    public static void createReminderChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel existing = manager.getNotificationChannel(REMINDER_CHANNEL_ID);
        if (existing != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(REMINDER_CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        manager.createNotificationChannel(channel);
    }
}
