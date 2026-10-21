package com.shivjeet1.mynotes.Model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import androidx.annotation.NonNull;

/** A single to-do item with an optional due date and reminder. */
@Entity(tableName = "tasks", indices = {@Index("done"), @Index("remindAt")})
public class Task {

    @PrimaryKey(autoGenerate = true)
    int id = 0;

    @NonNull
    @ColumnInfo(name = "title")
    String title = "";

    @NonNull
    @ColumnInfo(name = "notes")
    String notes = "";

    /** Epoch milliseconds. 0 means the task has no due date. */
    @ColumnInfo(name = "dueAt")
    long dueAt = 0L;

    /** Epoch milliseconds. 0 means no reminder is scheduled. */
    @ColumnInfo(name = "remindAt")
    long remindAt = 0L;

    @ColumnInfo(name = "reminderFired")
    boolean reminderFired = false;

    @ColumnInfo(name = "done")
    boolean done = false;

    @ColumnInfo(name = "createdAt")
    long createdAt = 0L;

    /** Epoch milliseconds. 0 while the task is still open. */
    @ColumnInfo(name = "completedAt")
    long completedAt = 0L;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getDueAt() {
        return dueAt;
    }

    public void setDueAt(long dueAt) {
        this.dueAt = dueAt;
    }

    public long getRemindAt() {
        return remindAt;
    }

    public void setRemindAt(long remindAt) {
        this.remindAt = remindAt;
    }

    public boolean isReminderFired() {
        return reminderFired;
    }

    public void setReminderFired(boolean reminderFired) {
        this.reminderFired = reminderFired;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }
}
