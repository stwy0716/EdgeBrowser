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
    private static final int DB_VERSION = 2;

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

    // 标签页表 - 隐私模式
    public static final String COL_T_PRIVATE = "is_private";

    // 站点设置表
    public static final String TABLE_SITE_SETTINGS = "site_settings";
    public static final String COL_SS_DOMAIN = "domain";
    public static final String COL_SS_DESKTOP_MODE = "desktop_mode";
    public static final String COL_SS_JS_ENABLED = "javascript_enabled";
    public static final String COL_SS_BLOCK_IMAGES = "block_images";
    public static final String COL_SS_AD_BLOCK_OVERRIDE = "ad_block_override";

    // 离线阅读列表表
    public static final String TABLE_READING_LIST = "reading_list";
    public static final String COL_RL_ID = "id";
    public static final String COL_RL_TITLE = "title";
    public static final String COL_RL_URL = "url";
    public static final String COL_RL_SAVED_PATH = "saved_path";
    public static final String COL_RL_SAVED_AT = "saved_at";
    public static final String COL_RL_IS_READ = "is_read";

    // 扩展表
    public static final String TABLE_EXTENSIONS = "extensions";
    public static final String COL_EXT_ID = "id";
    public static final String COL_EXT_NAME = "name";
    public static final String COL_EXT_DESC = "description";
    public static final String COL_EXT_VERSION = "version";
    public static final String COL_EXT_JS = "js_content";
    public static final String COL_EXT_CSS = "css_content";
    public static final String COL_EXT_ENABLED = "enabled";
    public static final String COL_EXT_INSTALLED_AT = "installed_at";

    // 统计表
    public static final String TABLE_STATS = "stats";
    public static final String COL_ST_DATE = "date";
    public static final String COL_ST_PAGES = "pages_visited";
    public static final String COL_ST_DATA = "data_received";
    public static final String COL_ST_TIME = "time_spent";

    // 密码表
    public static final String TABLE_PASSWORDS = "passwords";
    public static final String COL_PW_ID = "id";
    public static final String COL_PW_DOMAIN = "domain";
    public static final String COL_PW_USERNAME = "username";
    public static final String COL_PW_PASSWORD = "password";
    public static final String COL_PW_SAVED_AT = "saved_at";

    // 快捷链接表
    public static final String TABLE_QUICK_LINKS = "quick_links";
    public static final String COL_QL_ID = "id";
    public static final String COL_QL_TITLE = "title";
    public static final String COL_QL_URL = "url";
    public static final String COL_QL_POSITION = "position";

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
                + COL_T_PINNED + " INTEGER DEFAULT 0, "
                + COL_T_PRIVATE + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_SETTINGS + " ("
                + COL_S_KEY + " TEXT PRIMARY KEY, "
                + COL_S_VALUE + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_SITE_SETTINGS + " ("
                + COL_SS_DOMAIN + " TEXT PRIMARY KEY, "
                + COL_SS_DESKTOP_MODE + " INTEGER DEFAULT 0, "
                + COL_SS_JS_ENABLED + " INTEGER DEFAULT 1, "
                + COL_SS_BLOCK_IMAGES + " INTEGER DEFAULT 0, "
                + COL_SS_AD_BLOCK_OVERRIDE + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_READING_LIST + " ("
                + COL_RL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_RL_TITLE + " TEXT NOT NULL, "
                + COL_RL_URL + " TEXT NOT NULL, "
                + COL_RL_SAVED_PATH + " TEXT, "
                + COL_RL_SAVED_AT + " INTEGER DEFAULT 0, "
                + COL_RL_IS_READ + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_EXTENSIONS + " ("
                + COL_EXT_ID + " TEXT PRIMARY KEY, "
                + COL_EXT_NAME + " TEXT NOT NULL, "
                + COL_EXT_DESC + " TEXT, "
                + COL_EXT_VERSION + " TEXT, "
                + COL_EXT_JS + " TEXT, "
                + COL_EXT_CSS + " TEXT, "
                + COL_EXT_ENABLED + " INTEGER DEFAULT 1, "
                + COL_EXT_INSTALLED_AT + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_STATS + " ("
                + COL_ST_DATE + " TEXT PRIMARY KEY, "
                + COL_ST_PAGES + " INTEGER DEFAULT 0, "
                + COL_ST_DATA + " INTEGER DEFAULT 0, "
                + COL_ST_TIME + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_PASSWORDS + " ("
                + COL_PW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PW_DOMAIN + " TEXT NOT NULL, "
                + COL_PW_USERNAME + " TEXT NOT NULL, "
                + COL_PW_PASSWORD + " TEXT NOT NULL, "
                + COL_PW_SAVED_AT + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_QUICK_LINKS + " ("
                + COL_QL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_QL_TITLE + " TEXT NOT NULL, "
                + COL_QL_URL + " TEXT NOT NULL, "
                + COL_QL_POSITION + " INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 简单升级策略：重建表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TABS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SITE_SETTINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_READING_LIST);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXTENSIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PASSWORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUICK_LINKS);
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

    public void saveTabState(int index, String title, String url, boolean pinned, boolean isPrivate) {
        ContentValues cv = new ContentValues();
        cv.put(COL_T_ID, index);
        cv.put(COL_T_TITLE, title);
        cv.put(COL_T_URL, url);
        cv.put(COL_T_PINNED, pinned ? 1 : 0);
        cv.put(COL_T_PRIVATE, isPrivate ? 1 : 0);
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

    // === 站点设置操作 ===

    public Cursor getSiteSettings(String domain) {
        return getReadableDatabase().query(TABLE_SITE_SETTINGS, null,
                COL_SS_DOMAIN + "=?", new String[]{domain}, null, null, null);
    }

    public void saveSiteSettings(String domain, ContentValues values) {
        values.put(COL_SS_DOMAIN, domain);
        getWritableDatabase().insertWithOnConflict(TABLE_SITE_SETTINGS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    // === 离线阅读列表操作 ===

    public long addReadingItem(String title, String url, String savedPath) {
        ContentValues cv = new ContentValues();
        cv.put(COL_RL_TITLE, title);
        cv.put(COL_RL_URL, url);
        cv.put(COL_RL_SAVED_PATH, savedPath);
        cv.put(COL_RL_SAVED_AT, System.currentTimeMillis());
        cv.put(COL_RL_IS_READ, 0);
        return getWritableDatabase().insert(TABLE_READING_LIST, null, cv);
    }

    public Cursor getReadingList() {
        return getReadableDatabase().query(TABLE_READING_LIST, null, null, null, null, null,
                COL_RL_SAVED_AT + " DESC");
    }

    public void removeReadingItem(long id) {
        getWritableDatabase().delete(TABLE_READING_LIST, COL_RL_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    public void markReadingItemRead(long id) {
        ContentValues cv = new ContentValues();
        cv.put(COL_RL_IS_READ, 1);
        getWritableDatabase().update(TABLE_READING_LIST, cv, COL_RL_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    // === 扩展操作 ===

    public void addExtension(String id, String name, String desc, String version,
                             String js, String css) {
        ContentValues cv = new ContentValues();
        cv.put(COL_EXT_ID, id);
        cv.put(COL_EXT_NAME, name);
        cv.put(COL_EXT_DESC, desc);
        cv.put(COL_EXT_VERSION, version);
        cv.put(COL_EXT_JS, js);
        cv.put(COL_EXT_CSS, css);
        cv.put(COL_EXT_ENABLED, 1);
        cv.put(COL_EXT_INSTALLED_AT, System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(TABLE_EXTENSIONS, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor getExtensions() {
        return getReadableDatabase().query(TABLE_EXTENSIONS, null, null, null, null, null,
                COL_EXT_NAME + " ASC");
    }

    public void updateExtensionEnabled(String id, boolean enabled) {
        ContentValues cv = new ContentValues();
        cv.put(COL_EXT_ENABLED, enabled ? 1 : 0);
        getWritableDatabase().update(TABLE_EXTENSIONS, cv, COL_EXT_ID + "=?",
                new String[]{id});
    }

    public void removeExtension(String id) {
        getWritableDatabase().delete(TABLE_EXTENSIONS, COL_EXT_ID + "=?",
                new String[]{id});
    }

    // === 密码操作 ===

    public long addPassword(String domain, String username, String password) {
        ContentValues cv = new ContentValues();
        cv.put(COL_PW_DOMAIN, domain);
        cv.put(COL_PW_USERNAME, username);
        cv.put(COL_PW_PASSWORD, password);
        cv.put(COL_PW_SAVED_AT, System.currentTimeMillis());
        return getWritableDatabase().insert(TABLE_PASSWORDS, null, cv);
    }

    public Cursor getPasswords(String domain) {
        return getReadableDatabase().query(TABLE_PASSWORDS, null,
                COL_PW_DOMAIN + "=?", new String[]{domain}, null, null,
                COL_PW_SAVED_AT + " DESC");
    }

    public Cursor getAllPasswords() {
        return getReadableDatabase().query(TABLE_PASSWORDS, null, null, null, null, null,
                COL_PW_SAVED_AT + " DESC");
    }

    public void removePassword(long id) {
        getWritableDatabase().delete(TABLE_PASSWORDS, COL_PW_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    // === 统计操作 ===

    public void addStatEntry(String date, int pages, long dataBytes, long timeMs) {
        ContentValues cv = new ContentValues();
        cv.put(COL_ST_DATE, date);
        cv.put(COL_ST_PAGES, pages);
        cv.put(COL_ST_DATA, dataBytes);
        cv.put(COL_ST_TIME, timeMs);
        getWritableDatabase().insertWithOnConflict(TABLE_STATS, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor getStats() {
        return getReadableDatabase().query(TABLE_STATS, null, null, null, null, null,
                COL_ST_DATE + " DESC");
    }

    // === 快捷链接操作 ===

    public void saveQuickLinks(List<QuickLinkEntry> links) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_QUICK_LINKS, null, null);
        for (QuickLinkEntry link : links) {
            ContentValues cv = new ContentValues();
            cv.put(COL_QL_TITLE, link.title);
            cv.put(COL_QL_URL, link.url);
            cv.put(COL_QL_POSITION, link.position);
            db.insert(TABLE_QUICK_LINKS, null, cv);
        }
    }

    public Cursor getQuickLinks() {
        return getReadableDatabase().query(TABLE_QUICK_LINKS, null, null, null, null, null,
                COL_QL_POSITION + " ASC");
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
        public boolean isPrivate;
    }

    public static class SiteSettings {
        public String domain;
        public boolean desktopMode;
        public boolean javascriptEnabled;
        public boolean blockImages;
        public boolean adBlockOverride;
    }

    public static class ReadingItem {
        public long id;
        public String title;
        public String url;
        public String savedPath;
        public long savedAt;
        public boolean isRead;
    }

    public static class ExtensionItem {
        public String id;
        public String name;
        public String description;
        public String version;
        public String jsContent;
        public String cssContent;
        public boolean enabled;
        public long installedAt;
    }

    public static class StatEntry {
        public String date;
        public int pagesVisited;
        public long dataReceived;
        public long timeSpent;
    }

    public static class PasswordEntry {
        public long id;
        public String domain;
        public String username;
        public String password;
        public long savedAt;
    }

    public static class QuickLinkEntry {
        public long id;
        public String title;
        public String url;
        public int position;
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
                s.isPrivate = c.getInt(c.getColumnIndexOrThrow(COL_T_PRIVATE)) == 1;
                list.add(s);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public static SiteSettings cursorToSiteSettings(Cursor c) {
        try {
            if (c.moveToFirst()) {
                SiteSettings s = new SiteSettings();
                s.domain = c.getString(c.getColumnIndexOrThrow(COL_SS_DOMAIN));
                s.desktopMode = c.getInt(c.getColumnIndexOrThrow(COL_SS_DESKTOP_MODE)) == 1;
                s.javascriptEnabled = c.getInt(c.getColumnIndexOrThrow(COL_SS_JS_ENABLED)) == 1;
                s.blockImages = c.getInt(c.getColumnIndexOrThrow(COL_SS_BLOCK_IMAGES)) == 1;
                s.adBlockOverride = c.getInt(c.getColumnIndexOrThrow(COL_SS_AD_BLOCK_OVERRIDE)) == 1;
                return s;
            }
        } finally {
            c.close();
        }
        return null;
    }

    public static List<ReadingItem> cursorToReadingList(Cursor c) {
        List<ReadingItem> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                ReadingItem item = new ReadingItem();
                item.id = c.getLong(c.getColumnIndexOrThrow(COL_RL_ID));
                item.title = c.getString(c.getColumnIndexOrThrow(COL_RL_TITLE));
                item.url = c.getString(c.getColumnIndexOrThrow(COL_RL_URL));
                item.savedPath = c.getString(c.getColumnIndexOrThrow(COL_RL_SAVED_PATH));
                item.savedAt = c.getLong(c.getColumnIndexOrThrow(COL_RL_SAVED_AT));
                item.isRead = c.getInt(c.getColumnIndexOrThrow(COL_RL_IS_READ)) == 1;
                list.add(item);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public static List<ExtensionItem> cursorToExtensions(Cursor c) {
        List<ExtensionItem> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                ExtensionItem item = new ExtensionItem();
                item.id = c.getString(c.getColumnIndexOrThrow(COL_EXT_ID));
                item.name = c.getString(c.getColumnIndexOrThrow(COL_EXT_NAME));
                item.description = c.getString(c.getColumnIndexOrThrow(COL_EXT_DESC));
                item.version = c.getString(c.getColumnIndexOrThrow(COL_EXT_VERSION));
                item.jsContent = c.getString(c.getColumnIndexOrThrow(COL_EXT_JS));
                item.cssContent = c.getString(c.getColumnIndexOrThrow(COL_EXT_CSS));
                item.enabled = c.getInt(c.getColumnIndexOrThrow(COL_EXT_ENABLED)) == 1;
                item.installedAt = c.getLong(c.getColumnIndexOrThrow(COL_EXT_INSTALLED_AT));
                list.add(item);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public static List<StatEntry> cursorToStats(Cursor c) {
        List<StatEntry> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                StatEntry entry = new StatEntry();
                entry.date = c.getString(c.getColumnIndexOrThrow(COL_ST_DATE));
                entry.pagesVisited = c.getInt(c.getColumnIndexOrThrow(COL_ST_PAGES));
                entry.dataReceived = c.getLong(c.getColumnIndexOrThrow(COL_ST_DATA));
                entry.timeSpent = c.getLong(c.getColumnIndexOrThrow(COL_ST_TIME));
                list.add(entry);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public static StatEntry cursorToStatEntry(Cursor c) {
        try {
            if (c.moveToFirst()) {
                StatEntry entry = new StatEntry();
                entry.date = c.getString(c.getColumnIndexOrThrow(COL_ST_DATE));
                entry.pagesVisited = c.getInt(c.getColumnIndexOrThrow(COL_ST_PAGES));
                entry.dataReceived = c.getLong(c.getColumnIndexOrThrow(COL_ST_DATA));
                entry.timeSpent = c.getLong(c.getColumnIndexOrThrow(COL_ST_TIME));
                return entry;
            }
        } finally {
            c.close();
        }
        return null;
    }

    public static List<PasswordEntry> cursorToPasswords(Cursor c) {
        List<PasswordEntry> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                PasswordEntry entry = new PasswordEntry();
                entry.id = c.getLong(c.getColumnIndexOrThrow(COL_PW_ID));
                entry.domain = c.getString(c.getColumnIndexOrThrow(COL_PW_DOMAIN));
                entry.username = c.getString(c.getColumnIndexOrThrow(COL_PW_USERNAME));
                entry.password = c.getString(c.getColumnIndexOrThrow(COL_PW_PASSWORD));
                entry.savedAt = c.getLong(c.getColumnIndexOrThrow(COL_PW_SAVED_AT));
                list.add(entry);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public static List<QuickLinkEntry> cursorToQuickLinks(Cursor c) {
        List<QuickLinkEntry> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                QuickLinkEntry entry = new QuickLinkEntry();
                entry.id = c.getLong(c.getColumnIndexOrThrow(COL_QL_ID));
                entry.title = c.getString(c.getColumnIndexOrThrow(COL_QL_TITLE));
                entry.url = c.getString(c.getColumnIndexOrThrow(COL_QL_URL));
                entry.position = c.getInt(c.getColumnIndexOrThrow(COL_QL_POSITION));
                list.add(entry);
            }
        } finally {
            c.close();
        }
        return list;
    }
}