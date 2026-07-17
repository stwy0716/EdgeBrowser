package com.edge.browser.history;

import android.content.Context;

import com.edge.browser.data.DatabaseHelper;

import java.util.List;

public class HistoryManager {

    private static HistoryManager instance;
    private final DatabaseHelper db;

    private HistoryManager(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static synchronized HistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryManager(context.getApplicationContext());
        }
        return instance;
    }

    public void addVisit(String title, String url) {
        if (url == null || url.isEmpty() || "about:blank".equals(url)) return;
        db.addHistory(title != null ? title : url, url);
    }

    public List<DatabaseHelper.HistoryEntry> getHistory() {
        return DatabaseHelper.cursorToHistory(db.getHistory());
    }

    public void clearHistory() {
        db.clearHistory();
    }

    public void removeEntry(long id) {
        db.removeHistory(id);
    }
}