package com.shivjeet1.mynotes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.shivjeet1.mynotes.Adapter.TaskListAdapter;
import com.shivjeet1.mynotes.Database.RoomDb;
import com.shivjeet1.mynotes.Model.Task;
import com.shivjeet1.mynotes.util.AppBarActions;
import com.shivjeet1.mynotes.util.ReminderScheduler;
import com.shivjeet1.mynotes.util.TaskDates;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class TasksFragment extends Fragment implements PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "TasksFragment";
    private static final String STATE_FILTER = "state_filter";
    private static final String STATE_SCROLL = "state_scroll";

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Task> allTasks = new ArrayList<>();
    private final List<Task> visibleTasks = new ArrayList<>();

    private final TaskListAdapter.TaskListener taskListener = new TaskListAdapter.TaskListener() {
        @Override
        public void onOpen(Task task) {
            openTask(task);
        }

        @Override
        public void onToggleDone(Task task) {
            toggleDone(task);
        }

        @Override
        public void onMore(Task task, View anchor) {
            showTaskMenu(task, anchor);
        }
    };

    private RecyclerView recyclerView;
    private TaskListAdapter adapter;
    private Context appContext;
    private RoomDb database;
    private MaterialToolbar toolbar;
    private ChipGroup filterChips;
    private FloatingActionButton fab;
    private View contentContainer;
    private View emptyState;
    private View loadingState;
    private View errorState;
    private ImageView emptyIcon;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private MaterialButton emptyAction;
    private View rootView;

    private Task selectedTask;
    private int filter = R.id.chipAll;
    private boolean isLoading = true;
    private boolean loadFailed;
    private boolean destroyed;
    private boolean resumedOnce;
    private Parcelable pendingScrollState;
    private ListState listState = ListState.LOADING;

    private enum ListState {
        LOADING, CONTENT, EMPTY, ERROR
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        destroyed = false;
        resumedOnce = false;
        rootView = view;
        appContext = requireContext().getApplicationContext();
        database = RoomDb.getInstance(appContext);

        toolbar = view.findViewById(R.id.toolbar);
        filterChips = view.findViewById(R.id.filterChips);
        recyclerView = view.findViewById(R.id.taskRv);
        fab = view.findViewById(R.id.addTaskBtn);
        contentContainer = view.findViewById(R.id.taskContentContainer);
        emptyState = view.findViewById(R.id.taskEmptyState);
        loadingState = view.findViewById(R.id.taskLoadingState);
        errorState = view.findViewById(R.id.taskErrorState);
        emptyIcon = view.findViewById(R.id.taskEmptyIcon);
        emptyTitle = view.findViewById(R.id.taskEmptyTitle);
        emptyMessage = view.findViewById(R.id.taskEmptyMessage);
        emptyAction = view.findViewById(R.id.taskEmptyAction);

        if (savedInstanceState != null) {
            filter = savedInstanceState.getInt(STATE_FILTER, R.id.chipAll);
            pendingScrollState = savedInstanceState.getParcelable(STATE_SCROLL);
        }
        filterChips.check(filter);

        ViewCompat.setAccessibilityHeading(emptyTitle, true);
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.taskErrorTitle), true);
        TooltipCompat.setTooltipText(fab, getString(R.string.add_task));

        AppBarActions.bind(requireContext(), toolbar, this::openSettings);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new TaskListAdapter(requireContext(), taskListener);
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(clicked -> openNewTask());
        emptyAction.setOnClickListener(clicked -> openNewTask());
        view.findViewById(R.id.taskRetryButton)
                .setOnClickListener(clicked -> loadTasks(true, false));
        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            if (checked != filter) {
                filter = checked;
                applyFilter(true);
            }
        });

        updateUiState();
        loadTasks(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isAdded()) {
            return;
        }
        AppBarActions.updateThemeAction(requireContext(), toolbar);
        if (resumedOnce) {
            loadTasks(false, false);
        }
        resumedOnce = true;
    }

    private void openSettings() {
        startActivity(new Intent(requireContext(), SettingsActivity.class));
    }

    private void openNewTask() {
        startActivity(new Intent(requireContext(), TaskEditorActivity.class));
    }

    private void openTask(Task task) {
        Intent intent = new Intent(requireContext(), TaskEditorActivity.class);
        intent.putExtra(TaskEditorActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }

    private void loadTasks(boolean showLoading, boolean scrollToTop) {
        if (showLoading) {
            isLoading = true;
            loadFailed = false;
            updateUiState();
        }
        try {
            dbExecutor.execute(() -> {
                try {
                    List<Task> loaded = database.taskDao().getAll();
                    mainHandler.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        isLoading = false;
                        loadFailed = false;
                        allTasks.clear();
                        allTasks.addAll(loaded);
                        applyFilter(scrollToTop);
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not load tasks", error);
                    mainHandler.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        isLoading = false;
                        loadFailed = true;
                        updateUiState();
                    });
                }
            });
        } catch (RejectedExecutionException ignored) {
            // The fragment is being torn down.
        }
    }

    private void applyFilter(boolean scrollToTop) {
        visibleTasks.clear();
        long endOfToday = TaskDates.endOfToday();
        long startOfTomorrow = TaskDates.startOfTomorrow();
        for (Task task : allTasks) {
            if (filter == R.id.chipDone) {
                if (task.isDone()) {
                    visibleTasks.add(task);
                }
            } else if (filter == R.id.chipToday) {
                if (!task.isDone() && task.getDueAt() > 0 && task.getDueAt() <= endOfToday) {
                    visibleTasks.add(task);
                }
            } else if (filter == R.id.chipUpcoming) {
                if (!task.isDone() && task.getDueAt() >= startOfTomorrow) {
                    visibleTasks.add(task);
                }
            } else {
                visibleTasks.add(task);
            }
        }

        final Parcelable scrollState = pendingScrollState;
        pendingScrollState = null;
        updateUiState();
        adapter.submitList(new ArrayList<>(visibleTasks), () -> {
            if (destroyed) {
                return;
            }
            if (scrollState != null) {
                recyclerView.getLayoutManager().onRestoreInstanceState(scrollState);
            } else if (scrollToTop) {
                recyclerView.scrollToPosition(0);
            }
        });
    }

    private void updateUiState() {
        ListState state;
        if (isLoading) {
            state = ListState.LOADING;
        } else if (loadFailed) {
            state = ListState.ERROR;
        } else if (visibleTasks.isEmpty()) {
            state = ListState.EMPTY;
        } else {
            state = ListState.CONTENT;
        }

        if (isLoading || loadFailed) {
            toolbar.setSubtitle(null);
        } else if (filter == R.id.chipAll) {
            int total = allTasks.size();
            toolbar.setSubtitle(getResources().getQuantityString(
                    R.plurals.task_count, total, total));
        } else {
            int total = allTasks.size();
            toolbar.setSubtitle(getResources().getQuantityString(
                    R.plurals.task_count_filtered, total, visibleTasks.size(), total));
        }

        if (state == ListState.EMPTY) {
            configureEmptyState();
        }

        if (state != listState) {
            listState = state;
        }
        recyclerView.setVisibility(state == ListState.CONTENT ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(state == ListState.EMPTY ? View.VISIBLE : View.GONE);
        loadingState.setVisibility(state == ListState.LOADING ? View.VISIBLE : View.GONE);
        errorState.setVisibility(state == ListState.ERROR ? View.VISIBLE : View.GONE);
    }

    private void configureEmptyState() {
        int title;
        int message;
        int icon;
        if (filter == R.id.chipToday) {
            title = R.string.today_empty_title;
            message = R.string.today_empty_message;
            icon = R.drawable.ic_calendar_today_24;
        } else if (filter == R.id.chipUpcoming) {
            title = R.string.upcoming_empty_title;
            message = R.string.upcoming_empty_message;
            icon = R.drawable.ic_calendar_today_24;
        } else if (filter == R.id.chipDone) {
            title = R.string.done_empty_title;
            message = R.string.done_empty_message;
            icon = R.drawable.ic_check_circle_24;
        } else {
            title = R.string.tasks_empty_title;
            message = R.string.tasks_empty_message;
            icon = R.drawable.ic_checklist_24;
        }
        emptyIcon.setImageResource(icon);
        emptyTitle.setText(title);
        emptyMessage.setText(message);
        emptyAction.setVisibility(filter == R.id.chipAll ? View.VISIBLE : View.GONE);
        emptyAction.setText(R.string.create_first_task);
    }

    private void toggleDone(Task task) {
        boolean newDone = !task.isDone();
        long completedAt = newDone ? System.currentTimeMillis() : 0L;
        Task updated = copyOf(task);
        updated.setDone(newDone);
        updated.setCompletedAt(completedAt);
        try {
            dbExecutor.execute(() -> {
                try {
                    database.taskDao().setDone(task.getId(), newDone, completedAt);
                    if (newDone) {
                        ReminderScheduler.cancel(appContext, task.getId());
                    } else if (task.getRemindAt() > System.currentTimeMillis()
                            && !task.isReminderFired()) {
                        ReminderScheduler.schedule(appContext, updated);
                    }
                    mainHandler.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        loadTasks(false, false);
                        announce(newDone ? R.string.task_completed : R.string.task_reopened);
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not update task", error);
                    mainHandler.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        loadTasks(false, false);
                        showMessage(R.string.task_complete_failed);
                    });
                }
            });
        } catch (RejectedExecutionException ignored) {
            // The fragment is being torn down.
        }
    }

    private void showTaskMenu(Task task, View anchor) {
        selectedTask = task;
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.task_popup);

        MenuItem deleteItem = popupMenu.getMenu().findItem(R.id.delete);
        SpannableString deleteTitle = new SpannableString(getString(R.string.delete_task));
        deleteTitle.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.danger)),
                0, deleteTitle.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        deleteItem.setTitle(deleteTitle);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (selectedTask == null) {
            return false;
        }
        if (item.getItemId() == R.id.edit) {
            openTask(selectedTask);
            return true;
        }
        if (item.getItemId() == R.id.delete) {
            deleteTask(selectedTask);
            return true;
        }
        return false;
    }

    private void deleteTask(Task task) {
        ReminderScheduler.cancel(appContext, task.getId());
        allTasks.removeIf(existing -> existing.getId() == task.getId());
        applyFilter(false);

        try {
            dbExecutor.execute(() -> {
                try {
                    database.taskDao().delete(task);
                    mainHandler.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        loadTasks(false, false);
                        showDeleteUndo(task);
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not delete task", error);
                    mainHandler.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        loadTasks(false, false);
                        showMessage(R.string.delete_task_failed);
                    });
                }
            });
        } catch (RejectedExecutionException ignored) {
            // The fragment is being torn down.
        }
    }

    private void showDeleteUndo(Task task) {
        Snackbar.make(rootView, R.string.task_deleted, Snackbar.LENGTH_LONG)
                .setAnchorView(fab)
                .setAction(R.string.undo, clicked -> restoreTask(task))
                .show();
    }

    private void restoreTask(Task task) {
        Task restored = copyOf(task);
        restored.setId(0);
        try {
            dbExecutor.execute(() -> {
                try {
                    long newId = database.taskDao().insert(restored);
                    restored.setId((int) newId);
                    if (!restored.isDone() && restored.getRemindAt() > System.currentTimeMillis()
                            && !restored.isReminderFired()) {
                        ReminderScheduler.schedule(appContext, restored);
                    }
                    mainHandler.post(() -> {
                        if (!destroyed) {
                            loadTasks(false, false);
                        }
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not restore task", error);
                    mainHandler.post(() -> {
                        if (!destroyed) {
                            loadTasks(false, false);
                        }
                    });
                }
            });
        } catch (RejectedExecutionException ignored) {
            // The fragment is being torn down.
        }
    }

    private Task copyOf(Task task) {
        Task copy = new Task();
        copy.setId(task.getId());
        copy.setTitle(task.getTitle());
        copy.setNotes(task.getNotes());
        copy.setDueAt(task.getDueAt());
        copy.setRemindAt(task.getRemindAt());
        copy.setReminderFired(task.isReminderFired());
        copy.setDone(task.isDone());
        copy.setCreatedAt(task.getCreatedAt());
        copy.setCompletedAt(task.getCompletedAt());
        return copy;
    }

    private void announce(int messageRes) {
        Snackbar bar = Snackbar.make(rootView, messageRes, Snackbar.LENGTH_SHORT);
        bar.setAnchorView(fab);
        bar.show();
    }

    private void showMessage(int messageRes) {
        Snackbar bar = Snackbar.make(rootView, messageRes, Snackbar.LENGTH_LONG);
        bar.setAnchorView(fab);
        bar.show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_FILTER, filter);
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager != null) {
            outState.putParcelable(STATE_SCROLL, layoutManager.onSaveInstanceState());
        }
    }

    @Override
    public void onDestroyView() {
        destroyed = true;
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        dbExecutor.shutdown();
        super.onDestroy();
    }
}
