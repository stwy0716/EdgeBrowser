package com.edge.browser.tab;

import java.util.*;

public class TabGroupManager {

    private static TabGroupManager instance;
    private final Map<String, TabGroup> groups;

    public static class TabGroup {
        private String id;
        private String name;
        private int color;
        private boolean isCollapsed;
        private List<String> tabIds;

        public TabGroup(String name, int color) {
            this.id = UUID.randomUUID().toString();
            this.name = name;
            this.color = color;
            this.isCollapsed = false;
            this.tabIds = new ArrayList<>();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getColor() { return color; }
        public void setColor(int color) { this.color = color; }
        public boolean isCollapsed() { return isCollapsed; }
        public void setCollapsed(boolean collapsed) { isCollapsed = collapsed; }
        public List<String> getTabIds() { return tabIds; }
        public void addTab(String tabId) { if (!tabIds.contains(tabId)) tabIds.add(tabId); }
        public void removeTab(String tabId) { tabIds.remove(tabId); }
        public int getTabCount() { return tabIds.size(); }
    }

    // Predefined group colors (matching Edge style)
    public static final int[] GROUP_COLORS = {
            0xFF0078D4, // Blue
            0xFFE81123, // Red
            0xFFFFB900, // Yellow
            0xFF10893E, // Green
            0xFF881798, // Purple
            0xFF0063B1, // Dark Blue
            0xFFE74856, // Pink
            0xFF00B7C3, // Teal
    };

    private TabGroupManager() {
        groups = new LinkedHashMap<>();
    }

    public static synchronized TabGroupManager getInstance() {
        if (instance == null) {
            instance = new TabGroupManager();
        }
        return instance;
    }

    public TabGroup createGroup(String name, int color) {
        TabGroup group = new TabGroup(name, color);
        groups.put(group.getId(), group);
        return group;
    }

    public void deleteGroup(String groupId) {
        groups.remove(groupId);
    }

    public TabGroup getGroup(String groupId) {
        return groups.get(groupId);
    }

    public List<TabGroup> getAllGroups() {
        return new ArrayList<>(groups.values());
    }

    public void addTabToGroup(String groupId, String tabId) {
        TabGroup group = groups.get(groupId);
        if (group != null) {
            group.addTab(tabId);
        }
    }

    public void removeTabFromGroup(String groupId, String tabId) {
        TabGroup group = groups.get(groupId);
        if (group != null) {
            group.removeTab(tabId);
            if (group.getTabCount() == 0) {
                groups.remove(groupId);
            }
        }
    }

    public void toggleGroupCollapse(String groupId) {
        TabGroup group = groups.get(groupId);
        if (group != null) {
            group.setCollapsed(!group.isCollapsed());
        }
    }
}