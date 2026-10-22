package com.shivjeet1.mynotes.Adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.shivjeet1.mynotes.Model.Task;
import com.shivjeet1.mynotes.R;
import com.shivjeet1.mynotes.util.TaskDates;

public class TaskListAdapter extends ListAdapter<Task, TaskListAdapter.TaskViewHolder> {

    public interface TaskListener {
        void onOpen(Task task);

        void onToggleDone(Task task);

        void onMore(Task task, View anchor);
    }

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Task>() {
                @Override
                public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                    return oldItem.getId() == newItem.getId()
                            && TextUtils.equals(oldItem.getTitle(), newItem.getTitle())
                            && TextUtils.equals(oldItem.getNotes(), newItem.getNotes())
                            && oldItem.getDueAt() == newItem.getDueAt()
                            && oldItem.getRemindAt() == newItem.getRemindAt()
                            && oldItem.isReminderFired() == newItem.isReminderFired()
                            && oldItem.isDone() == newItem.isDone()
                            && oldItem.getCompletedAt() == newItem.getCompletedAt();
                }
            };

    private final Context context;
    private final TaskListener listener;

    public TaskListAdapter(Context context, TaskListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        TaskViewHolder holder = new TaskViewHolder(view);
        holder.row.setOnClickListener(clicked -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onOpen(getItem(position));
            }
        });
        holder.check.setOnClickListener(clicked -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onToggleDone(getItem(position));
            }
        });
        holder.more.setOnClickListener(clicked -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onMore(getItem(position), holder.more);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = getItem(position);
        boolean done = task.isDone();

        holder.title.setText(TextUtils.isEmpty(task.getTitle())
                ? context.getString(R.string.untitled) : task.getTitle());
        holder.title.setTextColor(ContextCompat.getColor(context,
                done ? R.color.text_secondary : R.color.text_primary));
        int flags = holder.title.getPaintFlags();
        if (done) {
            holder.title.setPaintFlags(flags | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.title.setPaintFlags(flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

        holder.check.setImageResource(done
                ? R.drawable.ic_check_circle_filled_24
                : R.drawable.ic_radio_button_unchecked_24);
        ImageViewCompat.setImageTintList(holder.check, ColorStateList.valueOf(
                ContextCompat.getColor(context, done ? R.color.success : R.color.outline_interactive)));
        holder.check.setContentDescription(context.getString(
                done ? R.string.mark_not_done : R.string.mark_done));

        boolean overdue = !done && task.getDueAt() > 0
                && task.getDueAt() < System.currentTimeMillis();
        if (task.getDueAt() > 0) {
            holder.dueRow.setVisibility(View.VISIBLE);
            holder.dueText.setText(TaskDates.formatDue(context, task.getDueAt(), done));
            int dueColor = ContextCompat.getColor(context,
                    overdue ? R.color.danger : R.color.text_secondary);
            holder.dueText.setTextColor(dueColor);
            ImageViewCompat.setImageTintList(holder.dueIcon, ColorStateList.valueOf(dueColor));
            holder.dueIcon.setImageResource(overdue
                    ? R.drawable.ic_alert_24 : R.drawable.ic_calendar_today_24);
        } else {
            holder.dueRow.setVisibility(View.GONE);
        }

        if (!done && task.getRemindAt() > 0) {
            holder.reminderRow.setVisibility(View.VISIBLE);
            holder.reminderText.setText(TaskDates.formatReminder(context,
                    task.getRemindAt(), task.isReminderFired()));
        } else {
            holder.reminderRow.setVisibility(View.GONE);
        }
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        final View row;
        final ImageButton check;
        final TextView title;
        final View dueRow;
        final ImageView dueIcon;
        final TextView dueText;
        final View reminderRow;
        final ImageView reminderIcon;
        final TextView reminderText;
        final ImageButton more;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            row = itemView.findViewById(R.id.taskRow);
            check = itemView.findViewById(R.id.taskCheck);
            title = itemView.findViewById(R.id.taskTitle);
            dueRow = itemView.findViewById(R.id.dueRow);
            dueIcon = itemView.findViewById(R.id.dueIcon);
            dueText = itemView.findViewById(R.id.dueText);
            reminderRow = itemView.findViewById(R.id.reminderRow);
            reminderIcon = itemView.findViewById(R.id.reminderIcon);
            reminderText = itemView.findViewById(R.id.reminderText);
            more = itemView.findViewById(R.id.taskMore);
        }
    }
}
