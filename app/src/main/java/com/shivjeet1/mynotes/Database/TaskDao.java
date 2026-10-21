package com.shivjeet1.mynotes.Database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.shivjeet1.mynotes.Model.Task;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    /**
     * Open tasks first, then by due date (undated last), newest first as the
     * tie-breaker. Completed tasks fall to the bottom.
     */
    @Query("SELECT * FROM tasks ORDER BY done ASC, "
            + "CASE WHEN dueAt = 0 THEN 1 ELSE 0 END ASC, dueAt ASC, id DESC")
    List<Task> getAll();

    @Query("SELECT * FROM tasks WHERE id = :id")
    Task getById(int id);

    @Query("SELECT * FROM tasks WHERE done = 0 AND remindAt > 0 AND reminderFired = 0")
    List<Task> getPendingReminders();

    @Query("UPDATE tasks SET done = :done, completedAt = :completedAt WHERE id = :id")
    void setDone(int id, boolean done, long completedAt);

    @Query("UPDATE tasks SET reminderFired = :fired WHERE id = :id")
    void setReminderFired(int id, boolean fired);

    @Query("DELETE FROM tasks")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM tasks")
    int count();
}
