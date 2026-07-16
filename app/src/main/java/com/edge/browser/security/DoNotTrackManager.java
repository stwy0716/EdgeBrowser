package com.edge.browser.security;

import android.content.Context;

import com.edge.browser.BrowserLogger;
import com.edge.browser.data.DatabaseHelper;

import java.util.HashMap;
import java.util.Map;

public class DoNotTrackManager {

    private static final String TAG = "DoNotTrackManager";
    private static final String KEY_DNT_ENABLED = "dnt_enabled";

    private static DoNotTrackManager instance;
    private final DatabaseHelper db;
    private boolean enabled;

    private DoNotTrackManager(Context context) {
        this.db = DatabaseHelper.getInstance(context);
        loadState();
    }

    public static synchronized DoNotTrackManager getInstance(Context context) {
        if (instance == null) {
            instance = new DoNotTrackManager(context.getApplicationContext());
        }
        return instance;
    }

    public void loadState() {
        loadState(db);
    }

    public void loadState(DatabaseHelper db) {
        String value = db.getSetting(KEY_DNT_ENABLED, "false");
        enabled = Boolean.parseBoolean(value);
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "DoNotTrack loaded: " + enabled);
    }

    public void saveState() {
        saveState(db);
    }

    public void saveState(DatabaseHelper db) {
        db.setSetting(KEY_DNT_ENABLED, String.valueOf(enabled));
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "DoNotTrack saved: " + enabled);
    }

    public Map<String, String> getDNTHeaders() {
        Map<String, String> headers = new HashMap<>();
        if (enabled) {
            headers.put("DNT", "1");
        }
        return headers;
    }

    public String getDNTJavaScript() {
        if (enabled) {
            return "Object.defineProperty(navigator, 'doNotTrack', {get: function(){return '1';}});";
        }
        return "";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveState();
    }
}