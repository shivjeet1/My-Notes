package com.shivjeet1.mynotes.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.shivjeet1.mynotes.Database.RoomDb;
import com.shivjeet1.mynotes.Model.Task;
import com.shivjeet1.mynotes.util.ReminderScheduler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Restores alarms after a reboot or an app update. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        PendingResult pendingResult = goAsync();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                RoomDb database = RoomDb.getInstance(appContext);
                List<Task> tasks = database.taskDao().getPendingReminders();
                for (Task task : tasks) {
                    ReminderScheduler.schedule(appContext, task);
                }
            } finally {
                pendingResult.finish();
                executor.shutdown();
            }
        });
    }
}
