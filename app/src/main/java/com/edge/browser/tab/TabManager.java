package com.edge.browser.tab;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TabManager {

    private static TabManager instance;
    private final List<TabItem> tabs;
    private final Stack<TabItem> closedTabs;
    private int currentTabIndex = -1;
    private TabChangeListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface TabChangeListener {
        void onTabAdded(TabItem tab);
        void onTabRemoved(TabItem tab);
        void onTabSwitched(TabItem tab, int index);
        void onTabUpdated(TabItem tab);
    }

    private TabManager() {
        tabs = new ArrayList<>();
        closedTabs = new Stack<>();
    }

    public static synchronized TabManager getInstance() {
        if (instance == null) {
            instance = new TabManager();
        }
        return instance;
    }

    public void setListener(TabChangeListener listener) {
        this.listener = listener;
    }

    public TabItem addTab(String title, String url) {
        TabItem tab = new TabItem(title, url);
        tabs.add(tab);
        currentTabIndex = tabs.size() - 1;
        if (listener != null) listener.onTabAdded(tab);
        return tab;
    }

    public TabItem addTab(TabItem tab) {
        tabs.add(tab);
        currentTabIndex = tabs.size() - 1;
        if (listener != null) listener.onTabAdded(tab);
        return tab;
    }

    public void removeTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            TabItem removed = tabs.remove(index);
            if (removed != null && !removed.isIncognito()) {
                closedTabs.push(removed);
            }
            if (listener != null) listener.onTabRemoved(removed);

            if (tabs.isEmpty()) {
                currentTabIndex = -1;
                addTab("新标签页", "about:blank");
            } else if (currentTabIndex >= tabs.size()) {
                currentTabIndex = tabs.size() - 1;
            }
            if (listener != null && currentTabIndex >= 0) {
                listener.onTabSwitched(tabs.get(currentTabIndex), currentTabIndex);
            }
        }
    }

    public void removeTab(TabItem tab) {
        int index = tabs.indexOf(tab);
        if (index >= 0) removeTab(index);
    }

    public void switchToTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            currentTabIndex = index;
            TabItem tab = tabs.get(index);
            tab.touch();
            if (listener != null) listener.onTabSwitched(tab, index);
        }
    }

    public TabItem getCurrentTab() {
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            return tabs.get(currentTabIndex);
        }
        return null;
    }

    public int getCurrentTabIndex() {
        return currentTabIndex;
    }

    public List<TabItem> getAllTabs() {
        return new ArrayList<>(tabs);
    }

    public int getTabCount() {
        return tabs.size();
    }

    public TabItem getTabAt(int index) {
        if (index >= 0 && index < tabs.size()) {
            return tabs.get(index);
        }
        return null;
    }

    public void updateTab(TabItem tab) {
        if (listener != null) listener.onTabUpdated(tab);
    }

    public TabItem restoreClosedTab() {
        if (!closedTabs.isEmpty()) {
            TabItem restored = closedTabs.pop();
            tabs.add(restored);
            currentTabIndex = tabs.size() - 1;
            if (listener != null) {
                listener.onTabAdded(restored);
                listener.onTabSwitched(restored, currentTabIndex);
            }
            return restored;
        }
        return null;
    }

    public List<TabItem> getClosedTabs() {
        return new ArrayList<>(closedTabs);
    }

    public void pinTab(int index) {
        TabItem tab = getTabAt(index);
        if (tab != null) {
            tab.setPinned(!tab.isPinned());
            if (listener != null) listener.onTabUpdated(tab);
        }
    }

    public void muteTab(int index) {
        TabItem tab = getTabAt(index);
        if (tab != null) {
            tab.setMuted(!tab.isMuted());
            if (listener != null) listener.onTabUpdated(tab);
        }
    }

    public void closeAllTabs() {
        List<TabItem> toRemove = new ArrayList<>(tabs);
        for (TabItem tab : toRemove) {
            if (!tab.isPinned()) {
                tabs.remove(tab);
                if (listener != null) listener.onTabRemoved(tab);
            }
        }
        if (tabs.isEmpty()) {
            addTab("新标签页", "about:blank");
        }
    }

    public void saveAllTabs() {
        // Save all tab URLs to bookmarks
    }

    public List<TabItem> getPinnedTabs() {
        List<TabItem> pinned = new ArrayList<>();
        for (TabItem tab : tabs) {
            if (tab.isPinned()) pinned.add(tab);
        }
        return pinned;
    }

    public List<TabItem> getTabsByGroup(String groupId) {
        List<TabItem> group = new ArrayList<>();
        for (TabItem tab : tabs) {
            if (groupId != null && groupId.equals(tab.getGroupId())) {
                group.add(tab);
            }
        }
        return group;
    }

    public void moveTab(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < tabs.size() &&
                toIndex >= 0 && toIndex < tabs.size()) {
            TabItem tab = tabs.remove(fromIndex);
            tabs.add(toIndex, tab);
            if (currentTabIndex == fromIndex) {
                currentTabIndex = toIndex;
            }
        }
    }
}