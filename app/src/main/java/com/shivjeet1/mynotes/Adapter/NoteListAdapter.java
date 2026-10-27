package com.shivjeet1.mynotes.Adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.shivjeet1.mynotes.Interface.NotesClickListener;
import com.shivjeet1.mynotes.Model.Notes;
import com.shivjeet1.mynotes.R;
import com.shivjeet1.mynotes.util.Motion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class NoteListAdapter extends ListAdapter<Notes, NoteListAdapter.NotesViewHolder> {

    private static final String DATE_TIME_PATTERN = "d MMM yyyy, HH:mm";
    private static final String LEGACY_DATE_PATTERN = "EEE, d MMM yyyy HH:mm a";
    private static final String DATE_PATTERN = "d MMM yyyy";

    private static final DiffUtil.ItemCallback<Notes> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Notes>() {
                @Override
                public boolean areItemsTheSame(@NonNull Notes oldItem, @NonNull Notes newItem) {
                    return oldItem.getID() == newItem.getID();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Notes oldItem, @NonNull Notes newItem) {
                    return TextUtils.equals(oldItem.getTitle(), newItem.getTitle())
                            && TextUtils.equals(oldItem.getNotes(), newItem.getNotes())
                            && TextUtils.equals(oldItem.getDate(), newItem.getDate())
                            && Objects.equals(oldItem.getPinned(), newItem.getPinned());
                }
            };

    private final Context context;
    private final NotesClickListener listener;

    public NoteListAdapter(Context context, NotesClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public NotesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_list, parent, false);
        NotesViewHolder holder = new NotesViewHolder(view);

        holder.itemView.setOnClickListener(itemView -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onClick(getItem(position));
            }
        });
        holder.itemView.setOnLongClickListener(itemView -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onLongPress(getItem(position), holder.moreBtn);
            }
            return true;
        });
        holder.moreBtn.setOnClickListener(button -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onLongPress(getItem(position), holder.moreBtn);
            }
        });

        ViewCompat.addAccessibilityAction(holder.itemView,
                context.getString(R.string.pin_toggle), (view1, arguments) -> {
                    int position = holder.getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onTogglePin(getItem(position));
                    }
                    return true;
                });
        ViewCompat.addAccessibilityAction(holder.itemView,
                context.getString(R.string.delete_note), (view1, arguments) -> {
                    int position = holder.getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onDelete(getItem(position));
                    }
                    return true;
                });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull NotesViewHolder holder, int position) {
        Notes note = getItem(position);
        boolean pinned = Boolean.TRUE.equals(note.getPinned());
        holder.itemView.setTransitionName("note_" + note.getID());

        String title = note.getTitle();
        holder.titleTxt.setText(TextUtils.isEmpty(title)
                ? context.getString(R.string.untitled)
                : title);
        holder.notesTxt.setText(note.getNotes());

        String date = note.getDate();
        holder.dateTxt.setText(TextUtils.isEmpty(date)
                ? context.getString(R.string.no_date)
                : formatCardDate(date));

        holder.cardView.setStrokeColor(ContextCompat.getColorStateList(context,
                pinned ? R.color.card_stroke_pinned : R.color.card_stroke));
        holder.cardView.setStrokeWidth(context.getResources().getDimensionPixelSize(
                pinned ? R.dimen.card_stroke_width_pinned : R.dimen.card_stroke_width));

        Object previousPinState = holder.pinnedIcon.getTag();
        holder.pinnedIcon.setTag(pinned);
        holder.pinnedIcon.setVisibility(pinned ? View.VISIBLE : View.GONE);
        holder.pinnedIcon.setContentDescription(
                pinned ? context.getString(R.string.pinned) : null);
        boolean justPinned = pinned && previousPinState != null
                && !Boolean.TRUE.equals(previousPinState);
        if (justPinned && Motion.isEnabled(context)) {
            holder.pinnedIcon.setScaleX(0.6f);
            holder.pinnedIcon.setScaleY(0.6f);
            holder.pinnedIcon.setAlpha(0f);
            holder.pinnedIcon.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(Motion.MEDIUM)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .start();
        } else {
            holder.pinnedIcon.animate().cancel();
            holder.pinnedIcon.setScaleX(1f);
            holder.pinnedIcon.setScaleY(1f);
            holder.pinnedIcon.setAlpha(1f);
        }
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getID();
    }

    /**
     * Cards have room for a date, not a full timestamp. Older builds stored a
     * long timestamp; parse the known formats and fall back to the raw value if
     * the data came from a different locale or an unknown format.
     */
    private String formatCardDate(String raw) {
        String[] patterns = {DATE_TIME_PATTERN, LEGACY_DATE_PATTERN, DATE_PATTERN};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.getDefault());
                parser.setLenient(false);
                Date parsed = parser.parse(raw);
                if (parsed != null) {
                    return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(parsed);
                }
            } catch (ParseException ignored) {
                // Try the next known format.
            }
        }
        return raw;
    }

    static class NotesViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final TextView titleTxt;
        final TextView notesTxt;
        final TextView dateTxt;
        final ImageView pinnedIcon;
        final ImageButton moreBtn;

        NotesViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.note_container);
            titleTxt = itemView.findViewById(R.id.titleTxt);
            notesTxt = itemView.findViewById(R.id.notesTxt);
            dateTxt = itemView.findViewById(R.id.dateTxt);
            pinnedIcon = itemView.findViewById(R.id.pinned);
            moreBtn = itemView.findViewById(R.id.moreBtn);
        }
    }
}
