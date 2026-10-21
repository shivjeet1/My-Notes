package com.shivjeet1.mynotes.Database;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.shivjeet1.mynotes.Model.Notes;

import java.util.List;

@Dao
public interface MainDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Notes note);
    @Query("SELECT * FROM notes ORDER BY pinned DESC, id DESC")
    List<Notes> getAll();

    @Query("UPDATE notes SET title = :title, note = :notes, date = :date WHERE ID = :id")
    void update(int id, String title, String notes, String date);

    @Delete
    void delete(Notes notes);
    @Query("UPDATE notes SET pinned = :pin Where ID = :id")
    void pin(int id, boolean pin);
    @Query("DELETE FROM notes")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM notes")
    int count();
}
