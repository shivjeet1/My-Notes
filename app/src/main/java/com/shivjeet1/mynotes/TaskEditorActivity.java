package com.shivjeet1.mynotes;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.shivjeet1.mynotes.Database.RoomDb;
import com.shivjeet1.mynotes.Model.Task;
import com.shivjeet1.mynotes.util.AppPreferences;
import com.shivjeet1.mynotes.util.DirtyStateController;
import com.shivjeet1.mynotes.util.ReminderScheduler;
import com.shivjeet1.mynotes.util.TaskDates;
import com.shivjeet1.mynotes.util.WindowInsetsHelper;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class TaskEditorActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";

    private static final String TAG = "TaskEditorActivity";

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Context appContext;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> updateReminderUi());

    private MaterialToolbar toolbar;
    private EditText titleEdt;
    private EditText notesEdt;
    private TextView titleError;
    private TextView dueDateValue;
    private TextView dueTimeValue;
    private TextView reminderHint;
    private TextView reminderError;
    private TextView notificationWarning;
    private TextView exactAlarmWarning;
    private View dueDateRow;
    private View dueTimeRow;
    private ImageButton clearDueBtn;
    private ChipGroup reminderChips;
    private MaterialSwitch doneSwitch;
    private MaterialButton openNotificationSettings;
    private View loadingOverlay;
    private MenuItem saveItem;
    private MenuItem deleteItem;

    private RoomDb database;
    private Task task = new Task();
    private boolean editing;
    private boolean loaded;
    private boolean saving;
    private boolean updatingChips;
    private boolean notificationPermissionRequested;

    private long dueAt;
    private long remindAt;
    private int reminderOffset = -1;

    private String originalTitle = "";
    private String originalNotes = "";
    private long originalDueAt;
    private long originalRemindAt;
    private boolean originalDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_editor);
        appContext = getApplicationContext();
        WindowInsetsHelper.apply(this, findViewById(R.id.editorRoot));

        database = RoomDb.getInstance(this);

        toolbar = findViewById(R.id.toolbar);
        titleEdt = findViewById(R.id.taskTitleEdt);
        notesEdt = findViewById(R.id.taskNotesEdt);
        titleError = findViewById(R.id.taskTitleError);
        dueDateValue = findViewById(R.id.dueDateValue);
        dueTimeValue = findViewById(R.id.dueTimeValue);
        reminderHint = findViewById(R.id.reminderHint);
        reminderError = findViewById(R.id.reminderError);
        notificationWarning = findViewById(R.id.notificationWarning);
        exactAlarmWarning = findViewById(R.id.exactAlarmWarning);
        dueDateRow = findViewById(R.id.dueDateRow);
        dueTimeRow = findViewById(R.id.dueTimeRow);
        clearDueBtn = findViewById(R.id.clearDueBtn);
        reminderChips = findViewById(R.id.reminderChips);
        doneSwitch = findViewById(R.id.doneSwitch);
        openNotificationSettings = findViewById(R.id.openNotificationSettings);
        loadingOverlay = findViewById(R.id.editorLoading);

        ViewCompat.setAccessibilityHeading(findViewById(R.id.scheduleLabel), true);
        ViewCompat.setAccessibilityHeading(findViewById(R.id.reminderLabel), true);

        toolbar.inflateMenu(R.menu.menu_task_editor);
        saveItem = toolbar.getMenu().findItem(R.id.action_save);
        deleteItem = toolbar.getMenu().findItem(R.id.action_delete);
        deleteItem.setVisible(false);
        toolbar.setNavigationOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                saveTask();
                return true;
            }
            if (item.getItemId() == R.id.action_delete) {
                confirmDelete();
                return true;
            }
            return false;
        });

        titleEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                titleError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                updateSaveState();
            }
        });
        notesEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                updateSaveState();
            }
        });

        dueDateRow.setOnClickListener(view -> showDatePicker());
        dueTimeRow.setOnClickListener(view -> showTimePicker());
        clearDueBtn.setOnClickListener(view -> clearDueDate());
        reminderChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (updatingChips || checkedIds.isEmpty()) {
                return;
            }
            onReminderChipChanged(checkedIds.get(0));
        });
        doneSwitch.setOnCheckedChangeListener((button, checked) -> {
            updateReminderUi();
            updateSaveState();
        });
        openNotificationSettings.setOnClickListener(view -> openNotificationSettings());

        task.setCreatedAt(System.currentTimeMillis());
        int taskId = getIntent().getIntExtra(EXTRA_TASK_ID, 0);
        editing = taskId > 0;
        if (editing) {
            toolbar.setTitle(R.string.edit_task);
            loaded = false;
            loadingOverlay.setVisibility(View.VISIBLE);
            loadTask(taskId);
        } else {
            toolbar.setTitle(R.string.new_task);
            loaded = true;
            loadingOverlay.setVisibility(View.GONE);
            selectReminderChip(-1);
            updateScheduleUi();
            updateReminderUi();
            updateSaveState();
            titleEdt.requestFocus();
        }

        new DirtyStateController(this, R.string.discard_title, R.string.discard_task_message,
                this::hasUnsavedChanges);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loaded) {
            updateReminderUi();
        }
    }

    private void loadTask(int taskId) {
        try {
            dbExecutor.execute(() -> {
                Task loadedTask = database.taskDao().getById(taskId);
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (loadedTask == null) {
                        Toast.makeText(this, R.string.task_load_failed, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    task = loadedTask;
                    dueAt = loadedTask.getDueAt();
                    remindAt = loadedTask.getRemindAt();
                    reminderOffset = offsetFromReminder(dueAt, remindAt);
                    loaded = true;
                    titleEdt.setText(loadedTask.getTitle());
                    notesEdt.setText(loadedTask.getNotes());
                    doneSwitch.setChecked(loadedTask.isDone());
                    selectReminderChip(reminderOffset);
                    toolbar.setTitle(R.string.edit_task);
                    deleteItem.setVisible(true);
                    loadingOverlay.setVisibility(View.GONE);
                    captureOriginal();
                    updateScheduleUi();
                    updateReminderUi();
                    updateSaveState();
                });
            });
        } catch (RejectedExecutionException error) {
            finish();
        }
    }

    private int offsetFromReminder(long due, long reminder) {
        if (due <= 0 || reminder <= 0 || reminder > due) {
            return -1;
        }
        long minutes = Math.round((due - reminder) / 60000.0);
        int[] supported = {0, 5, 15, 30, 60, 1440};
        for (int candidate : supported) {
            if (candidate == minutes) {
                return candidate;
            }
        }
        return -1;
    }

    private void captureOriginal() {
        originalTitle = titleEdt.getText().toString().trim();
        originalNotes = notesEdt.getText().toString().trim();
        originalDueAt = dueAt;
        originalRemindAt = remindAt;
        originalDone = doneSwitch.isChecked();
    }

    private boolean hasUnsavedChanges() {
        if (!loaded) {
            return false;
        }
        return !originalTitle.equals(titleEdt.getText().toString().trim())
                || !originalNotes.equals(notesEdt.getText().toString().trim())
                || originalDueAt != dueAt
                || originalRemindAt != remindAt
                || originalDone != doneSwitch.isChecked();
    }

    private void updateSaveState() {
        if (saveItem != null) {
            saveItem.setEnabled(loaded && !saving
                    && !titleEdt.getText().toString().trim().isEmpty());
        }
    }

    private void showDatePicker() {
        long selection = dueAt > 0
                ? toUtcDate(dueAt)
                : MaterialDatePicker.todayInUtcMilliseconds();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.due_date_picker_title)
                .setSelection(selection)
                .build();
        picker.addOnPositiveButtonClickListener(this::applyPickedDate);
        picker.show(getSupportFragmentManager(), "due-date-picker");
    }

    private void applyPickedDate(long utcMillis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMillis);
        Calendar local = Calendar.getInstance();
        if (dueAt > 0) {
            local.setTimeInMillis(dueAt);
        } else {
            Calendar today = Calendar.getInstance();
            boolean selectedToday = utc.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && utc.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
            if (selectedToday) {
                local.setTimeInMillis(today.getTimeInMillis());
                local.add(Calendar.HOUR_OF_DAY, 1);
                local.set(Calendar.MINUTE, 0);
                if (!TaskDates.isSameDay(local.getTimeInMillis(), today.getTimeInMillis())) {
                    local.setTimeInMillis(today.getTimeInMillis());
                    local.set(Calendar.HOUR_OF_DAY, 23);
                    local.set(Calendar.MINUTE, 59);
                }
            } else {
                local.setTimeInMillis(today.getTimeInMillis());
                local.set(Calendar.HOUR_OF_DAY, 9);
                local.set(Calendar.MINUTE, 0);
            }
        }
        local.set(Calendar.YEAR, utc.get(Calendar.YEAR));
        local.set(Calendar.MONTH, utc.get(Calendar.MONTH));
        local.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
        local.set(Calendar.SECOND, 0);
        local.set(Calendar.MILLISECOND, 0);
        boolean wasEmpty = dueAt <= 0;
        dueAt = local.getTimeInMillis();

        if (wasEmpty && !editing && reminderOffset < 0
                && AppPreferences.isDefaultReminderEnabled(this)) {
            reminderOffset = AppPreferences.getReminderOffsetMinutes(this);
            selectReminderChip(reminderOffset);
            requestNotificationsIfNeeded();
        }
        recomputeReminder();
        updateScheduleUi();
        updateReminderUi();
        updateSaveState();
    }

    private void showTimePicker() {
        if (dueAt <= 0) {
            return;
        }
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(dueAt);
        int format = DateFormat.is24HourFormat(this)
                ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTitleText(R.string.due_time_picker_title)
                .setTimeFormat(format)
                .setHour(local.get(Calendar.HOUR_OF_DAY))
                .setMinute(local.get(Calendar.MINUTE))
                .build();
        picker.addOnPositiveButtonClickListener(view -> {
            Calendar updated = Calendar.getInstance();
            updated.setTimeInMillis(dueAt);
            updated.set(Calendar.HOUR_OF_DAY, picker.getHour());
            updated.set(Calendar.MINUTE, picker.getMinute());
            updated.set(Calendar.SECOND, 0);
            updated.set(Calendar.MILLISECOND, 0);
            dueAt = updated.getTimeInMillis();
            recomputeReminder();
            updateScheduleUi();
            updateReminderUi();
            updateSaveState();
        });
        picker.show(getSupportFragmentManager(), "due-time-picker");
    }

    private long toUtcDate(long localMillis) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(localMillis);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        return utc.getTimeInMillis();
    }

    private void clearDueDate() {
        dueAt = 0;
        remindAt = 0;
        reminderOffset = -1;
        selectReminderChip(-1);
        updateScheduleUi();
        updateReminderUi();
        updateSaveState();
    }

    private void onReminderChipChanged(int checkedId) {
        int offset = offsetForChip(checkedId);
        if (dueAt <= 0 && offset >= 0) {
            selectReminderChip(-1);
            return;
        }
        reminderOffset = offset;
        recomputeReminder();
        if (offset >= 0) {
            requestNotificationsIfNeeded();
        }
        updateReminderUi();
        updateSaveState();
    }

    private void recomputeReminder() {
        if (dueAt <= 0 || reminderOffset < 0) {
            remindAt = 0;
        } else {
            remindAt = dueAt - reminderOffset * 60000L;
        }
    }

    private void selectReminderChip(int offset) {
        updatingChips = true;
        reminderChips.check(chipForOffset(offset));
        updatingChips = false;
    }

    private int chipForOffset(int offset) {
        switch (offset) {
            case 0:
                return R.id.reminderChipAtTime;
            case 5:
                return R.id.reminderChip5;
            case 15:
                return R.id.reminderChip15;
            case 30:
                return R.id.reminderChip30;
            case 60:
                return R.id.reminderChip60;
            case 1440:
                return R.id.reminderChip1440;
            default:
                return R.id.reminderChipNone;
        }
    }

    private int offsetForChip(int chipId) {
        if (chipId == R.id.reminderChipAtTime) {
            return 0;
        }
        if (chipId == R.id.reminderChip5) {
            return 5;
        }
        if (chipId == R.id.reminderChip15) {
            return 15;
        }
        if (chipId == R.id.reminderChip30) {
            return 30;
        }
        if (chipId == R.id.reminderChip60) {
            return 60;
        }
        if (chipId == R.id.reminderChip1440) {
            return 1440;
        }
        return -1;
    }

    private void updateScheduleUi() {
        boolean hasDate = dueAt > 0;
        dueDateValue.setText(hasDate
                ? TaskDates.formatDate(this, dueAt) : getString(R.string.add_due_date));
        dueTimeValue.setText(hasDate
                ? TaskDates.formatTime(this, dueAt) : getString(R.string.set_date_first));
        clearDueBtn.setVisibility(hasDate ? View.VISIBLE : View.GONE);
        dueTimeRow.setEnabled(hasDate);
        dueTimeRow.setAlpha(hasDate ? 1f : 0.5f);

        for (int index = 0; index < reminderChips.getChildCount(); index++) {
            View child = reminderChips.getChildAt(index);
            child.setEnabled(hasDate);
        }
        reminderChips.setAlpha(hasDate ? 1f : 0.5f);
    }

    private void updateReminderUi() {
        boolean hasDate = dueAt > 0;
        boolean done = doneSwitch.isChecked();
        boolean hasReminder = hasDate && reminderOffset >= 0;

        if (!hasDate) {
            reminderHint.setText(R.string.reminder_hint_no_date);
        } else if (!hasReminder) {
            reminderHint.setText(R.string.reminder_hint_off);
        } else if (done) {
            reminderHint.setText(R.string.reminder_hint_done);
        } else {
            reminderHint.setText(getString(R.string.reminder_hint,
                    TaskDates.formatDateTime(this, remindAt)));
        }

        boolean reminderPast = hasReminder && !done && remindAt <= System.currentTimeMillis();
        reminderError.setVisibility(reminderPast ? View.VISIBLE : View.GONE);

        boolean notificationsBlocked = hasReminder && !done && !notificationsAllowed();
        notificationWarning.setVisibility(notificationsBlocked ? View.VISIBLE : View.GONE);
        openNotificationSettings.setVisibility(notificationsBlocked ? View.VISIBLE : View.GONE);

        boolean exactBlocked = hasReminder && !done && !canScheduleExactAlarms();
        exactAlarmWarning.setVisibility(exactBlocked ? View.VISIBLE : View.GONE);

        updateSaveState();
    }

    private boolean notificationsAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(this).areNotificationsEnabled();
    }

    private boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        return manager != null && manager.canScheduleExactAlarms();
    }

    private void requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || notificationsAllowed() || notificationPermissionRequested) {
            return;
        }
        notificationPermissionRequested = true;
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
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

    private void saveTask() {
        if (!loaded || saving) {
            return;
        }
        String title = titleEdt.getText().toString().trim();
        if (title.isEmpty()) {
            titleError.setVisibility(View.VISIBLE);
            titleEdt.requestFocus();
            return;
        }
        boolean done = doneSwitch.isChecked();
        long computedRemindAt = reminderOffset >= 0 ? dueAt - reminderOffset * 60000L : 0L;
        if (!done && reminderOffset >= 0
                && computedRemindAt <= System.currentTimeMillis()) {
            reminderError.setVisibility(View.VISIBLE);
            return;
        }

        Task toSave;
        if (editing) {
            toSave = copyOf(task);
        } else {
            toSave = new Task();
            toSave.setCreatedAt(System.currentTimeMillis());
        }
        toSave.setTitle(title);
        toSave.setNotes(notesEdt.getText().toString().trim());
        toSave.setDueAt(dueAt);
        toSave.setRemindAt(computedRemindAt);
        if (computedRemindAt != task.getRemindAt()) {
            toSave.setReminderFired(false);
        }
        if (done) {
            if (task.isDone() && task.getCompletedAt() > 0) {
                toSave.setCompletedAt(task.getCompletedAt());
            } else {
                toSave.setCompletedAt(System.currentTimeMillis());
            }
        } else {
            toSave.setCompletedAt(0L);
        }
        toSave.setDone(done);

        saving = true;
        updateSaveState();
        try {
            dbExecutor.execute(() -> {
                try {
                    if (editing) {
                        database.taskDao().update(toSave);
                    } else {
                        long id = database.taskDao().insert(toSave);
                        toSave.setId((int) id);
                    }
                    ReminderScheduler.schedule(appContext, toSave);
                    mainHandler.post(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            finish();
                        }
                    });
                } catch (Exception error) {
                    mainHandler.post(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        saving = false;
                        updateSaveState();
                        Snackbar.make(findViewById(R.id.editorRoot),
                                R.string.save_task_failed, Snackbar.LENGTH_LONG).show();
                    });
                }
            });
        } catch (RejectedExecutionException error) {
            saving = false;
            updateSaveState();
        }
    }

    private void confirmDelete() {
        if (!editing || saving) {
            return;
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_task_title)
                .setMessage(R.string.delete_task_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_task, (dismissed, which) -> deleteTask())
                .create();
        dialog.setOnShowListener(ignored -> {
            Button delete = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (delete != null) {
                delete.setTextColor(ContextCompat.getColor(this, R.color.danger));
            }
        });
        dialog.show();
    }

    private void deleteTask() {
        if (!loaded || saving) {
            return;
        }
        saving = true;
        updateSaveState();
        ReminderScheduler.cancel(appContext, task.getId());
        try {
            dbExecutor.execute(() -> {
                try {
                    database.taskDao().delete(task);
                    mainHandler.post(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            finish();
                        }
                    });
                } catch (Exception error) {
                    mainHandler.post(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        saving = false;
                        updateSaveState();
                        Snackbar.make(findViewById(R.id.editorRoot),
                                R.string.delete_task_failed, Snackbar.LENGTH_LONG).show();
                    });
                }
            });
        } catch (RejectedExecutionException error) {
            saving = false;
            updateSaveState();
        }
    }

    private Task copyOf(Task source) {
        Task copy = new Task();
        copy.setId(source.getId());
        copy.setTitle(source.getTitle());
        copy.setNotes(source.getNotes());
        copy.setDueAt(source.getDueAt());
        copy.setRemindAt(source.getRemindAt());
        copy.setReminderFired(source.isReminderFired());
        copy.setDone(source.isDone());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setCompletedAt(source.getCompletedAt());
        return copy;
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        dbExecutor.shutdown();
        super.onDestroy();
    }
}
