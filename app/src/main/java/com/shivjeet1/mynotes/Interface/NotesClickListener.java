package com.shivjeet1.mynotes.Interface;

import android.view.View;

import com.shivjeet1.mynotes.Model.Notes;

public interface NotesClickListener {
    void onClick(Notes notes);

    void onLongPress(Notes notes, View anchor);

    void onTogglePin(Notes notes);

    void onDelete(Notes notes);
}
