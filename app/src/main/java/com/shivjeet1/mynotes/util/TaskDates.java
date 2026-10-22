package com.shivjeet1.mynotes.util;

import android.content.Context;
import android.text.format.DateFormat;

import com.shivjeet1.mynotes.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Locale-aware task date formatting and day-boundary helpers. */
public final class TaskDates {

    private TaskDates() {
    }

    public static boolean isToday(long time) {
        return isSameDay(time, System.currentTimeMillis());
    }

    public static boolean isTomorrow(long time) {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        return isSameDay(time, tomorrow.getTimeInMillis());
    }

    public static boolean isSameDay(long first, long second) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(first);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(second);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    public static long endOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public static long startOfTomorrow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static String formatDate(Context context, long time) {
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                .format(new Date(time));
    }

    public static String formatTime(Context context, long time) {
        return DateFormat.getTimeFormat(context).format(new Date(time));
    }

    public static String formatDateTime(Context context, long time) {
        return formatDate(context, time) + ", " + formatTime(context, time);
    }

    /** The due line shown on a task row. */
    public static String formatDue(Context context, long dueAt, boolean done) {
        if (dueAt <= 0) {
            return "";
        }
        long now = System.currentTimeMillis();
        if (!done && dueAt < now) {
            return context.getString(R.string.overdue);
        }
        if (isToday(dueAt)) {
            return context.getString(R.string.today) + " " + formatTime(context, dueAt);
        }
        if (isTomorrow(dueAt)) {
            return context.getString(R.string.tomorrow) + " " + formatTime(context, dueAt);
        }
        return formatDateTime(context, dueAt);
    }

    /** The reminder line shown on a task row. */
    public static String formatReminder(Context context, long remindAt, boolean fired) {
        if (remindAt <= 0) {
            return "";
        }
        if (fired) {
            return context.getString(R.string.reminder_sent);
        }
        if (isToday(remindAt)) {
            return context.getString(R.string.reminder_for, formatTime(context, remindAt));
        }
        if (isTomorrow(remindAt)) {
            return context.getString(R.string.reminder_for,
                    context.getString(R.string.tomorrow) + " " + formatTime(context, remindAt));
        }
        return context.getString(R.string.reminder_for, formatDateTime(context, remindAt));
    }
}
