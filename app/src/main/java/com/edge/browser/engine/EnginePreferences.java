package com.edge.browser.engine;

import com.edge.browser.data.DatabaseHelper;

public class EnginePreferences {

    private static final String PREF_ENGINE = "browser_engine";

    public enum EngineType {
        GECKO("Gecko (Firefox)"),
        CHROMIUM("Chromium");

        private final String displayName;

        EngineType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static EnginePreferences instance;
    private EngineType currentEngine = EngineType.CHROMIUM;

    private EnginePreferences() {}

    public static synchronized EnginePreferences getInstance() {
        if (instance == null) instance = new EnginePreferences();
        return instance;
    }

    public EngineType getCurrentEngine() {
        return currentEngine;
    }

    public void setCurrentEngine(EngineType type) {
        this.currentEngine = type;
    }

    public void loadState(DatabaseHelper db) {
        String val = db.getSetting(PREF_ENGINE, EngineType.CHROMIUM.name());
        try {
            currentEngine = EngineType.valueOf(val);
        } catch (IllegalArgumentException e) {
            currentEngine = EngineType.CHROMIUM;
        }
    }

    public void saveState(DatabaseHelper db) {
        db.setSetting(PREF_ENGINE, currentEngine.name());
    }
}