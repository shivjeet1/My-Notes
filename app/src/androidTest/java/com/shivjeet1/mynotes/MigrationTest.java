package com.shivjeet1.mynotes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.shivjeet1.mynotes.Database.RoomDb;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String TEST_DB = "migration-test";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            RoomDb.class.getCanonicalName(),
            new FrameworkSQLiteOpenHelperFactory());

    @Test
    public void migrate2To3KeepsEveryNote() throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 2);
        database.execSQL("INSERT INTO notes (ID, title, note, date, pinned) "
                + "VALUES (1, 'Keep me', 'Body text', '18 Apr 2025, 09:30', 1)");
        database.close();

        database = helper.runMigrationsAndValidate(
                TEST_DB, 3, true, RoomDb.MIGRATION_2_3);

        Cursor notes = database.query("SELECT title, note, pinned FROM notes WHERE ID = 1");
        assertTrue(notes.moveToFirst());
        assertEquals("Keep me", notes.getString(0));
        assertEquals("Body text", notes.getString(1));
        assertEquals(1, notes.getInt(2));
        notes.close();

        Cursor tasks = database.query("SELECT COUNT(*) FROM tasks");
        assertTrue(tasks.moveToFirst());
        assertEquals(0, tasks.getInt(0));
        tasks.close();

        database.close();
    }
}
