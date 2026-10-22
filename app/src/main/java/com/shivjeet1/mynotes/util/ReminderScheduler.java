package com.shivjeet1.mynotes.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.shivjeet1.mynotes.Model.Task;
import com.shivjeet1.mynotes.receiver.ReminderReceiver;

/** Schedules and cancels task reminders with an inexact fallback. */
public final class ReminderScheduler {

    private static final String ACTION_REMIND = "com.shivjeet1.mynotes.action.REMIND";
    public static final String EXTRA_TASK_ID = "task_id";

    private ReminderScheduler() {
    }

    public static void schedule(Context context, Task task) {
        if (task == null || task.getId() <= 0 || task.isDone()
                || task.getRemindAt() <= 0 || task.isReminderFired()) {
            if (task != null) {
                cancel(context, task.getId());
            }
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        long triggerAt = task.getRemindAt();
        long now = System.currentTimeMillis();
        if (triggerAt <= now) {
            triggerAt = now + 1000L;
        }
        PendingIntent pendingIntent = pendingIntent(context, task.getId());
        if (canScheduleExact(alarmManager)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                return;
            } catch (SecurityException ignored) {
                // Fall through to an inexact alarm when permission was revoked.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
    }

    public static void cancel(Context context, int taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent(context, taskId));
        }
    }

    public static boolean canScheduleExact(AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return alarmManager.canScheduleExactAlarms();
    }

    private static PendingIntent pendingIntent(Context context, int taskId) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setAction(ACTION_REMIND)
                .putExtra(EXTRA_TASK_ID, taskId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, taskId, intent, flags);
    }
}
