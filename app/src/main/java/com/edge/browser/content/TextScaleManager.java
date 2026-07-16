package com.edge.browser.content;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TextScaleManager {

    private static final String PREF_NAME = "edge_text_scale";
    private static final String KEY_DEFAULT_SCALE = "default_scale";
    private static final String KEY_SITE_SCALES = "site_scales";

    private static TextScaleManager instance;
    private final SharedPreferences prefs;
    private int defaultScale;
    private final Map<String, Integer> siteScales;

    private TextScaleManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.siteScales = new HashMap<>();
        loadSettings();
    }

    public static synchronized TextScaleManager getInstance(Context context) {
        if (instance == null) {
            instance = new TextScaleManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadSettings() {
        defaultScale = prefs.getInt(KEY_DEFAULT_SCALE, 100);
        String json = prefs.getString(KEY_SITE_SCALES, "{}");
        try {
            JSONObject obj = new JSONObject(json);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                siteScales.put(key, obj.getInt(key));
            }
        } catch (JSONException e) {
            // Reset to empty if JSON is corrupted
            siteScales.clear();
        }
    }

    public void setDefaultScale(int scale) {
        this.defaultScale = Math.max(50, Math.min(200, scale));
        prefs.edit().putInt(KEY_DEFAULT_SCALE, defaultScale).apply();
    }

    public int getDefaultScale() {
        return defaultScale;
    }

    public void setSiteScale(String domain, int scale) {
        if (domain == null || domain.isEmpty()) {
            return;
        }
        int clamped = Math.max(50, Math.min(200, scale));
        siteScales.put(domain.toLowerCase(), clamped);
        saveSiteScales();
    }

    public int getSiteScale(String domain) {
        if (domain == null) {
            return defaultScale;
        }
        Integer scale = siteScales.get(domain.toLowerCase());
        return scale != null ? scale : defaultScale;
    }

    public void resetSiteScale(String domain) {
        if (domain == null) {
            return;
        }
        siteScales.remove(domain.toLowerCase());
        saveSiteScales();
    }

    public void resetAll() {
        siteScales.clear();
        saveSiteScales();
    }

    private void saveSiteScales() {
        JSONObject obj = new JSONObject(siteScales);
        prefs.edit().putString(KEY_SITE_SCALES, obj.toString()).apply();
    }
}