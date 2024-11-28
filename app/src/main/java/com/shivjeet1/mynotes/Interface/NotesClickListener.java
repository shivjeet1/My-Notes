package com.shivjeet1.mynotes.Interface;

import androidx.cardview.widget.CardView;

import com.shivjeet1.mynotes.Model.Notes;

public interface NotesClickListener {
    void onClick(Notes notes);
    void onLongPress(Notes notes, CardView cardView);
}
