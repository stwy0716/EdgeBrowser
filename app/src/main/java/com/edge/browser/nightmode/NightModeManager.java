package com.edge.browser.nightmode;

import com.edge.browser.data.DatabaseHelper;

public class NightModeManager {

    private static final String PREF_NIGHT_MODE = "night_mode_enabled";

    private static NightModeManager instance;
    private boolean enabled = false;

    private NightModeManager() {}

    public static synchronized NightModeManager getInstance() {
        if (instance == null) {
            instance = new NightModeManager();
        }
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void loadState(DatabaseHelper db) {
        if (db == null) return;
        String value = db.getSetting(PREF_NIGHT_MODE, "false");
        enabled = "true".equals(value);
    }

    public void saveState(DatabaseHelper db) {
        if (db == null) return;
        db.setSetting(PREF_NIGHT_MODE, enabled ? "true" : "false");
    }

    public String getDarkCSS() {
        return "html {"
                + "filter: invert(0.9) hue-rotate(180deg) !important;"
                + "background-color: #111 !important;"
                + "}"
                + "body {"
                + "background-color: #111 !important;"
                + "color: #ddd !important;"
                + "}"
                + "img, video, canvas, svg, [style*=\"background-image\"] {"
                + "filter: invert(1) hue-rotate(180deg) !important;"
                + "}"
                + "a { color: #6af !important; }"
                + "input, textarea, select {"
                + "background-color: #222 !important;"
                + "color: #eee !important;"
                + "border-color: #444 !important;"
                + "}"
                + "::-webkit-scrollbar { background-color: #222; }"
                + "::-webkit-scrollbar-thumb { background-color: #555; }";
    }

    public String getDarkJS() {
        return "javascript:(function() {"
                + "var styleId = 'edge-night-mode-style';"
                + "var existing = document.getElementById(styleId);"
                + "if (existing) {"
                + "  existing.remove();"
                + "  return;"
                + "}"
                + "var style = document.createElement('style');"
                + "style.id = styleId;"
                + "style.type = 'text/css';"
                + "style.textContent = '" + getDarkCSS().replace("'", "\\'") + "';"
                + "document.head.appendChild(style);"
                + "})()";
    }
}