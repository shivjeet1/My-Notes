package com.shivjeet1.mynotes.receiver;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.shivjeet1.mynotes.NotesApplication;
import com.shivjeet1.mynotes.R;
import com.shivjeet1.mynotes.TaskEditorActivity;
import com.shivjeet1.mynotes.Database.RoomDb;
import com.shivjeet1.mynotes.Model.Task;
import com.shivjeet1.mynotes.util.ReminderScheduler;
import com.shivjeet1.mynotes.util.TaskDates;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Posts a reminder notification and marks the task so it cannot fire twice. */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra(ReminderScheduler.EXTRA_TASK_ID, 0);
        if (taskId <= 0) {
            return;
        }
        Context appContext = context.getApplicationContext();
        PendingResult pendingResult = goAsync();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                RoomDb database = RoomDb.getInstance(appContext);
                Task task = database.taskDao().getById(taskId);
                if (task == null || task.isDone()) {
                    return;
                }
                database.taskDao().setReminderFired(taskId, true);
                showNotification(appContext, task);
            } finally {
                pendingResult.finish();
                executor.shutdown();
            }
        });
    }

    private void showNotification(Context context, Task task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotesApplication.createReminderChannel(context);

        Intent openIntent = new Intent(context, TaskEditorActivity.class)
                .putExtra(TaskEditorActivity.EXTRA_TASK_ID, task.getId())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context, task.getId(), openIntent, flags);

        String body = context.getString(R.string.reminder_notification_body,
                TaskDates.formatDateTime(context, task.getDueAt()));
        String title = TextUtils.isEmpty(task.getTitle())
                ? context.getString(R.string.untitled) : task.getTitle();
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, NotesApplication.REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification_24)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setAutoCancel(true)
                        .setContentIntent(openPendingIntent)
                        .addAction(0, context.getString(R.string.reminder_notification_open),
                                openPendingIntent);
        try {
            NotificationManagerCompat.from(context).notify(task.getId(), builder.build());
        } catch (SecurityException ignored) {
            // Notification permission was revoked between the check and notify.
        }
    }
}
