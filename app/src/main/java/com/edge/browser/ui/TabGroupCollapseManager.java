package com.edge.browser.ui;

import android.content.Context;
import android.content.SharedPreferences;

import com.edge.browser.tab.TabGroupManager;
import com.edge.browser.tab.TabItem;
import com.edge.browser.tab.TabManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TabGroupCollapseManager {

    private static final String PREFS_NAME = "edge_tab_group_collapse";
    private static final String KEY_COLLAPSED_GROUPS = "collapsed_groups";

    private static TabGroupCollapseManager instance;
    private final SharedPreferences prefs;
    private final TabGroupManager tabGroupManager;
    private final TabManager tabManager;
    private Set<String> collapsedGroups;

    private TabGroupCollapseManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        tabGroupManager = TabGroupManager.getInstance();
        tabManager = TabManager.getInstance();
        loadState();
    }

    public static synchronized TabGroupCollapseManager getInstance(Context context) {
        if (instance == null) {
            instance = new TabGroupCollapseManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadState() {
        collapsedGroups = new HashSet<>(
                prefs.getStringSet(KEY_COLLAPSED_GROUPS, new HashSet<>()));
    }

    private void saveState() {
        prefs.edit().putStringSet(KEY_COLLAPSED_GROUPS, collapsedGroups).apply();
    }

    public void toggleGroup(String groupId) {
        if (groupId == null) return;

        TabGroupManager.TabGroup group = tabGroupManager.getGroup(groupId);
        if (group == null) return;

        boolean newCollapsed = !group.isCollapsed();
        group.setCollapsed(newCollapsed);

        if (newCollapsed) {
            collapsedGroups.add(groupId);
        } else {
            collapsedGroups.remove(groupId);
        }
        saveState();
    }

    public boolean isGroupCollapsed(String groupId) {
        if (groupId == null) return false;
        return collapsedGroups.contains(groupId);
    }

    public int getVisibleTabCount() {
        List<TabItem> allTabs = tabManager.getAllTabs();
        int visibleCount = 0;

        for (TabItem tab : allTabs) {
            String groupId = tab.getGroupId();
            if (groupId != null && isGroupCollapsed(groupId)) {
                continue;
            }
            visibleCount++;
        }
        return visibleCount;
    }

    public void syncWithTabGroupManager() {
        List<TabGroupManager.TabGroup> groups = tabGroupManager.getAllGroups();
        for (TabGroupManager.TabGroup group : groups) {
            if (collapsedGroups.contains(group.getId())) {
                group.setCollapsed(true);
            } else {
                group.setCollapsed(false);
            }
        }
    }
}