package com.shivjeet1.mynotes;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.shivjeet1.mynotes.Database.RoomDb;
import com.shivjeet1.mynotes.Model.Notes;
import com.shivjeet1.mynotes.util.AppExecutors;
import com.shivjeet1.mynotes.util.AppPreferences;
import com.shivjeet1.mynotes.util.DirtyStateController;
import com.shivjeet1.mynotes.util.NoteDraftStore;
import com.shivjeet1.mynotes.util.WindowInsetsHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotesTakeActivity extends AppCompatActivity {

    public static final String EXTRA_OLD_NOTE = "old_notes";
    public static final String EXTRA_NOTE = "note";
    public static final String EXTRA_AUTOSAVED = "autosaved";

    private static final String DATE_PATTERN = "d MMM yyyy, HH:mm";
    private static final String STATE_NOTE_ID = "note_id";

    private EditText titleEdt;
    private EditText noteEdt;
    private TextView editorMeta;
    private TextView bodyError;
    private MenuItem saveItem;
    private MaterialToolbar toolbar;
    private TextView autosaveStatus;
    private View draftBanner;
    private View draftDiscard;

    private Notes notes = new Notes();
    private boolean isOldNotes;
    private String originalTitle = "";
    private String originalBody = "";
    private RoomDb database;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autosaveRunnable = this::performAutosave;
    private boolean autosaveEnabled;
    private boolean saving;
    private boolean dirty;
    private boolean autosavedThisSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
        }
        setContentView(R.layout.activity_notes_take);

        WindowInsetsHelper.apply(this, findViewById(R.id.root));

        database = RoomDb.getInstance(this);
        autosaveEnabled = AppPreferences.isNoteAutosaveEnabled(this);

        toolbar = findViewById(R.id.toolbar);
        titleEdt = findViewById(R.id.titleEdt);
        noteEdt = findViewById(R.id.noteEdt);
        editorMeta = findViewById(R.id.editorMeta);
        bodyError = findViewById(R.id.bodyError);
        autosaveStatus = findViewById(R.id.autosaveStatus);
        draftBanner = findViewById(R.id.draftBanner);
        draftDiscard = findViewById(R.id.draftDiscard);

        toolbar.inflateMenu(R.menu.editor_menu);
        saveItem = toolbar.getMenu().findItem(R.id.action_save);
        if (autosaveEnabled) {
            saveItem.setVisible(false);
        }
        toolbar.setNavigationOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                flushAutosaveNow();
                saveNote();
                return true;
            }
            return false;
        });

        readExistingNote();
        if (savedInstanceState != null) {
            int savedId = savedInstanceState.getInt(STATE_NOTE_ID, notes.getID());
            if (savedId != notes.getID() && savedId > 0) {
                notes.setID(savedId);
            }
        }
        checkDraft();
        updateSaveState();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && notes.getID() > 0) {
            findViewById(R.id.root).setTransitionName("note_" + notes.getID());
        }

        titleEdt.addTextChangedListener(editorWatcher);
        noteEdt.addTextChangedListener(editorWatcher);

        if (!isOldNotes) {
            titleEdt.requestFocus();
        }

        if (!autosaveEnabled) {
            new DirtyStateController(this, R.string.discard_title, R.string.discard_message,
                    this::hasUnsavedChanges);
        }

        draftDiscard.setOnClickListener(v -> {
            NoteDraftStore.clear(this, notes.getID());
            draftBanner.setVisibility(View.GONE);
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            titleEdt.post(this::startPostponedEnterTransition);
        }
    }

    private void readExistingNote() {
        Object extra = getIntent().getSerializableExtra(EXTRA_OLD_NOTE);
        if (extra instanceof Notes) {
            Notes existing = (Notes) extra;
            notes = new Notes();
            notes.setID(existing.getID());
            notes.setTitle(existing.getTitle());
            notes.setNotes(existing.getNotes());
            notes.setDate(existing.getDate());
            notes.setPinned(existing.getPinned());

            isOldNotes = true;
            toolbar.setTitle(R.string.edit_note);
            titleEdt.setText(notes.getTitle());
            noteEdt.setText(notes.getNotes());
            originalTitle = titleEdt.getText().toString();
            originalBody = noteEdt.getText().toString();
            if (!TextUtils.isEmpty(notes.getDate())) {
                editorMeta.setText(getString(R.string.edited, notes.getDate()));
                editorMeta.setVisibility(View.VISIBLE);
            }
        } else {
            notes = new Notes();
            toolbar.setTitle(R.string.new_note);
        }
    }

    private void checkDraft() {
        NoteDraftStore.Draft draft = NoteDraftStore.load(this, notes.getID());
        if (draft == null) {
            return;
        }
        boolean differs = !safe(draft.title).equals(safe(originalTitle))
                || !safe(draft.body).equals(safe(originalBody));
        if (!differs && isOldNotes) {
            NoteDraftStore.clear(this, notes.getID());
            return;
        }
        if (!isOldNotes && safe(draft.title).isEmpty() && safe(draft.body).isEmpty()) {
            NoteDraftStore.clear(this, notes.getID());
            return;
        }
        if (isOldNotes && differs) {
            titleEdt.setText(draft.title);
            noteEdt.setText(draft.body);
            originalTitle = titleEdt.getText().toString();
            originalBody = noteEdt.getText().toString();
        } else if (!isOldNotes) {
            titleEdt.setText(draft.title);
            noteEdt.setText(draft.body);
        }
        draftBanner.setVisibility(View.VISIBLE);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private final TextWatcher editorWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence text, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
            bodyError.setVisibility(View.GONE);
            if (autosaveEnabled) {
                dirty = true;
                scheduleAutosave();
                saveDraft();
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            updateSaveState();
        }
    };

    private void scheduleAutosave() {
        handler.removeCallbacks(autosaveRunnable);
        if (!autosaveEnabled) {
            return;
        }
        handler.postDelayed(autosaveRunnable, 500L);
    }

    private void saveDraft() {
        NoteDraftStore.save(this, notes.getID(), titleEdt.getText().toString(), noteEdt.getText().toString());
    }

    private void performAutosave() {
        if (!autosaveEnabled || saving) {
            return;
        }
        String body = noteEdt.getText().toString().trim();
        if (body.isEmpty() && !isOldNotes && notes.getID() <= 0 && titleEdt.getText().toString().trim().isEmpty()) {
            return;
        }
        if (body.isEmpty()) {
            if (!isOldNotes) {
                return;
            }
            // allow saving empty-ish? keep existing behavior: require body for manual, for autosave on existing allow? no, require body
            return;
        }
        String title = titleEdt.getText().toString().trim();
        if (title.isEmpty()) {
            title = getString(R.string.untitled);
        }
        Notes toSave = new Notes();
        toSave.setID(notes.getID());
        toSave.setTitle(title);
        toSave.setNotes(body);
        toSave.setDate(new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(new Date()));
        toSave.setPinned(notes.getPinned());
        saving = true;
        setAutosaveStatus(getString(R.string.autosave_saving));
        boolean isNew = toSave.getID() <= 0 && !isOldNotes;
        AppExecutors.getInstance().io().execute(() -> {
            try {
                long id = 0;
                if (toSave.getID() <= 0) {
                    id = database.mainDAO().insert(toSave);
                } else {
                    database.mainDAO().update(toSave.getID(), toSave.getTitle(), toSave.getNotes(), toSave.getDate());
                    id = toSave.getID();
                }
                final long finalId = id;
                handler.post(() -> {
                    saving = false;
                    dirty = false;
                    if (finalId > 0 && notes.getID() <= 0) {
                        notes.setID((int) finalId);
                        isOldNotes = true;
                    }
                    notes.setTitle(toSave.getTitle());
                    notes.setNotes(toSave.getNotes());
                    notes.setDate(toSave.getDate());
                    originalTitle = titleEdt.getText().toString();
                    originalBody = noteEdt.getText().toString();
                    NoteDraftStore.clear(this, notes.getID());
                    draftBanner.setVisibility(View.GONE);
                    autosavedThisSession = true;
                    setAutosaveStatus(getString(R.string.autosave_saved,
                            new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date())));
                    editorMeta.setText(getString(R.string.edited, notes.getDate()));
                    editorMeta.setVisibility(View.VISIBLE);
                    handler.postDelayed(() -> autosaveStatus.setVisibility(View.GONE), 1200L);
                });
            } catch (Exception e) {
                handler.post(() -> {
                    saving = false;
                    setAutosaveStatus(getString(R.string.autosave_failed));
                    autosaveStatus.setOnClickListener(v -> {
                        autosaveStatus.setOnClickListener(null);
                        performAutosave();
                    });
                });
            }
        });
    }

    private void flushAutosaveNow() {
        handler.removeCallbacks(autosaveRunnable);
        if (autosaveEnabled && dirty && !saving) {
            performAutosave();
        }
    }

    private void setAutosaveStatus(String text) {
        if (!autosaveEnabled) {
            autosaveStatus.setVisibility(View.GONE);
            return;
        }
        autosaveStatus.setText(text);
        autosaveStatus.setVisibility(View.VISIBLE);
    }

    private void updateSaveState() {
        if (saveItem != null && saveItem.isVisible()) {
            saveItem.setEnabled(!noteEdt.getText().toString().trim().isEmpty());
        }
    }

    private boolean hasUnsavedChanges() {
        if (autosaveEnabled) {
            return dirty && !saving;
        }
        if (isOldNotes) {
            return !originalTitle.equals(titleEdt.getText().toString())
                    || !originalBody.equals(noteEdt.getText().toString());
        }
        return titleEdt.length() > 0 || noteEdt.length() > 0;
    }

    private void saveNote() {
        String body = noteEdt.getText().toString().trim();
        if (body.isEmpty()) {
            bodyError.setVisibility(View.VISIBLE);
            noteEdt.requestFocus();
            return;
        }

        String title = titleEdt.getText().toString().trim();
        if (title.isEmpty()) {
            title = getString(R.string.untitled);
        }

        if (!isOldNotes) {
            notes = new Notes();
        }
        notes.setTitle(title);
        notes.setNotes(body);
        notes.setDate(new SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
                .format(new Date()));

        Intent result = new Intent();
        result.putExtra(EXTRA_NOTE, notes);
        if (autosavedThisSession) {
            result.putExtra(EXTRA_AUTOSAVED, true);
        }
        setResult(Activity.RESULT_OK, result);
        NoteDraftStore.clear(this, notes.getID());
        finishEditor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (autosaveEnabled) {
            flushAutosaveNow();
            if (dirty) {
                saveDraft();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (autosaveEnabled) {
            flushAutosaveNow();
            if (dirty) {
                saveDraft();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_NOTE_ID, notes.getID());
    }

    @Override
    @android.annotation.SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        if (autosaveEnabled) {
            flushAutosaveNow();
        }
        finishEditor();
    }

    private void finishEditor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
            return;
        }
        finish();
    }
}
