package com.edge.browser.privacy;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import android.webkit.WebView;

import com.edge.browser.EdgeApplication;
import com.edge.browser.history.HistoryManager;
import com.edge.browser.password.PasswordManager;

public class ClearOnExitManager {

    private static ClearOnExitManager instance;
    private final SharedPreferences prefs;
    private static final String PREFS_NAME = "clear_on_exit";

    private boolean enabled;
    private boolean clearHistory;
    private boolean clearCookies;
    private boolean clearCache;
    private boolean clearPasswords;

    private ClearOnExitManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadSettings();
    }

    public static synchronized ClearOnExitManager getInstance(Context context) {
        if (instance == null) {
            instance = new ClearOnExitManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadSettings() {
        enabled = prefs.getBoolean("enabled", false);
        clearHistory = prefs.getBoolean("clear_history", false);
        clearCookies = prefs.getBoolean("clear_cookies", false);
        clearCache = prefs.getBoolean("clear_cache", false);
        clearPasswords = prefs.getBoolean("clear_passwords", false);
    }

    private void saveSettings() {
        prefs.edit()
                .putBoolean("enabled", enabled)
                .putBoolean("clear_history", clearHistory)
                .putBoolean("clear_cookies", clearCookies)
                .putBoolean("clear_cache", clearCache)
                .putBoolean("clear_passwords", clearPasswords)
                .apply();
    }

    public void executeClear(Context context) {
        if (!enabled) return;

        if (clearHistory) {
            try {
                HistoryManager.getInstance(context).clearHistory();
            } catch (Exception ignored) {}
        }

        if (clearCookies) {
            try {
                CookieManager.getInstance().removeAllCookies(null);
                CookieManager.getInstance().flush();
            } catch (Exception ignored) {}
        }

        if (clearCache) {
            try {
                EdgeApplication app = EdgeApplication.getInstance();
                if (app != null) {
                    WebView webView = new WebView(app);
                    webView.clearCache(true);
                    webView.destroy();
                }
            } catch (Exception ignored) {}
        }

        if (clearPasswords) {
            try {
            } catch (Exception ignored) {}
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; saveSettings(); }

    public boolean isClearHistory() { return clearHistory; }
    public void setClearHistory(boolean clearHistory) { this.clearHistory = clearHistory; saveSettings(); }

    public boolean isClearCookies() { return clearCookies; }
    public void setClearCookies(boolean clearCookies) { this.clearCookies = clearCookies; saveSettings(); }

    public boolean isClearCache() { return clearCache; }
    public void setClearCache(boolean clearCache) { this.clearCache = clearCache; saveSettings(); }

    public boolean isClearPasswords() { return clearPasswords; }
    public void setClearPasswords(boolean clearPasswords) { this.clearPasswords = clearPasswords; saveSettings(); }
}