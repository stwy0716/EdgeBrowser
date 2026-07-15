package com.edge.browser.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "edge_browser.db";
    private static final int DB_VERSION = 1;

    // 书签表
    public static final String TABLE_BOOKMARKS = "bookmarks";
    public static final String COL_BM_ID = "id";
    public static final String COL_BM_TITLE = "title";
    public static final String COL_BM_URL = "url";
    public static final String COL_BM_ICON = "icon_url";
    public static final String COL_BM_CREATED = "created_at";

    // 历史表
    public static final String TABLE_HISTORY = "history";
    public static final String COL_H_ID = "id";
    public static final String COL_H_TITLE = "title";
    public static final String COL_H_URL = "url";
    public static final String COL_H_VISITED = "visited_at";

    // 标签页状态表
    public static final String TABLE_TABS = "tabs_state";
    public static final String COL_T_ID = "tab_index";
    public static final String COL_T_TITLE = "title";
    public static final String COL_T_URL = "url";
    public static final String COL_T_PINNED = "is_pinned";

    // 设置表
    public static final String TABLE_SETTINGS = "settings";
    public static final String COL_S_KEY = "key";
    public static final String COL_S_VALUE = "value";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_BOOKMARKS + " ("
                + COL_BM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_BM_TITLE + " TEXT NOT NULL, "
                + COL_BM_URL + " TEXT NOT NULL, "
                + COL_BM_ICON + " TEXT, "
                + COL_BM_CREATED + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_HISTORY + " ("
                + COL_H_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_H_TITLE + " TEXT NOT NULL, "
                + COL_H_URL + " TEXT NOT NULL, "
                + COL_H_VISITED + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_TABS + " ("
                + COL_T_ID + " INTEGER PRIMARY KEY, "
                + COL_T_TITLE + " TEXT, "
                + COL_T_URL + " TEXT, "
                + COL_T_PINNED + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_SETTINGS + " ("
                + COL_S_KEY + " TEXT PRIMARY KEY, "
                + COL_S_VALUE + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 简单升级策略：重建表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TABS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    // === 设置操作 ===

    public void setSetting(String key, String value) {
        ContentValues cv = new ContentValues();
        cv.put(COL_S_KEY, key);
        cv.put(COL_S_VALUE, value);
        getWritableDatabase().insertWithOnConflict(TABLE_SETTINGS, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getSetting(String key, String defaultValue) {
        Cursor c = getReadableDatabase().query(TABLE_SETTINGS,
                new String[]{COL_S_VALUE}, COL_S_KEY + "=?",
                new String[]{key}, null, null, null);
        try {
            if (c.moveToFirst()) return c.getString(0);
        } finally {
            c.close();
        }
        return defaultValue;
    }

    // === 书签操作 ===

    public long addBookmark(String title, String url, String iconUrl) {
        ContentValues cv = new ContentValues();
        cv.put(COL_BM_TITLE, title);
        cv.put(COL_BM_URL, url);
        cv.put(COL_BM_ICON, iconUrl);
        cv.put(COL_BM_CREATED, System.currentTimeMillis());
        return getWritableDatabase().insert(TABLE_BOOKMARKS, null, cv);
    }

    public void removeBookmark(long id) {
        getWritableDatabase().delete(TABLE_BOOKMARKS, COL_BM_ID + "=?", new String[]{String.valueOf(id)});
    }

    public Cursor getBookmarks() {
        return getReadableDatabase().query(TABLE_BOOKMARKS, null, null, null, null, null,
                COL_BM_CREATED + " DESC");
    }

    public boolean isBookmarked(String url) {
        Cursor c = getReadableDatabase().query(TABLE_BOOKMARKS,
                new String[]{COL_BM_ID}, COL_BM_URL + "=?",
                new String[]{url}, null, null, null);
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    // === 历史操作 ===

    public long addHistory(String title, String url) {
        // 先删除旧记录
        getWritableDatabase().delete(TABLE_HISTORY, COL_H_URL + "=?", new String[]{url});
        ContentValues cv = new ContentValues();
        cv.put(COL_H_TITLE, title);
        cv.put(COL_H_URL, url);
        cv.put(COL_H_VISITED, System.currentTimeMillis());
        return getWritableDatabase().insert(TABLE_HISTORY, null, cv);
    }

    public void clearHistory() {
        getWritableDatabase().delete(TABLE_HISTORY, null, null);
    }

    public Cursor getHistory() {
        return getReadableDatabase().query(TABLE_HISTORY, null, null, null, null, null,
                COL_H_VISITED + " DESC");
    }

    // === 标签页状态操作 ===

    public void saveTabState(int index, String title, String url, boolean pinned) {
        ContentValues cv = new ContentValues();
        cv.put(COL_T_ID, index);
        cv.put(COL_T_TITLE, title);
        cv.put(COL_T_URL, url);
        cv.put(COL_T_PINNED, pinned ? 1 : 0);
        getWritableDatabase().insertWithOnConflict(TABLE_TABS, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor getTabStates() {
        return getReadableDatabase().query(TABLE_TABS, null, null, null, null, null,
                COL_T_ID + " ASC");
    }

    public void clearTabStates() {
        getWritableDatabase().delete(TABLE_TABS, null, null);
    }

    // === 转换为列表的辅助方法 ===

    public static class BookmarkEntry {
        public long id;
        public String title;
        public String url;
        public String iconUrl;
        public long createdAt;
    }

    public static class HistoryEntry {
        public long id;
        public String title;
        public String url;
        public long visitedAt;
    }

    public static class TabState {
        public int index;
        public String title;
        public String url;
        public boolean pinned;
    }

    public static List<BookmarkEntry> cursorToBookmarks(Cursor c) {
        List<BookmarkEntry> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                BookmarkEntry e = new BookmarkEntry();
                e.id = c.getLong(c.getColumnIndexOrThrow(COL_BM_ID));
                e.title = c.getString(c.getColumnIndexOrThrow(COL_BM_TITLE));
                e.url = c.getString(c.getColumnIndexOrThrow(COL_BM_URL));
                e.iconUrl = c.getString(c.getColumnIndexOrThrow(COL_BM_ICON));
                e.createdAt = c.getLong(c.getColumnIndexOrThrow(COL_BM_CREATED));
                list.add(e);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public static List<HistoryEntry> cursorToHistory(Cursor c) {
        List<HistoryEntry> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                HistoryEntry e = new HistoryEntry();
                e.id = c.getLong(c.getColumnIndexOrThrow(COL_H_ID));
                e.title = c.getString(c.getColumnIndexOrThrow(COL_H_TITLE));
                e.url = c.getString(c.getColumnIndexOrThrow(COL_H_URL));
                e.visitedAt = c.getLong(c.getColumnIndexOrThrow(COL_H_VISITED));
                list.add(e);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public static List<TabState> cursorToTabStates(Cursor c) {
        List<TabState> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                TabState s = new TabState();
                s.index = c.getInt(c.getColumnIndexOrThrow(COL_T_ID));
                s.title = c.getString(c.getColumnIndexOrThrow(COL_T_TITLE));
                s.url = c.getString(c.getColumnIndexOrThrow(COL_T_URL));
                s.pinned = c.getInt(c.getColumnIndexOrThrow(COL_T_PINNED)) == 1;
                list.add(s);
            }
        } finally {
            c.close();
        }
        return list;
    }
}