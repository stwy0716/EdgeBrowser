package com.edge.browser.content;

import android.content.Context;
import android.content.SharedPreferences;

import com.edge.browser.data.DatabaseHelper;

import java.util.HashSet;
import java.util.Set;

public class DataSaverManager {

    private static final String PREF_NAME = "edge_data_saver";
    private static final String KEY_WHITELIST = "whitelist_domains";
    private static final String KEY_ENABLED = "data_saver_enabled";

    private static DataSaverManager instance;
    private final SharedPreferences prefs;
    private final DatabaseHelper db;
    private boolean enabled;
    private final Set<String> whitelist;

    private DataSaverManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.db = DatabaseHelper.getInstance(context);
        this.whitelist = new HashSet<>(prefs.getStringSet(KEY_WHITELIST, new HashSet<>()));
        loadState();
    }

    public static synchronized DataSaverManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataSaverManager(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveState();
    }

    public void loadState() {
        String value = db.getSetting(KEY_ENABLED, "false");
        this.enabled = "true".equals(value);
    }

    public void saveState() {
        db.setSetting(KEY_ENABLED, enabled ? "true" : "false");
    }

    public boolean shouldBlockImage(String url, String pageUrl) {
        if (!enabled || url == null) {
            return false;
        }
        if (pageUrl != null && isWhitelisted(extractDomain(pageUrl))) {
            return false;
        }
        return true;
    }

    public void addWhitelist(String domain) {
        if (domain == null || domain.isEmpty()) {
            return;
        }
        whitelist.add(domain.toLowerCase());
        saveWhitelist();
    }

    public void removeWhitelist(String domain) {
        if (domain == null) {
            return;
        }
        whitelist.remove(domain.toLowerCase());
        saveWhitelist();
    }

    public boolean isWhitelisted(String domain) {
        if (domain == null) {
            return false;
        }
        return whitelist.contains(domain.toLowerCase());
    }

    private void saveWhitelist() {
        prefs.edit().putStringSet(KEY_WHITELIST, new HashSet<>(whitelist)).apply();
    }

    private String extractDomain(String url) {
        if (url == null) {
            return null;
        }
        try {
            String host = url;
            if (url.startsWith("http://") || url.startsWith("https://")) {
                int start = url.indexOf("://") + 3;
                int end = url.indexOf('/', start);
                host = end > 0 ? url.substring(start, end) : url.substring(start);
            }
            int colon = host.indexOf(':');
            if (colon > 0) {
                host = host.substring(0, colon);
            }
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host;
        } catch (Exception e) {
            return null;
        }
    }
}