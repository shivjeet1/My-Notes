package com.shivjeet1.mynotes;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.shivjeet1.mynotes.Adapter.NoteListAdapter;
import com.shivjeet1.mynotes.Database.RoomDb;
import com.shivjeet1.mynotes.Interface.NotesClickListener;
import com.shivjeet1.mynotes.Model.Notes;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    RecyclerView recyclerView;
    NoteListAdapter noteListAdapter;
    RoomDb database;
    List<Notes> notes = new ArrayList<>();
    Notes selectedNotes;

    FloatingActionButton fabBtn;
    SearchView searchView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.noteRv);

        fabBtn = findViewById(R.id.addBtn);
        searchView = findViewById(R.id.searchView);
       database = RoomDb.getInstance(this);

       notes = database.mainDAO().getAll();

       updateRecycle(notes);

       fabBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               Intent intent = new Intent(MainActivity.this,NotesTakeActivity.class);
               startActivityForResult(intent,101);
           }
       });

       searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
           @Override
           public boolean onQueryTextSubmit(String query) {
               return false;
           }

           @Override
           public boolean onQueryTextChange(String newText) {
               filter(newText);
               return true;
           }
       });
    }

    private void filter(String newtext){
        List<Notes> filterList = new ArrayList<>();
        for (Notes singleNote : notes){
            if (singleNote.getTitle().toLowerCase().contains(newtext.toLowerCase())
            ||singleNote.getNotes().toLowerCase().contains(newtext.toLowerCase())){
                filterList.add(singleNote);
            }
        }
        noteListAdapter.filterList(filterList);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101){
            if (resultCode == Activity.RESULT_OK){
                Notes new_notes = (Notes) data.getSerializableExtra("note");
                database.mainDAO().insert(new_notes);
                notes.clear();
                notes.addAll(database.mainDAO().getAll());
                noteListAdapter.notifyDataSetChanged();
            }
        } else  if (requestCode == 102){ //update and edit the notes on click of notes
            if (resultCode == Activity.RESULT_OK){
                Notes new_notes = (Notes) data.getSerializableExtra("note");
                database.mainDAO().update(new_notes.getID(), new_notes.getTitle(),new_notes.getNotes());
                notes.clear();
                notes.addAll(database.mainDAO().getAll());
                noteListAdapter.notifyDataSetChanged();
            }
        }


    }

    private void updateRecycle(List<Notes> notes){
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));
        noteListAdapter = new NoteListAdapter(MainActivity.this,notes,notesClickListener);
        recyclerView.setAdapter(noteListAdapter);
    }
    private final NotesClickListener notesClickListener = new NotesClickListener() {
        @Override
        public void onClick(Notes notes) {
            Intent intent = new Intent(MainActivity.this,NotesTakeActivity.class);
            intent.putExtra("old_notes",notes);
            startActivityForResult(intent,102);
        }

        @Override
        public void onLongPress(Notes notes, CardView cardView) {
        //on long press we have to unpin/ pin the notes and delete the notes

            selectedNotes = new Notes();
            selectedNotes = notes;
            showPop(cardView);
        }
    };

    private void showPop(CardView cardView) {
        //let create a popup menu
        PopupMenu popupMenu = new PopupMenu(this,cardView);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.popup_menu);
        popupMenu.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.pin){
            if (selectedNotes.getPinned()){
                database.mainDAO().pin(selectedNotes.getID(),false);
                Toast.makeText(this, "Unstarred", Toast.LENGTH_SHORT).show();
            }else{
                database.mainDAO().pin(selectedNotes.getID(),true);
                Toast.makeText(this, "Starred", Toast.LENGTH_SHORT).show();
            }
            notes.clear();
            notes.addAll(database.mainDAO().getAll());
            noteListAdapter.notifyDataSetChanged();
            return  true;
        } else if (item.getItemId() == R.id.delete) {
            database.mainDAO().delete(selectedNotes);
            notes.remove(selectedNotes);
            noteListAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Note is Deleted successfully..", Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }
}
