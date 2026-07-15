package com.edge.browser.reading;

import android.content.Context;

import com.edge.browser.data.DatabaseHelper;

import java.util.List;

public class ReadingListManager {

    private static ReadingListManager instance;
    private final DatabaseHelper db;

    private ReadingListManager(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static synchronized ReadingListManager getInstance(Context context) {
        if (instance == null) {
            instance = new ReadingListManager(context.getApplicationContext());
        }
        return instance;
    }

    public long addItem(String title, String url, String savedPath) {
        return db.addReadingItem(title, url, savedPath);
    }

    public List<DatabaseHelper.ReadingItem> getItems() {
        return DatabaseHelper.cursorToReadingList(db.getReadingList());
    }

    public void removeItem(long id) {
        db.removeReadingItem(id);
    }

    public void markAsRead(long id) {
        db.markReadingItemRead(id);
    }
}