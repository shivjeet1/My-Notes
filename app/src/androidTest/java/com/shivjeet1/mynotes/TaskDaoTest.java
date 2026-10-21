package com.shivjeet1.mynotes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.shivjeet1.mynotes.Database.RoomDb;
import com.shivjeet1.mynotes.Database.TaskDao;
import com.shivjeet1.mynotes.Model.Task;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class TaskDaoTest {

    private RoomDb database;
    private TaskDao taskDao;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        RoomDb.class)
                .allowMainThreadQueries()
                .build();
        taskDao = database.taskDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void ordersOpenTasksByDueDateAndSinksCompletedOnes() {
        taskDao.insert(task("Later", 200L, false));
        taskDao.insert(task("Sooner", 100L, false));
        taskDao.insert(task("Someday", 0L, false));
        taskDao.insert(task("Finished", 50L, true));

        List<Task> tasks = taskDao.getAll();
        assertEquals(4, tasks.size());
        assertEquals("Sooner", tasks.get(0).getTitle());
        assertEquals("Later", tasks.get(1).getTitle());
        assertEquals("Someday", tasks.get(2).getTitle());
        assertEquals("Finished", tasks.get(3).getTitle());
    }

    @Test
    public void pendingRemindersSkipDoneAndAlreadyFiredTasks() {
        long now = System.currentTimeMillis();
        taskDao.insert(taskWithReminder("Open", now + 60_000L, false, false));
        taskDao.insert(taskWithReminder("Done", now + 60_000L, true, false));
        taskDao.insert(taskWithReminder("Fired", now + 60_000L, false, true));
        taskDao.insert(taskWithReminder("No reminder", 0L, false, false));

        List<Task> pending = taskDao.getPendingReminders();
        assertEquals(1, pending.size());
        assertEquals("Open", pending.get(0).getTitle());
    }

    @Test
    public void markDoneRecordsCompletionTime() {
        Task task = task("Call dentist", 0L, false);
        int id = (int) taskDao.insert(task);

        long completedAt = System.currentTimeMillis();
        taskDao.setDone(id, true, completedAt);

        Task loaded = taskDao.getById(id);
        assertNotNull(loaded);
        assertTrue(loaded.isDone());
        assertEquals(completedAt, loaded.getCompletedAt());

        taskDao.setDone(id, false, 0L);
        loaded = taskDao.getById(id);
        assertNotNull(loaded);
        assertFalse(loaded.isDone());
        assertEquals(0L, loaded.getCompletedAt());
    }

    private Task task(String title, long dueAt, boolean done) {
        Task task = new Task();
        task.setTitle(title);
        task.setNotes("");
        task.setDueAt(dueAt);
        task.setDone(done);
        task.setCreatedAt(System.currentTimeMillis());
        if (done) {
            task.setCompletedAt(System.currentTimeMillis());
        }
        return task;
    }

    private Task taskWithReminder(String title, long remindAt, boolean done, boolean fired) {
        Task task = task(title, 0L, done);
        task.setRemindAt(remindAt);
        task.setReminderFired(fired);
        return task;
    }
}
