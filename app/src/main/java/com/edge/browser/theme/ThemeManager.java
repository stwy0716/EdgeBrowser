package com.edge.browser.theme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    private static ThemeManager instance;
    private SharedPreferences prefs;

    public enum ThemeMode {
        LIGHT, DARK, SYSTEM
    }

    public enum NewTabLayout {
        FOCUSED,    // Minimal blank
        INSPIRATIONAL, // Wallpaper + news
        INFORMATIONAL  // News feed
    }

    private ThemeMode currentTheme = ThemeMode.SYSTEM;
    private NewTabLayout newTabLayout = NewTabLayout.FOCUSED;
    private boolean showNewsFeed = false;
    private boolean showTitleBar = true;
    private boolean showBookmarkBar = true;
    private boolean showSidebar = true;
    private float fontScale = 1.0f;
    private float pageZoom = 1.0f;
    private String customWallpaperUri = null;

    private ThemeManager() {}

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void init(Context context) {
        prefs = context.getSharedPreferences("edge_theme", Context.MODE_PRIVATE);
        loadSettings();
    }

    private void loadSettings() {
        if (prefs == null) return;
        String theme = prefs.getString("theme_mode", "SYSTEM");
        currentTheme = ThemeMode.valueOf(theme);
        String layout = prefs.getString("new_tab_layout", "FOCUSED");
        newTabLayout = NewTabLayout.valueOf(layout);
        showNewsFeed = prefs.getBoolean("show_news", false);
        showTitleBar = prefs.getBoolean("show_title_bar", true);
        showBookmarkBar = prefs.getBoolean("show_bookmark_bar", true);
        showSidebar = prefs.getBoolean("show_sidebar", true);
        fontScale = prefs.getFloat("font_scale", 1.0f);
        pageZoom = prefs.getFloat("page_zoom", 1.0f);
        customWallpaperUri = prefs.getString("wallpaper_uri", null);
    }

    public void applyTheme(Activity activity) {
        switch (currentTheme) {
            case LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public boolean isDarkMode(Context context) {
        if (currentTheme == ThemeMode.DARK) return true;
        if (currentTheme == ThemeMode.SYSTEM) {
            int nightMode = context.getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK;
            return nightMode == Configuration.UI_MODE_NIGHT_YES;
        }
        return false;
    }

    // Getters and Setters
    public ThemeMode getCurrentTheme() { return currentTheme; }
    public void setCurrentTheme(ThemeMode theme) {
        this.currentTheme = theme;
        if (prefs != null) prefs.edit().putString("theme_mode", theme.name()).apply();
    }

    public NewTabLayout getNewTabLayout() { return newTabLayout; }
    public void setNewTabLayout(NewTabLayout layout) {
        this.newTabLayout = layout;
        if (prefs != null) prefs.edit().putString("new_tab_layout", layout.name()).apply();
    }

    public boolean isShowNewsFeed() { return showNewsFeed; }
    public void setShowNewsFeed(boolean show) {
        this.showNewsFeed = show;
        if (prefs != null) prefs.edit().putBoolean("show_news", show).apply();
    }

    public boolean isShowTitleBar() { return showTitleBar; }
    public void setShowTitleBar(boolean show) {
        this.showTitleBar = show;
        if (prefs != null) prefs.edit().putBoolean("show_title_bar", show).apply();
    }

    public boolean isShowBookmarkBar() { return showBookmarkBar; }
    public void setShowBookmarkBar(boolean show) {
        this.showBookmarkBar = show;
        if (prefs != null) prefs.edit().putBoolean("show_bookmark_bar", show).apply();
    }

    public boolean isShowSidebar() { return showSidebar; }
    public void setShowSidebar(boolean show) {
        this.showSidebar = show;
        if (prefs != null) prefs.edit().putBoolean("show_sidebar", show).apply();
    }

    public float getFontScale() { return fontScale; }
    public void setFontScale(float scale) {
        this.fontScale = scale;
        if (prefs != null) prefs.edit().putFloat("font_scale", scale).apply();
    }

    public float getPageZoom() { return pageZoom; }
    public void setPageZoom(float zoom) {
        this.pageZoom = zoom;
        if (prefs != null) prefs.edit().putFloat("page_zoom", zoom).apply();
    }

    public String getCustomWallpaperUri() { return customWallpaperUri; }
    public void setCustomWallpaperUri(String uri) {
        this.customWallpaperUri = uri;
        if (prefs != null) prefs.edit().putString("wallpaper_uri", uri).apply();
    }
}