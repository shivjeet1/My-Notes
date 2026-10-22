package com.shivjeet1.mynotes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.shivjeet1.mynotes.Adapter.NoteListAdapter;
import com.shivjeet1.mynotes.Database.RoomDb;
import com.shivjeet1.mynotes.Interface.NotesClickListener;
import com.shivjeet1.mynotes.Model.Notes;
import com.shivjeet1.mynotes.util.AppBarActions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class NotesFragment extends Fragment implements PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "NotesFragment";
    private static final String STATE_QUERY = "state_query";
    private static final String STATE_SCROLL = "state_scroll";

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final List<Notes> allNotes = new ArrayList<>();
    private final List<Notes> visibleNotes = new ArrayList<>();

    private final NotesClickListener notesClickListener = new NotesClickListener() {
        @Override
        public void onClick(Notes notes) {
            openEditor(notes);
        }

        @Override
        public void onLongPress(Notes notes, View anchor) {
            showPop(anchor, notes);
        }

        @Override
        public void onTogglePin(Notes notes) {
            togglePin(notes);
        }

        @Override
        public void onDelete(Notes notes) {
            deleteNote(notes);
        }
    };

    private final ActivityResultLauncher<Intent> noteEditorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    return;
                }
                Serializable extra = result.getData()
                        .getSerializableExtra(NotesTakeActivity.EXTRA_NOTE);
                if (!(extra instanceof Notes)) {
                    return;
                }
                Notes savedNote = (Notes) extra;
                commitPendingDelete();
                persistNote(savedNote, savedNote.getID() <= 0);
            });

    private RecyclerView recyclerView;
    private NoteListAdapter noteListAdapter;
    private RoomDb database;
    private FloatingActionButton fabBtn;
    private TextInputEditText searchInput;
    private MaterialToolbar toolbar;
    private ViewGroup contentContainer;
    private View emptyState;
    private View loadingState;
    private View errorState;
    private ImageView emptyIcon;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private MaterialButton emptyAction;
    private View rootView;

    private Notes selectedNotes;
    private Notes pendingDelete;
    private Snackbar pendingDeleteSnackbar;

    private String currentQuery = "";
    private boolean isLoading = true;
    private boolean loadFailed;
    private boolean destroyed;
    private boolean resumedOnce;
    private Parcelable pendingScrollState;
    private ListState listState = ListState.LOADING;

    private enum ListState {
        LOADING, CONTENT, EMPTY, SEARCH_EMPTY, ERROR
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        destroyed = false;
        resumedOnce = false;

        rootView = view;
        database = RoomDb.getInstance(requireContext());

        recyclerView = view.findViewById(R.id.noteRv);
        fabBtn = view.findViewById(R.id.addBtn);
        searchInput = view.findViewById(R.id.searchInput);
        toolbar = view.findViewById(R.id.toolbar);
        contentContainer = view.findViewById(R.id.contentContainer);
        emptyState = view.findViewById(R.id.emptyState);
        loadingState = view.findViewById(R.id.loadingState);
        errorState = view.findViewById(R.id.errorState);
        emptyIcon = view.findViewById(R.id.emptyIcon);
        emptyTitle = view.findViewById(R.id.emptyTitle);
        emptyMessage = view.findViewById(R.id.emptyMessage);
        emptyAction = view.findViewById(R.id.emptyAction);

        ViewCompat.setAccessibilityHeading(emptyTitle, true);
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.errorTitle), true);
        TooltipCompat.setTooltipText(fabBtn, getString(R.string.new_note));

        AppBarActions.bind(requireContext(), toolbar, this::openSettings);

        if (savedInstanceState != null) {
            currentQuery = savedInstanceState.getString(STATE_QUERY, "");
            pendingScrollState = savedInstanceState.getParcelable(STATE_SCROLL);
            if (!currentQuery.isEmpty()) {
                searchInput.setText(currentQuery);
                searchInput.setSelection(searchInput.getText().length());
            }
        }

        setupRecyclerView();
        setupSearch();
        setupActions();

        updateUiState();
        loadNotes(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isAdded()) {
            return;
        }
        AppBarActions.updateThemeAction(requireContext(), toolbar);
        if (resumedOnce) {
            loadNotes(false, false);
        }
        resumedOnce = true;
    }

    private void setupRecyclerView() {
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(
                getResources().getInteger(R.integer.note_grid_span),
                StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(
                StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(false);
        noteListAdapter = new NoteListAdapter(requireContext(), notesClickListener);
        recyclerView.setAdapter(noteListAdapter);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                currentQuery = editable == null ? "" : editable.toString().trim();
                searchHandler.removeCallbacksAndMessages(null);
                searchHandler.postDelayed(() -> {
                    applyFilter(false);
                    announceSearchState();
                }, 200);
            }
        });
        searchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void setupActions() {
        fabBtn.setOnClickListener(view -> openEditor(null));
        rootView.findViewById(R.id.retryButton).setOnClickListener(view -> loadNotes(true, false));
        emptyAction.setOnClickListener(view -> {
            if (allNotes.isEmpty()) {
                openEditor(null);
            } else {
                clearSearch();
            }
        });
    }

    private void openSettings() {
        startActivity(new Intent(requireContext(), SettingsActivity.class));
    }

    private void openEditor(Notes note) {
        Intent intent = new Intent(requireContext(), NotesTakeActivity.class);
        if (note != null) {
            intent.putExtra(NotesTakeActivity.EXTRA_OLD_NOTE, note);
        }
        noteEditorLauncher.launch(intent);
    }

    private void persistNote(Notes savedNote, boolean isNew) {
        try {
            dbExecutor.execute(() -> {
                try {
                    if (isNew) {
                        database.mainDAO().insert(savedNote);
                    } else {
                        database.mainDAO().update(savedNote.getID(), savedNote.getTitle(),
                                savedNote.getNotes(), savedNote.getDate());
                    }
                    mainHandler.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        if (isNew) {
                            searchInput.setText("");
                            searchHandler.removeCallbacksAndMessages(null);
                            currentQuery = "";
                        }
                        loadNotes(false, isNew);
                        showFeedback(R.string.note_saved);
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not save note", error);
                    mainHandler.post(() -> showSaveFailure(savedNote, isNew));
                }
            });
        } catch (RejectedExecutionException error) {
            showSaveFailure(savedNote, isNew);
        }
    }

    private void showSaveFailure(Notes savedNote, boolean isNew) {
        if (destroyed) {
            return;
        }
        Snackbar.make(rootView, R.string.save_failed, Snackbar.LENGTH_INDEFINITE)
                .setAnchorView(fabBtn)
                .setAction(R.string.retry, view -> persistNote(savedNote, isNew))
                .show();
    }

    private void loadNotes(boolean showLoading, boolean scrollToTop) {
        if (showLoading) {
            isLoading = true;
            loadFailed = false;
            updateUiState();
        }
        final int excludedId = pendingDelete != null ? pendingDelete.getID() : -1;
        try {
            dbExecutor.execute(() -> {
                try {
                    List<Notes> loaded = database.mainDAO().getAll();
                    if (excludedId > 0) {
                        loaded.removeIf(note -> note.getID() == excludedId);
                    }
                    sortNotes(loaded);
                    mainHandler.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        if (pendingDelete != null) {
                            loaded.removeIf(note -> note.getID() == pendingDelete.getID());
                        }
                        isLoading = false;
                        loadFailed = false;
                        allNotes.clear();
                        allNotes.addAll(loaded);
                        applyFilter(scrollToTop);
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not load notes", error);
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
        String query = currentQuery.toLowerCase(Locale.ROOT);
        visibleNotes.clear();
        for (Notes note : allNotes) {
            if (pendingDelete != null && note.getID() == pendingDelete.getID()) {
                continue;
            }
            if (query.isEmpty()
                    || searchable(note.getTitle()).contains(query)
                    || searchable(note.getNotes()).contains(query)) {
                visibleNotes.add(note);
            }
        }

        final Parcelable scrollState = pendingScrollState;
        pendingScrollState = null;
        noteListAdapter.submitList(new ArrayList<>(visibleNotes), () -> {
            if (destroyed) {
                return;
            }
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (scrollState != null && layoutManager != null) {
                layoutManager.onRestoreInstanceState(scrollState);
            } else if (scrollToTop) {
                recyclerView.scrollToPosition(0);
            }
        });
        updateUiState();
    }

    private String searchable(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void sortNotes(List<Notes> notes) {
        Collections.sort(notes, (first, second) -> {
            boolean firstPinned = Boolean.TRUE.equals(first.getPinned());
            boolean secondPinned = Boolean.TRUE.equals(second.getPinned());
            if (firstPinned != secondPinned) {
                return firstPinned ? -1 : 1;
            }
            return Integer.compare(second.getID(), first.getID());
        });
    }

    private void updateUiState() {
        ListState state;
        if (isLoading) {
            state = ListState.LOADING;
        } else if (loadFailed) {
            state = ListState.ERROR;
        } else if (allNotes.isEmpty()) {
            state = ListState.EMPTY;
        } else if (visibleNotes.isEmpty() && !currentQuery.isEmpty()) {
            state = ListState.SEARCH_EMPTY;
        } else {
            state = ListState.CONTENT;
        }

        int total = allNotes.size();
        boolean showCount = state != ListState.LOADING && state != ListState.ERROR;
        if (!showCount) {
            toolbar.setSubtitle(null);
        } else if (currentQuery.isEmpty() || total == 0) {
            toolbar.setSubtitle(getResources().getQuantityString(
                    R.plurals.note_count, total, total));
        } else {
            toolbar.setSubtitle(getResources().getQuantityString(R.plurals.note_count_filtered,
                    total, visibleNotes.size(), total));
        }

        if (state == ListState.EMPTY) {
            emptyIcon.setImageResource(R.drawable.ic_note_24);
            emptyTitle.setText(R.string.empty_title);
            emptyMessage.setText(R.string.empty_message);
            emptyAction.setText(R.string.create_first_note);
        } else if (state == ListState.SEARCH_EMPTY) {
            emptyIcon.setImageResource(R.drawable.ic_search_24);
            emptyTitle.setText(getString(R.string.search_empty_title, currentQuery));
            emptyMessage.setText(R.string.search_empty_message);
            emptyAction.setText(R.string.clear_search);
        }

        if (state != listState) {
            TransitionManager.beginDelayedTransition(contentContainer,
                    new Fade().setDuration(150));
            listState = state;
        }
        recyclerView.setVisibility(state == ListState.CONTENT ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(state == ListState.EMPTY || state == ListState.SEARCH_EMPTY
                ? View.VISIBLE : View.GONE);
        loadingState.setVisibility(state == ListState.LOADING ? View.VISIBLE : View.GONE);
        errorState.setVisibility(state == ListState.ERROR ? View.VISIBLE : View.GONE);
    }

    private void announceSearchState() {
        if (currentQuery.isEmpty() || allNotes.isEmpty()) {
            return;
        }
        String announcement = visibleNotes.isEmpty()
                ? getString(R.string.search_empty_title, currentQuery)
                : getResources().getQuantityString(R.plurals.note_count,
                visibleNotes.size(), visibleNotes.size());
        toolbar.announceForAccessibility(announcement);
    }

    private void clearSearch() {
        searchInput.setText("");
        searchHandler.removeCallbacksAndMessages(null);
        currentQuery = "";
        applyFilter(false);
        searchInput.requestFocus();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
        searchInput.clearFocus();
    }

    private void showPop(View anchor, Notes note) {
        selectedNotes = note;
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.popup_menu);

        MenuItem pinItem = popupMenu.getMenu().findItem(R.id.pin);
        pinItem.setTitle(Boolean.TRUE.equals(note.getPinned())
                ? R.string.unpin_note : R.string.pin_note);

        MenuItem deleteItem = popupMenu.getMenu().findItem(R.id.delete);
        SpannableString deleteTitle = new SpannableString(getString(R.string.delete_note));
        deleteTitle.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.danger)),
                0, deleteTitle.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        deleteItem.setTitle(deleteTitle);

        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (selectedNotes == null) {
            return false;
        }
        if (item.getItemId() == R.id.pin) {
            togglePin(selectedNotes);
            return true;
        }
        if (item.getItemId() == R.id.delete) {
            deleteNote(selectedNotes);
            return true;
        }
        return false;
    }

    private void togglePin(Notes note) {
        commitPendingDelete();
        boolean newPinned = !Boolean.TRUE.equals(note.getPinned());
        try {
            dbExecutor.execute(() -> {
                try {
                    database.mainDAO().pin(note.getID(), newPinned);
                    mainHandler.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        loadNotes(false, false);
                        showFeedback(newPinned
                                ? R.string.pinned_feedback : R.string.unpinned_feedback);
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not update pin state", error);
                    mainHandler.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        loadNotes(false, false);
                        showFeedback(R.string.pin_failed);
                    });
                }
            });
        } catch (RejectedExecutionException ignored) {
            // The fragment is being torn down.
        }
    }

    private void deleteNote(Notes note) {
        commitPendingDelete();

        pendingDelete = note;
        allNotes.removeIf(existing -> existing.getID() == note.getID());
        applyFilter(false);

        Snackbar snackbar = Snackbar.make(rootView, R.string.note_deleted, Snackbar.LENGTH_LONG)
                .setAnchorView(fabBtn)
                .setAction(R.string.undo, view -> {
                    Notes restored = pendingDelete;
                    pendingDelete = null;
                    if (restored != null) {
                        allNotes.removeIf(existing -> existing.getID() == restored.getID());
                        allNotes.add(restored);
                        sortNotes(allNotes);
                        applyFilter(false);
                    }
                    pendingDeleteSnackbar = null;
                });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION && pendingDelete == note) {
                    pendingDelete = null;
                    deleteInBackground(note);
                }
                if (pendingDeleteSnackbar == transientBottomBar) {
                    pendingDeleteSnackbar = null;
                }
            }
        });
        pendingDeleteSnackbar = snackbar;
        snackbar.show();
    }

    private void commitPendingDelete() {
        Notes note = pendingDelete;
        pendingDelete = null;
        if (note != null) {
            deleteInBackground(note);
        }
        if (pendingDeleteSnackbar != null) {
            Snackbar snackbar = pendingDeleteSnackbar;
            pendingDeleteSnackbar = null;
            snackbar.dismiss();
        }
    }

    private void deleteInBackground(Notes note) {
        try {
            dbExecutor.execute(() -> {
                try {
                    database.mainDAO().delete(note);
                    mainHandler.post(() -> {
                        if (!destroyed) {
                            loadNotes(false, false);
                        }
                    });
                } catch (Exception error) {
                    Log.e(TAG, "Could not delete note", error);
                    mainHandler.post(() -> {
                        if (destroyed) {
                            return;
                        }
                        loadNotes(false, false);
                        showFeedback(R.string.delete_failed);
                    });
                }
            });
        } catch (RejectedExecutionException ignored) {
            // The fragment is being torn down; the committed note remains in Room.
        }
    }

    private void showFeedback(int messageRes) {
        if (destroyed) {
            return;
        }
        Snackbar snackbar = Snackbar.make(rootView, messageRes, Snackbar.LENGTH_SHORT);
        snackbar.setAnchorView(fabBtn);
        snackbar.show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, currentQuery);
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager != null) {
            outState.putParcelable(STATE_SCROLL, layoutManager.onSaveInstanceState());
        }
    }

    @Override
    public void onDestroyView() {
        destroyed = true;
        searchHandler.removeCallbacksAndMessages(null);
        mainHandler.removeCallbacksAndMessages(null);
        commitPendingDelete();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        dbExecutor.shutdown();
        super.onDestroy();
    }
}
