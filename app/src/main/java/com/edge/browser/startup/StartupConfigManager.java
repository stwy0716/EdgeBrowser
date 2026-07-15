package com.edge.browser.startup;

import android.content.Context;

import com.edge.browser.data.DatabaseHelper;

public class StartupConfigManager {

    private static final String KEY_STARTUP_MODE = "startup_mode";
    private static final String KEY_HOMEPAGE = "homepage";
    private static final String DEFAULT_STARTUP_MODE = "newtab";
    private static final String DEFAULT_HOMEPAGE = "https://www.bing.com";

    private static StartupConfigManager instance;
    private final DatabaseHelper db;

    private StartupConfigManager(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static synchronized StartupConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new StartupConfigManager(context.getApplicationContext());
        }
        return instance;
    }

    public String getStartupMode() {
        return db.getSetting(KEY_STARTUP_MODE, DEFAULT_STARTUP_MODE);
    }

    public void setStartupMode(String mode) {
        db.setSetting(KEY_STARTUP_MODE, mode);
    }

    public String getHomepage() {
        return db.getSetting(KEY_HOMEPAGE, DEFAULT_HOMEPAGE);
    }

    public void setHomepage(String url) {
        db.setSetting(KEY_HOMEPAGE, url);
    }
}