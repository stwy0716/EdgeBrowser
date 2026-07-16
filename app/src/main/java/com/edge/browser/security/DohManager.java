package com.edge.browser.security;

import android.content.Context;

import com.edge.browser.BrowserLogger;
import com.edge.browser.data.DatabaseHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DohManager {

    private static final String TAG = "DohManager";
    private static final String KEY_DOH_ENABLED = "doh_enabled";
    private static final String DOH_URL = "https://dns.google/resolve?name=";
    private static final String DOH_TYPE = "&type=A";

    private static DohManager instance;
    private final DatabaseHelper db;
    private final OkHttpClient client;
    private boolean enabled;

    private DohManager(Context context) {
        this.db = DatabaseHelper.getInstance(context);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        loadState();
    }

    public static synchronized DohManager getInstance(Context context) {
        if (instance == null) {
            instance = new DohManager(context.getApplicationContext());
        }
        return instance;
    }

    public void loadState() {
        loadState(db);
    }

    public void loadState(DatabaseHelper db) {
        String value = db.getSetting(KEY_DOH_ENABLED, "false");
        enabled = Boolean.parseBoolean(value);
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "DoH loaded: " + enabled);
    }

    public void saveState() {
        saveState(db);
    }

    public void saveState(DatabaseHelper db) {
        db.setSetting(KEY_DOH_ENABLED, String.valueOf(enabled));
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "DoH saved: " + enabled);
    }

    public String resolve(String hostname) {
        if (hostname == null || hostname.isEmpty()) {
            return null;
        }

        String url = DOH_URL + hostname + DOH_TYPE;
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/dns-json")
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                JSONArray answers = json.optJSONArray("Answer");
                if (answers != null && answers.length() > 0) {
                    JSONObject firstAnswer = answers.getJSONObject(0);
                    String ip = firstAnswer.optString("data");
                    BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                            "DoH resolved: " + hostname + " -> " + ip);
                    return ip;
                }
            }
        } catch (IOException e) {
            BrowserLogger.getInstance().e(TAG, BrowserLogger.LogCategory.PRIVACY,
                    "DoH resolve failed for: " + hostname, e);
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, BrowserLogger.LogCategory.PRIVACY,
                    "DoH JSON parse error for: " + hostname, e);
        }

        return null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveState();
    }
}