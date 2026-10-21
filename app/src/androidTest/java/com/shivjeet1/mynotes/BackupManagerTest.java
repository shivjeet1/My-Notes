package com.shivjeet1.mynotes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.shivjeet1.mynotes.Model.Notes;
import com.shivjeet1.mynotes.Model.Task;
import com.shivjeet1.mynotes.util.BackupManager;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class BackupManagerTest {

    @Test
    public void roundTripPreservesEveryField() throws Exception {
        Notes first = note(4, "Grocery list", "Milk, eggs", "18 Apr 2025, 09:30", true);
        Notes second = note(9, "Untitled", "A thought", "19 Apr 2025, 20:15", false);
        List<Notes> source = new ArrayList<>();
        source.add(first);
        source.add(second);

        Task task = new Task();
        task.setId(7);
        task.setTitle("Call the dentist");
        task.setNotes("Ask about Friday");
        task.setDueAt(1_767_225_600_000L);
        task.setRemindAt(1_767_222_000_000L);
        task.setReminderFired(true);
        task.setDone(false);
        task.setCreatedAt(1_767_000_000_000L);
        task.setCompletedAt(0L);
        List<Task> taskSource = new ArrayList<>();
        taskSource.add(task);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BackupManager.write(output, source, taskSource);

        String document = output.toString("UTF-8");
        assertTrue(document.contains("\"format\": \"mynotes-backup\""));
        assertTrue(document.contains("\"version\": 1"));
        assertTrue(document.contains("\"notes\": 2"));
        assertTrue(document.contains("\"tasks\": 1"));

        BackupManager.BackupData restored = BackupManager.read(
                new ByteArrayInputStream(output.toByteArray()));
        assertEquals(2, restored.getNotes().size());
        assertEquals(1, restored.getTasks().size());

        Notes restoredFirst = restored.getNotes().get(0);
        assertEquals(4, restoredFirst.getID());
        assertEquals("Grocery list", restoredFirst.getTitle());
        assertEquals("Milk, eggs", restoredFirst.getNotes());
        assertEquals("18 Apr 2025, 09:30", restoredFirst.getDate());
        assertTrue(restoredFirst.getPinned());
        assertFalse(restored.getNotes().get(1).getPinned());

        Task restoredTask = restored.getTasks().get(0);
        assertEquals(7, restoredTask.getId());
        assertEquals("Call the dentist", restoredTask.getTitle());
        assertEquals("Ask about Friday", restoredTask.getNotes());
        assertEquals(1_767_225_600_000L, restoredTask.getDueAt());
        assertEquals(1_767_222_000_000L, restoredTask.getRemindAt());
        assertTrue(restoredTask.isReminderFired());
        assertEquals(1_767_000_000_000L, restoredTask.getCreatedAt());
    }

    @Test
    public void emptyBackupRoundTrips() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BackupManager.write(output, new ArrayList<>(), new ArrayList<>());

        BackupManager.BackupData restored = BackupManager.read(
                new ByteArrayInputStream(output.toByteArray()));
        assertTrue(restored.getNotes().isEmpty());
        assertTrue(restored.getTasks().isEmpty());
    }

    @Test
    public void rejectsUnrelatedJson() {
        assertFailure("{\"hello\":\"world\"}", BackupManager.Failure.INVALID_FORMAT);
    }

    @Test
    public void rejectsBrokenJson() {
        assertFailure("{not json", BackupManager.Failure.INVALID_FORMAT);
    }

    @Test
    public void rejectsNewerFormatVersion() {
        assertFailure("{\"format\":\"mynotes-backup\",\"version\":2,\"notes\":[]}",
                BackupManager.Failure.UNSUPPORTED_VERSION);
    }

    @Test
    public void rejectsMissingNotesArray() {
        assertFailure("{\"format\":\"mynotes-backup\",\"version\":1}",
                BackupManager.Failure.MALFORMED);
    }

    @Test
    public void rejectsDuplicateIds() {
        assertFailure("{\"format\":\"mynotes-backup\",\"version\":1,\"notes\":["
                        + "{\"id\":1,\"title\":\"A\",\"note\":\"a\"},"
                        + "{\"id\":1,\"title\":\"B\",\"note\":\"b\"}]}",
                BackupManager.Failure.MALFORMED);
    }

    @Test
    public void rejectsDuplicateTaskIds() {
        assertFailure("{\"format\":\"mynotes-backup\",\"version\":1,\"notes\":[],\"tasks\":["
                        + "{\"id\":1,\"title\":\"A\"},"
                        + "{\"id\":1,\"title\":\"B\"}]}",
                BackupManager.Failure.MALFORMED);
    }

    @Test
    public void readsNotesWithoutOptionalFields() throws Exception {
        String document = "{\"format\":\"mynotes-backup\",\"version\":1,\"notes\":["
                + "{\"id\":3,\"title\":\"Only a title\"}]}";
        BackupManager.BackupData restored = BackupManager.read(
                new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)));
        Notes note = restored.getNotes().get(0);
        assertEquals(3, note.getID());
        assertEquals("Only a title", note.getTitle());
        assertEquals("", note.getNotes());
        assertEquals("", note.getDate());
        assertFalse(note.getPinned());
    }

    private void assertFailure(String document, BackupManager.Failure expected) {
        try {
            BackupManager.read(new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)));
            fail("Expected " + expected);
        } catch (BackupManager.BackupException error) {
            assertEquals(expected, error.getFailure());
        }
    }

    private Notes note(int id, String title, String body, String date, boolean pinned) {
        Notes note = new Notes();
        note.setID(id);
        note.setTitle(title);
        note.setNotes(body);
        note.setDate(date);
        note.setPinned(pinned);
        return note;
    }
}
