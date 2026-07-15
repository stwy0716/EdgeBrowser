package com.edge.browser.tab;

import java.util.UUID;

public class TabItem {
    private String id;
    private String title;
    private String url;
    private boolean isPinned;
    private boolean isSleeping;
    private boolean isMuted;
    private long lastAccessTime;
    private int groupColor;
    private String groupId;
    private boolean isIncognito;

    public TabItem(String title, String url) {
        this.id = UUID.randomUUID().toString();
        this.title = title != null ? title : "新标签页";
        this.url = url != null ? url : "about:blank";
        this.isPinned = false;
        this.isSleeping = false;
        this.isMuted = false;
        this.lastAccessTime = System.currentTimeMillis();
        this.groupColor = -1;
        this.groupId = null;
        this.isIncognito = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public boolean isSleeping() { return isSleeping; }
    public void setSleeping(boolean sleeping) { isSleeping = sleeping; }

    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { isMuted = muted; }

    public long getLastAccessTime() { return lastAccessTime; }
    public void setLastAccessTime(long lastAccessTime) { this.lastAccessTime = lastAccessTime; }

    public int getGroupColor() { return groupColor; }
    public void setGroupColor(int groupColor) { this.groupColor = groupColor; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public boolean isIncognito() { return isIncognito; }
    public void setIncognito(boolean incognito) { isIncognito = incognito; }

    public void touch() {
        this.lastAccessTime = System.currentTimeMillis();
        this.isSleeping = false;
    }
}