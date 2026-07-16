package com.edge.browser.security;

import android.content.Context;

import com.edge.browser.BrowserLogger;
import com.edge.browser.data.DatabaseHelper;

public class HttpsOnlyMode {

    private static final String TAG = "HttpsOnlyMode";
    private static final String KEY_HTTPS_ONLY_ENABLED = "https_only_enabled";

    private static HttpsOnlyMode instance;
    private final DatabaseHelper db;
    private boolean enabled;

    private HttpsOnlyMode(Context context) {
        this.db = DatabaseHelper.getInstance(context);
        loadState();
    }

    public static synchronized HttpsOnlyMode getInstance(Context context) {
        if (instance == null) {
            instance = new HttpsOnlyMode(context.getApplicationContext());
        }
        return instance;
    }

    public void loadState() {
        loadState(db);
    }

    public void loadState(DatabaseHelper db) {
        String value = db.getSetting(KEY_HTTPS_ONLY_ENABLED, "false");
        enabled = Boolean.parseBoolean(value);
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "HTTPS-Only mode loaded: " + enabled);
    }

    public void saveState() {
        saveState(db);
    }

    public void saveState(DatabaseHelper db) {
        db.setSetting(KEY_HTTPS_ONLY_ENABLED, String.valueOf(enabled));
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "HTTPS-Only mode saved: " + enabled);
    }

    public String enforceHttps(String url) {
        if (enabled && url != null && url.startsWith("http://")) {
            String upgraded = "https://" + url.substring(7);
            BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                    "Upgraded to HTTPS: " + url + " -> " + upgraded);
            return upgraded;
        }
        return url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveState();
    }
}