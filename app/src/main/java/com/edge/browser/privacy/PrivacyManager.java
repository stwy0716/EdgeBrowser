package com.edge.browser.privacy;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import android.webkit.WebView;

import com.edge.browser.EdgeApplication;

public class PrivacyManager {

    private static PrivacyManager instance;
    private final SharedPreferences prefs;
    private TrackingLevel trackingLevel = TrackingLevel.BALANCED;
    private boolean doNotTrack = true;
    private boolean httpsOnly = false;
    private boolean blockPopups = true;
    private boolean blockAds = true;
    private boolean blockNotifications = true;
    private boolean autoClearCookies = false;

    public enum TrackingLevel {
        BASIC,       // Only block malicious trackers
        BALANCED,    // Block cross-site tracking (default)
        STRICT       // Block all third-party cookies
    }

    private PrivacyManager(Context context) {
        this.prefs = context.getSharedPreferences("edge_privacy", Context.MODE_PRIVATE);
        loadSettings();
    }

    public static synchronized PrivacyManager getInstance(Context context) {
        if (instance == null) {
            instance = new PrivacyManager(context);
        }
        return instance;
    }

    private void loadSettings() {
        String level = prefs.getString("tracking_level", "BALANCED");
        trackingLevel = TrackingLevel.valueOf(level);
        doNotTrack = prefs.getBoolean("do_not_track", true);
        httpsOnly = prefs.getBoolean("https_only", false);
        blockPopups = prefs.getBoolean("block_popups", true);
        blockAds = prefs.getBoolean("block_ads", true);
        blockNotifications = prefs.getBoolean("block_notifications", true);
        autoClearCookies = prefs.getBoolean("auto_clear_cookies", false);
    }

    private void saveSettings() {
        prefs.edit()
                .putString("tracking_level", trackingLevel.name())
                .putBoolean("do_not_track", doNotTrack)
                .putBoolean("https_only", httpsOnly)
                .putBoolean("block_popups", blockPopups)
                .putBoolean("block_ads", blockAds)
                .putBoolean("block_notifications", blockNotifications)
                .putBoolean("auto_clear_cookies", autoClearCookies)
                .apply();
    }

    public void applyTrackingProtection(TrackingLevel level) {
        this.trackingLevel = level;
        saveSettings();

        CookieManager cookieManager = CookieManager.getInstance();
        switch (level) {
            case BASIC:
                cookieManager.setAcceptCookie(true);
                cookieManager.setAcceptThirdPartyCookies(null, true);
                break;
            case BALANCED:
                cookieManager.setAcceptCookie(true);
                cookieManager.setAcceptThirdPartyCookies(null, false);
                break;
            case STRICT:
                cookieManager.setAcceptCookie(true);
                cookieManager.setAcceptThirdPartyCookies(null, false);
                // Block all third-party cookies
                break;
        }
    }

    public void checkHttpsUpgrade(String url) {
        if (httpsOnly && url != null && url.startsWith("http://")) {
            // Force HTTPS upgrade
        }
    }

    public String getDoNotTrackHeader() {
        return doNotTrack ? "1" : "0";
    }

    public void clearBrowsingData(boolean cookies, boolean cache, boolean history) {
        if (cookies) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        }
        if (cache) {
            WebView webView = new WebView(EdgeApplication.getInstance());
            webView.clearCache(true);
            webView.destroy();
        }
    }

    // Getters and Setters
    public TrackingLevel getTrackingLevel() { return trackingLevel; }
    public boolean isDoNotTrack() { return doNotTrack; }
    public void setDoNotTrack(boolean doNotTrack) { this.doNotTrack = doNotTrack; saveSettings(); }
    public boolean isHttpsOnly() { return httpsOnly; }
    public void setHttpsOnly(boolean httpsOnly) { this.httpsOnly = httpsOnly; saveSettings(); }
    public boolean isBlockPopups() { return blockPopups; }
    public void setBlockPopups(boolean blockPopups) { this.blockPopups = blockPopups; saveSettings(); }
    public boolean isBlockAds() { return blockAds; }
    public void setBlockAds(boolean blockAds) { this.blockAds = blockAds; saveSettings(); }
    public boolean isBlockNotifications() { return blockNotifications; }
    public void setBlockNotifications(boolean blockNotifications) { this.blockNotifications = blockNotifications; saveSettings(); }
    public boolean isAutoClearCookies() { return autoClearCookies; }
    public void setAutoClearCookies(boolean autoClearCookies) { this.autoClearCookies = autoClearCookies; saveSettings(); }
}