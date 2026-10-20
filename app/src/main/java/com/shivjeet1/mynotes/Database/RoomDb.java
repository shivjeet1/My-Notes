package com.shivjeet1.mynotes.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.shivjeet1.mynotes.Model.Notes;

@Database(entities = Notes.class, version = 2, exportSchema = false)
public abstract class RoomDb extends RoomDatabase {

    private static final String DATABASE_NAME = "NoteApp";
    private static RoomDb database;

    public static synchronized RoomDb getInstance(Context context) {
        if (database == null) {
            // Schema version 2 is stable; future schema changes must ship a Migration.
            database = Room.databaseBuilder(context.getApplicationContext(),
                    RoomDb.class, DATABASE_NAME)
                    .build();
        }
        return database;
    }

    public abstract MainDAO mainDAO();
}
