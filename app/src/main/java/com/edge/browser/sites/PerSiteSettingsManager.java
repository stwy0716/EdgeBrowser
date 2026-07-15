package com.edge.browser.sites;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.edge.browser.data.DatabaseHelper;

public class PerSiteSettingsManager {

    private static PerSiteSettingsManager instance;
    private final DatabaseHelper db;

    private PerSiteSettingsManager(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static synchronized PerSiteSettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new PerSiteSettingsManager(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper.SiteSettings getSettings(String domain) {
        Cursor c = db.getSiteSettings(domain);
        return DatabaseHelper.cursorToSiteSettings(c);
    }

    public void saveSettings(String domain, DatabaseHelper.SiteSettings settings) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_SS_DESKTOP_MODE, settings.desktopMode ? 1 : 0);
        cv.put(DatabaseHelper.COL_SS_JS_ENABLED, settings.javascriptEnabled ? 1 : 0);
        cv.put(DatabaseHelper.COL_SS_BLOCK_IMAGES, settings.blockImages ? 1 : 0);
        cv.put(DatabaseHelper.COL_SS_AD_BLOCK_OVERRIDE, settings.adBlockOverride ? 1 : 0);
        db.saveSiteSettings(domain, cv);
    }

    public boolean getDesktopMode(String domain) {
        DatabaseHelper.SiteSettings settings = getSettings(domain);
        return settings != null && settings.desktopMode;
    }

    public boolean getJavascriptEnabled(String domain) {
        DatabaseHelper.SiteSettings settings = getSettings(domain);
        return settings == null || settings.javascriptEnabled;
    }
}