package com.edge.browser.content;

import android.content.Context;
import android.content.SharedPreferences;

import com.edge.browser.BrowserLogger;
import com.edge.browser.BrowserLogger.LogCategory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class OfflinePageManager {

    private static final String TAG = "OfflinePageManager";
    private static final String PREFS_NAME = "edge_offline_pages";
    private static final String KEY_PAGES = "offline_pages_list";
    private static final String PAGES_DIR = "offline_pages";

    private static OfflinePageManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final File pagesDir;

    private List<OfflinePage> pagesCache;
    private long nextId;

    public static class OfflinePage {
        public long id;
        public String title;
        public String url;
        public String filePath;
        public long savedAt;

        public OfflinePage() {}

        public OfflinePage(long id, String title, String url, String filePath, long savedAt) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.filePath = filePath;
            this.savedAt = savedAt;
        }
    }

    private OfflinePageManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        pagesDir = new File(context.getFilesDir(), PAGES_DIR);
        if (!pagesDir.exists()) {
            pagesDir.mkdirs();
        }
        loadCache();
        BrowserLogger.getInstance().d(TAG, LogCategory.SYSTEM, "OfflinePageManager initialized");
    }

    public static synchronized OfflinePageManager getInstance(Context context) {
        if (instance == null) {
            instance = new OfflinePageManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadCache() {
        String json = prefs.getString(KEY_PAGES, null);
        if (json != null) {
            try {
                Type listType = new TypeToken<ArrayList<OfflinePage>>(){}.getType();
                pagesCache = gson.fromJson(json, listType);
            } catch (Exception e) {
                BrowserLogger.getInstance().e(TAG, LogCategory.SYSTEM, "Failed to load offline pages cache", e);
                pagesCache = new ArrayList<>();
            }
        } else {
            pagesCache = new ArrayList<>();
        }

        nextId = 1;
        for (OfflinePage page : pagesCache) {
            if (page.id >= nextId) {
                nextId = page.id + 1;
            }
        }
    }

    private void saveCache() {
        String json = gson.toJson(pagesCache);
        prefs.edit().putString(KEY_PAGES, json).apply();
    }

    public void savePageForOffline(String url, String htmlContent, String title) {
        if (url == null || htmlContent == null) return;

        long id = nextId++;
        String fileName = "page_" + id + ".html";
        File htmlFile = new File(pagesDir, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(htmlFile);
            fos.write(htmlContent.getBytes("UTF-8"));
            fos.flush();
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, LogCategory.SYSTEM, "Failed to save offline page content", e);
            return;
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (Exception ignored) {}
            }
        }

        OfflinePage page = new OfflinePage(id, title != null ? title : url, url,
                htmlFile.getAbsolutePath(), System.currentTimeMillis());
        pagesCache.add(page);
        saveCache();

        BrowserLogger.getInstance().i(TAG, LogCategory.SYSTEM, "Page saved for offline: " + title);
    }

    public List<OfflinePage> getOfflinePages() {
        return new ArrayList<>(pagesCache);
    }

    public OfflinePage getPage(long id) {
        for (OfflinePage page : pagesCache) {
            if (page.id == id) {
                return page;
            }
        }
        return null;
    }

    public void deletePage(long id) {
        OfflinePage toRemove = null;
        for (OfflinePage page : pagesCache) {
            if (page.id == id) {
                toRemove = page;
                break;
            }
        }
        if (toRemove != null) {
            File file = new File(toRemove.filePath);
            if (file.exists()) {
                file.delete();
            }
            pagesCache.remove(toRemove);
            saveCache();
            BrowserLogger.getInstance().d(TAG, LogCategory.SYSTEM, "Offline page deleted: " + id);
        }
    }

    public String loadPageContent(long id) {
        OfflinePage page = getPage(id);
        if (page == null) return null;

        File file = new File(page.filePath);
        if (!file.exists()) return null;

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, LogCategory.SYSTEM, "Failed to load offline page content: " + id, e);
            return null;
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) {}
            }
        }
    }
}