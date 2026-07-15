package com.edge.browser.bookmark;

import android.content.Context;

import com.edge.browser.data.DatabaseHelper;

import java.util.List;

public class BookmarkManager {

    private static BookmarkManager instance;
    private final DatabaseHelper db;

    private BookmarkManager(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static synchronized BookmarkManager getInstance(Context context) {
        if (instance == null) {
            instance = new BookmarkManager(context.getApplicationContext());
        }
        return instance;
    }

    public long addBookmark(String title, String url) {
        return addBookmark(title, url, null);
    }

    public long addBookmark(String title, String url, String iconUrl) {
        return db.addBookmark(title, url, iconUrl);
    }

    public void removeBookmark(long id) {
        db.removeBookmark(id);
    }

    public void removeBookmarkByUrl(String url) {
        // 遍历删除
        List<DatabaseHelper.BookmarkEntry> list = getBookmarks();
        for (DatabaseHelper.BookmarkEntry b : list) {
            if (b.url.equals(url)) {
                db.removeBookmark(b.id);
                break;
            }
        }
    }

    public List<DatabaseHelper.BookmarkEntry> getBookmarks() {
        return DatabaseHelper.cursorToBookmarks(db.getBookmarks());
    }

    public boolean isBookmarked(String url) {
        return db.isBookmarked(url);
    }
}