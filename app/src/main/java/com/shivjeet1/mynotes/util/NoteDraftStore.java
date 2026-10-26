package com.shivjeet1.mynotes.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class NoteDraftStore {
    private static final String PREFS_NAME = "mynotes_drafts";
    private static final String KEY_NEW_TITLE = "new_title";
    private static final String KEY_NEW_BODY = "new_body";
    private static final String PREFIX_TITLE = "draft_title_";
    private static final String PREFIX_BODY = "draft_body_";

    private NoteDraftStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveNew(Context context, String title, String body) {
        prefs(context).edit()
                .putString(KEY_NEW_TITLE, title == null ? "" : title)
                .putString(KEY_NEW_BODY, body == null ? "" : body)
                .apply();
    }

    public static Draft loadNew(Context context) {
        SharedPreferences p = prefs(context);
        String title = p.getString(KEY_NEW_TITLE, "");
        String body = p.getString(KEY_NEW_BODY, "");
        if ((title == null || title.isEmpty()) && (body == null || body.isEmpty())) {
            return null;
        }
        return new Draft(title, body);
    }

    public static void clearNew(Context context) {
        prefs(context).edit().remove(KEY_NEW_TITLE).remove(KEY_NEW_BODY).apply();
    }

    public static void save(Context context, int id, String title, String body) {
        if (id <= 0) {
            saveNew(context, title, body);
            return;
        }
        prefs(context).edit()
                .putString(PREFIX_TITLE + id, title == null ? "" : title)
                .putString(PREFIX_BODY + id, body == null ? "" : body)
                .apply();
    }

    public static Draft load(Context context, int id) {
        if (id <= 0) {
            return loadNew(context);
        }
        SharedPreferences p = prefs(context);
        String title = p.getString(PREFIX_TITLE + id, null);
        String body = p.getString(PREFIX_BODY + id, null);
        if (title == null && body == null) {
            return null;
        }
        if ((title == null || title.isEmpty()) && (body == null || body.isEmpty())) {
            return null;
        }
        return new Draft(title == null ? "" : title, body == null ? "" : body);
    }

    public static void clear(Context context, int id) {
        if (id <= 0) {
            clearNew(context);
            return;
        }
        prefs(context).edit()
                .remove(PREFIX_TITLE + id)
                .remove(PREFIX_BODY + id)
                .apply();
    }

    public static class Draft {
        public final String title;
        public final String body;

        public Draft(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }
}
