package com.edge.browser.ui;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ToolbarCustomizer {

    private static final String PREFS_NAME = "edge_toolbar_customizer";
    private static final String KEY_BUTTONS = "toolbar_buttons";
    private static final String SEPARATOR = ",";

    public static final String BUTTON_BACK = "back";
    public static final String BUTTON_FORWARD = "forward";
    public static final String BUTTON_HOME = "home";
    public static final String BUTTON_TABS = "tabs";
    public static final String BUTTON_MENU = "menu";

    private static final List<String> DEFAULT_BUTTONS = Arrays.asList(
            BUTTON_BACK, BUTTON_FORWARD, BUTTON_HOME, BUTTON_TABS, BUTTON_MENU
    );

    private static ToolbarCustomizer instance;
    private final SharedPreferences prefs;
    private List<String> cachedButtons;

    private ToolbarCustomizer(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadFromPrefs();
    }

    public static synchronized ToolbarCustomizer getInstance(Context context) {
        if (instance == null) {
            instance = new ToolbarCustomizer(context.getApplicationContext());
        }
        return instance;
    }

    private void loadFromPrefs() {
        String stored = prefs.getString(KEY_BUTTONS, null);
        if (stored != null && !stored.isEmpty()) {
            String[] parts = stored.split(SEPARATOR);
            cachedButtons = new ArrayList<>(Arrays.asList(parts));
        } else {
            cachedButtons = new ArrayList<>(DEFAULT_BUTTONS);
        }
    }

    public List<String> getToolbarButtons() {
        return new ArrayList<>(cachedButtons);
    }

    public void setToolbarButtons(List<String> buttonIds) {
        if (buttonIds == null || buttonIds.isEmpty()) {
            return;
        }
        cachedButtons = new ArrayList<>(buttonIds);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cachedButtons.size(); i++) {
            if (i > 0) sb.append(SEPARATOR);
            sb.append(cachedButtons.get(i));
        }
        prefs.edit().putString(KEY_BUTTONS, sb.toString()).apply();
    }

    public void resetToDefault() {
        cachedButtons = new ArrayList<>(DEFAULT_BUTTONS);
        prefs.edit().remove(KEY_BUTTONS).apply();
    }

    public boolean isButtonVisible(String buttonId) {
        return cachedButtons.contains(buttonId);
    }
}