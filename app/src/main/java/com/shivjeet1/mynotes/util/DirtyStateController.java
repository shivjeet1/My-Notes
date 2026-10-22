package com.shivjeet1.mynotes.util;

import android.content.DialogInterface;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shivjeet1.mynotes.R;

/**
 * Intercepts back navigation while an editor has unsaved changes and offers the
 * same discard confirmation everywhere.
 */
public final class DirtyStateController {

    public interface DirtyCheck {
        boolean hasUnsavedChanges();
    }

    private final AppCompatActivity activity;
    private final DirtyCheck dirtyCheck;
    private final int titleRes;
    private final int messageRes;

    public DirtyStateController(AppCompatActivity activity, int titleRes, int messageRes,
                                DirtyCheck dirtyCheck) {
        this.activity = activity;
        this.dirtyCheck = dirtyCheck;
        this.titleRes = titleRes;
        this.messageRes = messageRes;
        activity.getOnBackPressedDispatcher().addCallback(activity,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (dirtyCheck.hasUnsavedChanges()) {
                            showDiscardDialog();
                        } else {
                            setEnabled(false);
                            activity.getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
    }

    private void showDiscardDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setNegativeButton(R.string.keep_editing, null)
                .setPositiveButton(R.string.discard, (dismissed, which) -> activity.finish())
                .create();
        dialog.setOnShowListener(ignored -> {
            Button discard = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (discard != null) {
                discard.setTextColor(ContextCompat.getColor(activity, R.color.danger));
            }
        });
        dialog.show();
    }
}
