package com.shivjeet1.mynotes;

import android.Manifest;
import android.app.AlarmManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.NotificationManagerCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.shivjeet1.mynotes.Database.RoomDb;
import com.shivjeet1.mynotes.Model.Notes;
import com.shivjeet1.mynotes.Model.Task;
import com.shivjeet1.mynotes.util.AppPreferences;
import com.shivjeet1.mynotes.util.BackupManager;
import com.shivjeet1.mynotes.util.ReminderScheduler;
import com.shivjeet1.mynotes.util.WindowInsetsHelper;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> createBackupLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("application/json"),
                    uri -> {
                        if (uri != null) {
                            writeBackup(uri);
                        }
                    });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> updateReminderSummaries());

    private final ActivityResultLauncher<String[]> openBackupLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            readBackup(uri);
                        }
                    });

    private TextView themeSummary;
    private View backupRow;
    private View restoreRow;
    private View dataProgress;
    private TextView dataStatus;
    private TextView notificationsSummary;
    private TextView preciseSummary;
    private TextView defaultReminderSummary;
    private TextView notesCountValue;
    private TextView tasksCountValue;
    private View notificationsRow;
    private View preciseTimingRow;
    private View defaultReminderRow;
    private TextView notesAutosaveSummary;
    private MaterialSwitch notesAutosaveSwitch;
    private RoomDb database;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        WindowInsetsHelper.apply(this, findViewById(R.id.settingsRoot));

        database = RoomDb.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed());

        themeSummary = findViewById(R.id.themeSummary);
        backupRow = findViewById(R.id.backupRow);
        restoreRow = findViewById(R.id.restoreRow);
        dataProgress = findViewById(R.id.dataProgress);
        dataStatus = findViewById(R.id.dataStatus);
        notificationsSummary = findViewById(R.id.notificationsSummary);
        preciseSummary = findViewById(R.id.preciseSummary);
        defaultReminderSummary = findViewById(R.id.defaultReminderSummary);
        notesCountValue = findViewById(R.id.notesCountValue);
        tasksCountValue = findViewById(R.id.tasksCountValue);
        notificationsRow = findViewById(R.id.notificationsRow);
        preciseTimingRow = findViewById(R.id.preciseTimingRow);
        defaultReminderRow = findViewById(R.id.defaultReminderRow);
        notesAutosaveSummary = findViewById(R.id.notesAutosaveSummary);
        notesAutosaveSwitch = findViewById(R.id.notesAutosaveSwitch);

        findViewById(R.id.themeRow).setOnClickListener(view -> showThemeChooser());
        notificationsRow.setOnClickListener(view -> onNotificationsClicked());
        preciseTimingRow.setOnClickListener(view -> onPreciseTimingClicked());
        defaultReminderRow.setOnClickListener(view -> showDefaultReminderChooser());
        View notesSettingsRow = findViewById(R.id.notesSettingsRow);
        if (notesSettingsRow != null) {
            notesSettingsRow.setOnClickListener(view -> {
                if (notesAutosaveSwitch != null) {
                    notesAutosaveSwitch.toggle();
                }
            });
        }
        if (notesAutosaveSwitch != null) {
            notesAutosaveSwitch.setChecked(AppPreferences.isNoteAutosaveEnabled(this));
            notesAutosaveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                AppPreferences.setNoteAutosaveEnabled(this, isChecked);
                updateNotesAutosaveSummary();
            });
        }
        backupRow.setOnClickListener(view -> createBackupLauncher.launch(BackupManager.FILE_NAME));
        restoreRow.setOnClickListener(view -> openBackupLauncher.launch(
                new String[]{"application/json", "text/plain"}));

        View appearanceLabel = findViewById(R.id.appearanceLabel);
        View remindersLabel = findViewById(R.id.remindersLabel);
        View dataLabel = findViewById(R.id.dataLabel);
        View aboutLabel = findViewById(R.id.aboutLabel);
        ViewCompat.setAccessibilityHeading(appearanceLabel, true);
        ViewCompat.setAccessibilityHeading(remindersLabel, true);
        ViewCompat.setAccessibilityHeading(dataLabel, true);
        ViewCompat.setAccessibilityHeading(aboutLabel, true);

        ((TextView) findViewById(R.id.versionValue)).setText(readVersionName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateThemeSummary();
        updateReminderSummaries();
        updateNotesAutosaveSummary();
        if (notesAutosaveSwitch != null) {
            notesAutosaveSwitch.setChecked(AppPreferences.isNoteAutosaveEnabled(this));
        }
        loadCounts();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        ioExecutor.shutdown();
        super.onDestroy();
    }

    private void onNotificationsClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (AppPreferences.hasRequestedNotificationPermission(this)) {
                openNotificationSettings();
            } else {
                AppPreferences.setNotificationPermissionRequested(this);
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
            return;
        }
        openNotificationSettings();
    }

    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
        }
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            // Some OEMs do not expose the per-app notification screen.
        }
    }

    private void onPreciseTimingClicked() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Snackbar.make(findViewById(R.id.settingsRoot),
                    R.string.exact_alarms_unavailable, Snackbar.LENGTH_LONG).show();
            return;
        }
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (manager != null && manager.canScheduleExactAlarms()) {
            Snackbar.make(findViewById(R.id.settingsRoot),
                    R.string.exact_alarms_allowed_message, Snackbar.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            // Some OEMs do not expose the exact-alarm screen.
        }
    }

    private void updateReminderSummaries() {
        boolean notificationsAllowed = NotificationManagerCompat.from(this)
                .areNotificationsEnabled();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsAllowed = notificationsAllowed
                    && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        notificationsSummary.setText(notificationsAllowed
                ? R.string.notifications_allowed : R.string.notifications_blocked);

        boolean exactAllowed = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            exactAllowed = manager != null && manager.canScheduleExactAlarms();
        }
        preciseSummary.setText(exactAllowed
                ? R.string.precise_allowed : R.string.precise_unavailable);

        boolean defaultEnabled = AppPreferences.isDefaultReminderEnabled(this);
        if (defaultEnabled) {
            defaultReminderSummary.setText(defaultReminderLabel(
                    AppPreferences.getReminderOffsetMinutes(this)));
        } else {
            defaultReminderSummary.setText(R.string.default_reminder_none);
        }
    }

    private void showDefaultReminderChooser() {
        final int[] offsets = {-1, 0, 5, 15, 30, 60, 1440};
        final String[] labels = {
                getString(R.string.default_reminder_none),
                getString(R.string.default_reminder_at_time),
                getString(R.string.default_reminder_5),
                getString(R.string.default_reminder_15),
                getString(R.string.default_reminder_30),
                getString(R.string.default_reminder_1h),
                getString(R.string.default_reminder_1d)
        };
        int checked = 0;
        if (AppPreferences.isDefaultReminderEnabled(this)) {
            int current = AppPreferences.getReminderOffsetMinutes(this);
            for (int index = 1; index < offsets.length; index++) {
                if (offsets[index] == current) {
                    checked = index;
                    break;
                }
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.default_reminder)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) {
                        AppPreferences.setDefaultReminderEnabled(this, false);
                    } else {
                        AppPreferences.setDefaultReminderEnabled(this, true);
                        AppPreferences.setReminderOffsetMinutes(this, offsets[which]);
                    }
                    updateReminderSummaries();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private int defaultReminderLabel(int offset) {
        switch (offset) {
            case 0:
                return R.string.default_reminder_at_time;
            case 5:
                return R.string.default_reminder_5;
            case 15:
                return R.string.default_reminder_15;
            case 30:
                return R.string.default_reminder_30;
            case 60:
                return R.string.default_reminder_1h;
            case 1440:
                return R.string.default_reminder_1d;
            default:
                return R.string.default_reminder_none;
        }
    }

    private void loadCounts() {
        try {
            ioExecutor.execute(() -> {
                int notes = database.mainDAO().count();
                int tasks = database.taskDao().count();
                mainHandler.post(() -> {
                    if (!isActivityUsable()) {
                        return;
                    }
                    notesCountValue.setText(String.valueOf(notes));
                    tasksCountValue.setText(String.valueOf(tasks));
                });
            });
        } catch (RejectedExecutionException ignored) {
            // The activity is finishing.
        }
    }

    private void updateThemeSummary() {
        switch (AppPreferences.getThemeMode(this)) {
            case LIGHT:
                themeSummary.setText(R.string.theme_summary_light);
                break;
            case DARK:
                themeSummary.setText(R.string.theme_summary_dark);
                break;
            case SYSTEM:
            default:
                themeSummary.setText(R.string.theme_summary_system);
                break;
        }
    }

    private void updateNotesAutosaveSummary() {
        if (notesAutosaveSummary == null) {
            return;
        }
        if (AppPreferences.isNoteAutosaveEnabled(this)) {
            notesAutosaveSummary.setText(R.string.note_autosave_on);
        } else {
            notesAutosaveSummary.setText(R.string.note_autosave_off);
        }
    }

    private void showThemeChooser() {
        final AppPreferences.ThemeMode[] modes = {
                AppPreferences.ThemeMode.SYSTEM,
                AppPreferences.ThemeMode.LIGHT,
                AppPreferences.ThemeMode.DARK
        };
        final String[] labels = {
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };

        AppPreferences.ThemeMode current = AppPreferences.getThemeMode(this);
        int checked = 0;
        for (int index = 0; index < modes.length; index++) {
            if (modes[index] == current) {
                checked = index;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.theme)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    applyTheme(modes[which]);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyTheme(AppPreferences.ThemeMode mode) {
        boolean changed = mode != AppPreferences.getThemeMode(this);
        AppPreferences.setThemeMode(this, mode);
        if (changed) {
            AppCompatDelegate.setDefaultNightMode(AppPreferences.toNightMode(mode));
        } else {
            updateThemeSummary();
        }
    }

    private void writeBackup(Uri uri) {
        if (busy) {
            return;
        }
        setBusy(true, R.string.backup_in_progress);
        try {
            ioExecutor.execute(() -> {
                try {
                    List<Notes> notes = database.mainDAO().getAll();
                    List<Task> tasks = database.taskDao().getAll();
                    OutputStream rawOutput = getContentResolver().openOutputStream(uri, "wt");
                    if (rawOutput == null) {
                        throw new IllegalStateException("The document provider returned no stream");
                    }
                    try (OutputStream output = rawOutput) {
                        BackupManager.write(output, notes, tasks);
                    }
                    mainHandler.post(() -> {
                        if (!isActivityUsable()) {
                            return;
                        }
                        setBusy(false, 0);
                        showMessage(getString(R.string.backup_saved));
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not write backup", error);
                    mainHandler.post(() -> {
                        if (!isActivityUsable()) {
                            return;
                        }
                        setBusy(false, 0);
                        Snackbar.make(findViewById(R.id.settingsRoot),
                                        R.string.backup_failed, Snackbar.LENGTH_LONG)
                                .setAction(R.string.retry, view -> writeBackup(uri))
                                .show();
                    });
                }
            });
        } catch (RejectedExecutionException error) {
            setBusy(false, 0);
        }
    }

    private void readBackup(Uri uri) {
        if (busy) {
            return;
        }
        setBusy(true, R.string.restore_reading);
        try {
            ioExecutor.execute(() -> {
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null) {
                        throw new IllegalStateException("The document provider returned no stream");
                    }
                    BackupManager.BackupData data = BackupManager.read(input);
                    mainHandler.post(() -> {
                        if (!isActivityUsable()) {
                            return;
                        }
                        setBusy(false, 0);
                        showRestoreDialog(data);
                    });
                } catch (BackupManager.BackupException error) {
                    int message = messageFor(error.getFailure());
                    mainHandler.post(() -> {
                        if (!isActivityUsable()) {
                            return;
                        }
                        setBusy(false, 0);
                        showErrorDialog(message);
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not read backup", error);
                    mainHandler.post(() -> {
                        if (!isActivityUsable()) {
                            return;
                        }
                        setBusy(false, 0);
                        showErrorDialog(R.string.backup_io_error);
                    });
                }
            });
        } catch (RejectedExecutionException error) {
            setBusy(false, 0);
        }
    }

    private void showRestoreDialog(BackupManager.BackupData data) {
        String counts = describeCounts(data.getNotes().size(), data.getTasks().size());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_title)
                .setMessage(getString(R.string.restore_message, counts))
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.merge, (dismissed, which) -> restore(data, false))
                .setPositiveButton(R.string.replace, (dismissed, which) -> restore(data, true))
                .create();
        dialog.setOnShowListener(ignored -> {
            Button replace = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (replace != null) {
                replace.setTextColor(ContextCompat.getColor(this, R.color.danger));
            }
        });
        dialog.show();
    }

    private void restore(BackupManager.BackupData data, boolean replace) {
        if (busy) {
            return;
        }
        setBusy(true, R.string.restore_in_progress);
        try {
            ioExecutor.execute(() -> {
                try {
                    List<Task> previousTasks = replace ? database.taskDao().getAll() : null;
                    database.runInTransaction(() -> {
                        if (replace) {
                            database.mainDAO().deleteAll();
                            database.taskDao().deleteAll();
                        }
                        for (Notes note : data.getNotes()) {
                            if (!replace) {
                                note.setID(0);
                            }
                            database.mainDAO().insert(note);
                        }
                        for (Task task : data.getTasks()) {
                            if (!replace) {
                                task.setId(0);
                            }
                            database.taskDao().insert(task);
                        }
                    });
                    if (previousTasks != null) {
                        for (Task oldTask : previousTasks) {
                            ReminderScheduler.cancel(getApplicationContext(), oldTask.getId());
                        }
                    }
                    for (Task pending : database.taskDao().getPendingReminders()) {
                        ReminderScheduler.schedule(getApplicationContext(), pending);
                    }
                    int noteCount = data.getNotes().size();
                    int taskCount = data.getTasks().size();
                    mainHandler.post(() -> {
                        if (!isActivityUsable()) {
                            return;
                        }
                        setResult(RESULT_OK);
                        setBusy(false, 0);
                        loadCounts();
                        showMessage(getString(R.string.restore_success,
                                describeCounts(noteCount, taskCount)));
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not restore backup", error);
                    mainHandler.post(() -> {
                        if (!isActivityUsable()) {
                            return;
                        }
                        setBusy(false, 0);
                        showMessage(getString(R.string.restore_failed));
                    });
                }
            });
        } catch (RejectedExecutionException error) {
            setBusy(false, 0);
        }
    }

    private void setBusy(boolean isBusy, int statusRes) {
        busy = isBusy;
        backupRow.setEnabled(!isBusy);
        restoreRow.setEnabled(!isBusy);
        backupRow.setAlpha(isBusy ? 0.5f : 1f);
        restoreRow.setAlpha(isBusy ? 0.5f : 1f);
        dataProgress.setVisibility(isBusy ? View.VISIBLE : View.GONE);
        dataStatus.setVisibility(isBusy ? View.VISIBLE : View.GONE);
        if (isBusy && statusRes != 0) {
            dataStatus.setText(statusRes);
        }
    }

    private int messageFor(BackupManager.Failure failure) {
        switch (failure) {
            case UNSUPPORTED_VERSION:
                return R.string.backup_unsupported_version;
            case MALFORMED:
                return R.string.backup_malformed;
            case IO:
                return R.string.backup_io_error;
            case INVALID_FORMAT:
            default:
                return R.string.backup_invalid_format;
        }
    }

    private void showErrorDialog(int messageRes) {
        new MaterialAlertDialogBuilder(this)
                .setMessage(messageRes)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.settingsRoot), message, Snackbar.LENGTH_LONG).show();
    }

    private String describeCounts(int notes, int tasks) {
        String noteText = getResources().getQuantityString(R.plurals.note_count, notes, notes);
        String taskText = getResources().getQuantityString(R.plurals.task_count, tasks, tasks);
        return noteText + " · " + taskText;
    }

    private boolean isActivityUsable() {
        return !isFinishing() && !isDestroyed();
    }

    private String readVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (PackageManager.NameNotFoundException error) {
            return "";
        }
    }
}
