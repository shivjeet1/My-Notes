package com.shivjeet1.mynotes.Database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.shivjeet1.mynotes.Model.Notes;
import com.shivjeet1.mynotes.Model.Task;

@Database(entities = {Notes.class, Task.class}, version = 3, exportSchema = true)
public abstract class RoomDb extends RoomDatabase {

    private static final String DATABASE_NAME = "NoteApp";
    private static RoomDb database;

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`title` TEXT NOT NULL, "
                    + "`notes` TEXT NOT NULL, "
                    + "`dueAt` INTEGER NOT NULL, "
                    + "`remindAt` INTEGER NOT NULL, "
                    + "`reminderFired` INTEGER NOT NULL, "
                    + "`done` INTEGER NOT NULL, "
                    + "`createdAt` INTEGER NOT NULL, "
                    + "`completedAt` INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_done` ON `tasks` (`done`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_remindAt` ON `tasks` (`remindAt`)");
        }
    };

    public static synchronized RoomDb getInstance(Context context) {
        if (database == null) {
            database = Room.databaseBuilder(context.getApplicationContext(),
                    RoomDb.class, DATABASE_NAME)
                    .addMigrations(MIGRATION_2_3)
                    .build();
        }
        return database;
    }

    public abstract MainDAO mainDAO();

    public abstract TaskDao taskDao();
}
