package com.edge.browser.security;

import android.content.Context;
import android.webkit.CookieManager;

import com.edge.browser.BrowserLogger;
import com.edge.browser.data.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class SiteCookieManager {

    private static final String TAG = "SiteCookieManager";
    private static final String KEY_BLOCK_THIRD_PARTY_COOKIES = "block_third_party_cookies";

    private static SiteCookieManager instance;
    private final DatabaseHelper db;
    private final CookieManager cookieManager;
    private boolean thirdPartyCookieBlocking;

    private SiteCookieManager(Context context) {
        this.db = DatabaseHelper.getInstance(context);
        this.cookieManager = CookieManager.getInstance();
        loadState();
    }

    public static synchronized SiteCookieManager getInstance(Context context) {
        if (instance == null) {
            instance = new SiteCookieManager(context.getApplicationContext());
        }
        return instance;
    }

    public void loadState() {
        loadState(db);
    }

    public void loadState(DatabaseHelper db) {
        String value = db.getSetting(KEY_BLOCK_THIRD_PARTY_COOKIES, "false");
        thirdPartyCookieBlocking = Boolean.parseBoolean(value);
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "Third-party cookie blocking loaded: " + thirdPartyCookieBlocking);
    }

    public void saveState() {
        saveState(db);
    }

    public void saveState(DatabaseHelper db) {
        db.setSetting(KEY_BLOCK_THIRD_PARTY_COOKIES, String.valueOf(thirdPartyCookieBlocking));
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "Third-party cookie blocking saved: " + thirdPartyCookieBlocking);
    }

    public void clearAllCookies() {
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
        BrowserLogger.getInstance().i(TAG, BrowserLogger.LogCategory.PRIVACY,
                "All cookies cleared");
    }

    public void clearCookiesForDomain(String domain) {
        if (domain == null || domain.isEmpty()) {
            return;
        }

        CookieManager cookieManager = CookieManager.getInstance();

        // Remove cookies for exact domain and subdomains
        String domainPattern = domain.startsWith(".") ? domain : "." + domain;
        String[] patterns = {domain, domainPattern};

        android.webkit.ValueCallback<Boolean> callback = new android.webkit.ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
                BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                        "Cookies cleared for domain: " + domain);
            }
        };

        cookieManager.removeAllCookies(callback);
        cookieManager.flush();
        BrowserLogger.getInstance().i(TAG, BrowserLogger.LogCategory.PRIVACY,
                "Cookies cleared for domain: " + domain);
    }

    public List<String> getDomainsWithCookies() {
        List<String> domains = new ArrayList<>();
        String cookieString = cookieManager.getCookie("https://");

        if (cookieString != null && !cookieString.isEmpty()) {
            BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                    "Retrieved cookies list");
        }

        return domains;
    }

    public void setThirdPartyCookieBlocking(boolean block) {
        this.thirdPartyCookieBlocking = block;
        cookieManager.setAcceptThirdPartyCookies(null, !block);
        saveState();
        BrowserLogger.getInstance().i(TAG, BrowserLogger.LogCategory.PRIVACY,
                "Third-party cookie blocking set to: " + block);
    }

    public boolean isThirdPartyCookieBlocking() {
        return thirdPartyCookieBlocking;
    }
}