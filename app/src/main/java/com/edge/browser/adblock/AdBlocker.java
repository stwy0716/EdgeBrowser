package com.edge.browser.adblock;

import com.edge.browser.data.DatabaseHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AdBlocker {

    private static final String PREF_AD_BLOCK = "ad_block_enabled";
    private static final Set<String> AD_HOSTS = new HashSet<>(Arrays.asList(
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "google-analytics.com", "googletagmanager.com", "googletagservices.com",
            "adservice.google.com", "pagead2.googlesyndication.com",
            "adsrvr.org", "adnxs.com", "criteo.com", "criteo.net",
            "outbrain.com", "taboola.com", "revcontent.com",
            "amazon-adsystem.com", "media.net", "adzerk.net",
            "rubiconproject.com", "pubmatic.com", "openx.net",
            "casalemedia.com", "indexww.com", "appnexus.com",
            "advertising.com", "adsafeprotected.com", "moatads.com",
            "scorecardresearch.com", "quantserve.com", "chartbeat.com",
            "hotjar.com", "mouseflow.com", "fullstory.com",
            "addthis.com", "sharethis.com", "disqus.com",
            "facebook.com/plugins", "facebook.com/tr",
            "baidu.com/baidu.php", "cnzz.com", "umeng.com",
            "p4p.1688.com", "tanx.com", "allyes.com"
    ));

    private static final Set<String> AD_PATH_PATTERNS = new HashSet<>(Arrays.asList(
            "/ads/", "/ad/", "/banner/", "/popup/", "/popunder/",
            "/sponsor/", "/promo/", "/track/", "/analytics/",
            "/pixel", "/impression", "/click?",
            "googleadservices", "googlesyndication"
    ));

    private static AdBlocker instance;
    private boolean enabled = true;

    private AdBlocker() {}

    public static synchronized AdBlocker getInstance() {
        if (instance == null) instance = new AdBlocker();
        return instance;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void loadState(DatabaseHelper db) {
        String val = db.getSetting(PREF_AD_BLOCK, "true");
        this.enabled = "true".equals(val);
    }

    public void saveState(DatabaseHelper db) {
        db.setSetting(PREF_AD_BLOCK, enabled ? "true" : "false");
    }

    public boolean shouldBlock(String url) {
        if (!enabled || url == null) return false;

        String lower = url.toLowerCase();

        // 检查域名
        for (String host : AD_HOSTS) {
            if (lower.contains(host)) return true;
        }

        // 检查路径模式
        for (String pattern : AD_PATH_PATTERNS) {
            if (lower.contains(pattern)) return true;
        }

        return false;
    }
}