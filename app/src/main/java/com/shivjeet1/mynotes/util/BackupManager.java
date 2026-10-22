package com.shivjeet1.mynotes.util;

import com.shivjeet1.mynotes.Model.Notes;
import com.shivjeet1.mynotes.Model.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Reads and writes the human-readable {@code notes-backup.json} format.
 *
 * <p>The format is deliberately plain: UTF-8, two-space indentation, and the
 * same note fields the app stores in Room. A {@code format} and {@code version}
 * header make it possible to reject unrelated JSON files and to evolve the
 * format without guessing.</p>
 */
public final class BackupManager {

    public static final String FILE_NAME = "notes-backup.json";
    public static final String FORMAT = "mynotes-backup";
    public static final int VERSION = 1;
    private static final String ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public enum Failure {
        INVALID_FORMAT,
        UNSUPPORTED_VERSION,
        MALFORMED,
        IO
    }

    public static final class BackupException extends Exception {
        private final Failure failure;

        public BackupException(Failure failure) {
            super(failure.name());
            this.failure = failure;
        }

        public BackupException(Failure failure, Throwable cause) {
            super(failure.name(), cause);
            this.failure = failure;
        }

        public Failure getFailure() {
            return failure;
        }
    }

    public static final class BackupData {
        private final List<Notes> notes;
        private final List<Task> tasks;

        BackupData(List<Notes> notes, List<Task> tasks) {
            this.notes = notes;
            this.tasks = tasks;
        }

        public List<Notes> getNotes() {
            return notes;
        }

        public List<Task> getTasks() {
            return tasks;
        }
    }

    private BackupManager() {
    }

    public static void write(OutputStream output, List<Notes> notes, List<Task> tasks)
            throws IOException {
        JSONObject root = new JSONObject();
        final String document;
        try {
            root.put("format", FORMAT);
            root.put("version", VERSION);
            root.put("exportedAt", timestamp());

            JSONObject counts = new JSONObject();
            counts.put("notes", notes.size());
            counts.put("tasks", tasks.size());
            root.put("counts", counts);

            JSONArray noteArray = new JSONArray();
            for (Notes note : notes) {
                JSONObject json = new JSONObject();
                json.put("id", note.getID());
                json.put("title", safe(note.getTitle()));
                json.put("note", safe(note.getNotes()));
                json.put("date", safe(note.getDate()));
                json.put("pinned", Boolean.TRUE.equals(note.getPinned()));
                noteArray.put(json);
            }
            root.put("notes", noteArray);

            JSONArray taskArray = new JSONArray();
            for (Task task : tasks) {
                JSONObject json = new JSONObject();
                json.put("id", task.getId());
                json.put("title", safe(task.getTitle()));
                json.put("notes", safe(task.getNotes()));
                json.put("dueAt", task.getDueAt());
                json.put("remindAt", task.getRemindAt());
                json.put("reminderFired", task.isReminderFired());
                json.put("done", task.isDone());
                json.put("createdAt", task.getCreatedAt());
                json.put("completedAt", task.getCompletedAt());
                taskArray.put(json);
            }
            root.put("tasks", taskArray);
            document = root.toString(2);
        } catch (JSONException error) {
            throw new IOException("Could not build the backup document", error);
        }

        OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write(document);
        writer.write('\n');
        writer.flush();
    }

    public static BackupData read(InputStream input) throws BackupException {
        final String content;
        try {
            content = readAll(input);
        } catch (IOException error) {
            throw new BackupException(Failure.IO, error);
        }

        final JSONObject root;
        try {
            root = new JSONObject(content);
        } catch (JSONException error) {
            throw new BackupException(Failure.INVALID_FORMAT, error);
        }

        if (!FORMAT.equals(root.optString("format"))) {
            throw new BackupException(Failure.INVALID_FORMAT);
        }

        int version = root.optInt("version", -1);
        if (version < 1) {
            throw new BackupException(Failure.MALFORMED);
        }
        if (version > VERSION) {
            throw new BackupException(Failure.UNSUPPORTED_VERSION);
        }

        JSONArray noteArray = root.optJSONArray("notes");
        if (noteArray == null) {
            throw new BackupException(Failure.MALFORMED);
        }

        List<Notes> notes = new ArrayList<>(noteArray.length());
        Set<Integer> seenIds = new HashSet<>();
        for (int index = 0; index < noteArray.length(); index++) {
            JSONObject json = noteArray.optJSONObject(index);
            if (json == null) {
                throw new BackupException(Failure.MALFORMED);
            }
            int id = json.optInt("id", 0);
            if (id < 0 || (id > 0 && !seenIds.add(id))) {
                throw new BackupException(Failure.MALFORMED);
            }

            Notes note = new Notes();
            note.setID(id);
            note.setTitle(json.optString("title", ""));
            note.setNotes(json.optString("note", ""));
            note.setDate(json.optString("date", ""));
            note.setPinned(json.optBoolean("pinned", false));
            notes.add(note);
        }

        List<Task> tasks = new ArrayList<>();
        JSONArray taskArray = root.optJSONArray("tasks");
        if (taskArray != null) {
            Set<Integer> seenTaskIds = new HashSet<>();
            for (int index = 0; index < taskArray.length(); index++) {
                JSONObject json = taskArray.optJSONObject(index);
                if (json == null) {
                    throw new BackupException(Failure.MALFORMED);
                }
                int id = json.optInt("id", 0);
                if (id < 0 || (id > 0 && !seenTaskIds.add(id))) {
                    throw new BackupException(Failure.MALFORMED);
                }

                Task task = new Task();
                task.setId(id);
                task.setTitle(json.optString("title", ""));
                task.setNotes(json.optString("notes", ""));
                task.setDueAt(json.optLong("dueAt", 0L));
                task.setRemindAt(json.optLong("remindAt", 0L));
                task.setReminderFired(json.optBoolean("reminderFired", false));
                task.setDone(json.optBoolean("done", false));
                task.setCreatedAt(json.optLong("createdAt", 0L));
                task.setCompletedAt(json.optLong("completedAt", 0L));
                tasks.add(task);
            }
        }

        return new BackupData(notes, tasks);
    }

    private static String readAll(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, read);
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String timestamp() {
        SimpleDateFormat format = new SimpleDateFormat(ISO_PATTERN, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }
}
