package com.edge.browser.security;

import android.content.Context;

import com.edge.browser.BrowserLogger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionCenter {

    private static final String TAG = "PermissionCenter";
    private static final String PERMISSIONS_FILE = "site_permissions.json";

    private static PermissionCenter instance;
    private final Context context;
    private final Gson gson;
    private final Map<String, SitePermission> permissionsMap;
    private final File permissionsFile;

    private PermissionCenter(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.permissionsMap = new HashMap<>();
        this.permissionsFile = new File(context.getFilesDir(), PERMISSIONS_FILE);
        loadPermissions();
    }

    public static synchronized PermissionCenter getInstance(Context context) {
        if (instance == null) {
            instance = new PermissionCenter(context);
        }
        return instance;
    }

    private void loadPermissions() {
        try {
            if (permissionsFile.exists()) {
                FileInputStream fis = new FileInputStream(permissionsFile);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                Type mapType = new TypeToken<Map<String, SitePermission>>(){}.getType();
                Map<String, SitePermission> loaded = gson.fromJson(sb.toString(), mapType);
                if (loaded != null) {
                    permissionsMap.putAll(loaded);
                }
                BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                        "Permissions loaded from file: " + permissionsMap.size() + " entries");
            }
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, BrowserLogger.LogCategory.PRIVACY,
                    "Failed to load permissions", e);
        }
    }

    private void savePermissions() {
        try {
            FileOutputStream fos = new FileOutputStream(permissionsFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            String json = gson.toJson(permissionsMap);
            writer.write(json);
            writer.flush();
            writer.close();
            BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                    "Permissions saved to file: " + permissionsMap.size() + " entries");
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, BrowserLogger.LogCategory.PRIVACY,
                    "Failed to save permissions", e);
        }
    }

    public SitePermission getPermissions(String domain) {
        SitePermission perm = permissionsMap.get(domain);
        if (perm == null) {
            perm = new SitePermission(domain);
            permissionsMap.put(domain, perm);
        }
        return perm;
    }

    public void setPermission(String domain, String type, boolean granted) {
        SitePermission perm = permissionsMap.get(domain);
        if (perm == null) {
            perm = new SitePermission(domain);
            permissionsMap.put(domain, perm);
        }

        switch (type.toLowerCase()) {
            case "camera":
                perm.camera = granted;
                break;
            case "microphone":
                perm.microphone = granted;
                break;
            case "location":
                perm.location = granted;
                break;
            case "notifications":
                perm.notifications = granted;
                break;
            default:
                BrowserLogger.getInstance().w(TAG, BrowserLogger.LogCategory.PRIVACY,
                        "Unknown permission type: " + type);
                return;
        }

        savePermissions();
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "Permission set: " + domain + " " + type + " = " + granted);
    }

    public void revokeAllPermissions(String domain) {
        permissionsMap.remove(domain);
        savePermissions();
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "All permissions revoked for: " + domain);
    }

    public void resetAllPermissions() {
        permissionsMap.clear();
        savePermissions();
        BrowserLogger.getInstance().i(TAG, BrowserLogger.LogCategory.PRIVACY,
                "All permissions reset");
    }

    public List<SitePermission> getAllPermissions() {
        return new ArrayList<>(permissionsMap.values());
    }

    public static class SitePermission {
        public String domain;
        public boolean camera;
        public boolean microphone;
        public boolean location;
        public boolean notifications;

        public SitePermission() {
        }

        public SitePermission(String domain) {
            this.domain = domain;
            this.camera = false;
            this.microphone = false;
            this.location = false;
            this.notifications = false;
        }
    }
}