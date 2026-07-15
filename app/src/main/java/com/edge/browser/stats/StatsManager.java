package com.edge.browser.stats;

import android.content.Context;
import android.database.Cursor;

import com.edge.browser.data.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsManager {

    private static StatsManager instance;
    private final DatabaseHelper db;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private StatsManager(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static synchronized StatsManager getInstance(Context context) {
        if (instance == null) {
            instance = new StatsManager(context.getApplicationContext());
        }
        return instance;
    }

    public void recordPageVisit() {
        String today = dateFormat.format(new Date());
        DatabaseHelper.StatEntry summary = getTodaySummary();
        if (summary != null) {
            db.addStatEntry(today, summary.pagesVisited + 1, summary.dataReceived, summary.timeSpent);
        } else {
            db.addStatEntry(today, 1, 0, 0);
        }
    }

    public void recordDataReceived(long bytes) {
        String today = dateFormat.format(new Date());
        DatabaseHelper.StatEntry summary = getTodaySummary();
        if (summary != null) {
            db.addStatEntry(today, summary.pagesVisited, summary.dataReceived + bytes, summary.timeSpent);
        } else {
            db.addStatEntry(today, 0, bytes, 0);
        }
    }

    public void recordTimeSpent(long ms) {
        String today = dateFormat.format(new Date());
        DatabaseHelper.StatEntry summary = getTodaySummary();
        if (summary != null) {
            db.addStatEntry(today, summary.pagesVisited, summary.dataReceived, summary.timeSpent + ms);
        } else {
            db.addStatEntry(today, 0, 0, ms);
        }
    }

    public List<DatabaseHelper.StatEntry> getStats() {
        return DatabaseHelper.cursorToStats(db.getStats());
    }

    public DatabaseHelper.StatEntry getTodaySummary() {
        String today = dateFormat.format(new Date());
        Cursor c = db.getReadableDatabase().query(
                DatabaseHelper.TABLE_STATS, null,
                DatabaseHelper.COL_ST_DATE + "=?", new String[]{today},
                null, null, null);
        return DatabaseHelper.cursorToStatEntry(c);
    }
}