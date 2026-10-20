package com.shivjeet1.mynotes;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shivjeet1.mynotes.Model.Notes;
import com.shivjeet1.mynotes.util.WindowInsetsHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotesTakeActivity extends AppCompatActivity {

    public static final String EXTRA_OLD_NOTE = "old_notes";
    public static final String EXTRA_NOTE = "note";

    private static final String DATE_PATTERN = "d MMM yyyy, HH:mm";

    private EditText titleEdt;
    private EditText noteEdt;
    private TextView editorMeta;
    private TextView bodyError;
    private MenuItem saveItem;
    private MaterialToolbar toolbar;

    private Notes notes = new Notes();
    private boolean isOldNotes;
    private String originalTitle = "";
    private String originalBody = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_take);

        WindowInsetsHelper.apply(this, findViewById(R.id.root));

        toolbar = findViewById(R.id.toolbar);
        titleEdt = findViewById(R.id.titleEdt);
        noteEdt = findViewById(R.id.noteEdt);
        editorMeta = findViewById(R.id.editorMeta);
        bodyError = findViewById(R.id.bodyError);

        toolbar.inflateMenu(R.menu.editor_menu);
        saveItem = toolbar.getMenu().findItem(R.id.action_save);
        toolbar.setNavigationOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                saveNote();
                return true;
            }
            return false;
        });

        readExistingNote();
        updateSaveState();

        titleEdt.addTextChangedListener(editorWatcher);
        noteEdt.addTextChangedListener(editorWatcher);

        if (!isOldNotes) {
            titleEdt.requestFocus();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (hasUnsavedChanges()) {
                    showDiscardDialog();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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

    private final TextWatcher editorWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence text, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
            bodyError.setVisibility(View.GONE);
        }

        @Override
        public void afterTextChanged(Editable editable) {
            updateSaveState();
        }
    };

    private void updateSaveState() {
        if (saveItem != null) {
            saveItem.setEnabled(!noteEdt.getText().toString().trim().isEmpty());
        }
    }

    private boolean hasUnsavedChanges() {
        if (isOldNotes) {
            return !originalTitle.equals(titleEdt.getText().toString())
                    || !originalBody.equals(noteEdt.getText().toString());
        }
        return titleEdt.length() > 0 || noteEdt.length() > 0;
    }

    private void showDiscardDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.discard_title)
                .setMessage(R.string.discard_message)
                .setNegativeButton(R.string.keep_editing, null)
                .setPositiveButton(R.string.discard, (dismissed, which) -> finish())
                .create();
        dialog.setOnShowListener(ignored -> {
            Button discard = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (discard != null) {
                discard.setTextColor(ContextCompat.getColor(this, R.color.danger));
            }
        });
        dialog.show();
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
        setResult(Activity.RESULT_OK, result);
        finish();
    }
}
