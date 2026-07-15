package com.edge.browser.quicklinks;

import android.content.Context;

import com.edge.browser.data.DatabaseHelper;

import java.util.List;

public class QuickLinkManager {

    private static QuickLinkManager instance;
    private final DatabaseHelper db;

    private QuickLinkManager(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static synchronized QuickLinkManager getInstance(Context context) {
        if (instance == null) {
            instance = new QuickLinkManager(context.getApplicationContext());
        }
        return instance;
    }

    public List<DatabaseHelper.QuickLinkEntry> getLinks() {
        return DatabaseHelper.cursorToQuickLinks(db.getQuickLinks());
    }

    public void saveLinks(List<DatabaseHelper.QuickLinkEntry> links) {
        db.saveQuickLinks(links);
    }

    public void addLink(String title, String url) {
        List<DatabaseHelper.QuickLinkEntry> links = getLinks();
        DatabaseHelper.QuickLinkEntry entry = new DatabaseHelper.QuickLinkEntry();
        entry.title = title;
        entry.url = url;
        entry.position = links.size();
        links.add(entry);
        saveLinks(links);
    }

    public void removeLink(long id) {
        List<DatabaseHelper.QuickLinkEntry> links = getLinks();
        for (int i = 0; i < links.size(); i++) {
            if (links.get(i).id == id) {
                links.remove(i);
                break;
            }
        }
        for (int i = 0; i < links.size(); i++) {
            links.get(i).position = i;
        }
        saveLinks(links);
    }
}