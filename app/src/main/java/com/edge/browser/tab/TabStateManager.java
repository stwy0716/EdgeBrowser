package com.edge.browser.tab;

import android.content.Context;

import com.edge.browser.data.DatabaseHelper;

import java.util.List;

public class TabStateManager {

    private static TabStateManager instance;
    private final DatabaseHelper db;

    private TabStateManager(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static synchronized TabStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new TabStateManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveTabStates(List<TabItem> tabs) {
        db.clearTabStates();
        for (int i = 0; i < tabs.size(); i++) {
            TabItem t = tabs.get(i);
            db.saveTabState(i, t.getTitle(), t.getUrl(), t.isPinned());
        }
    }

    public List<DatabaseHelper.TabState> loadTabStates() {
        return DatabaseHelper.cursorToTabStates(db.getTabStates());
    }
}