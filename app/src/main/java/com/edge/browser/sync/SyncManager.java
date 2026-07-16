package com.edge.browser.sync;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.edge.browser.bookmark.BookmarkManager;
import com.edge.browser.data.DatabaseHelper;
import com.edge.browser.history.HistoryManager;
import com.edge.browser.password.PasswordManager;
import com.edge.browser.quicklinks.QuickLinkManager;
import com.edge.browser.search.SearchEngineManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SyncManager {

    private static final String EXPORT_DIR = "EdgeBrowser/backup";
    private static final String EXPORT_FILENAME = "edge_backup_";
    private static final String EXPORT_EXT = ".json";

    private static SyncManager instance;
    private final Gson gson;

    private SyncManager() {
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public static synchronized SyncManager getInstance() {
        if (instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }

    private File getExportDir(Context context) {
        File dir = new File(context.getExternalFilesDir(null), EXPORT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private String generateFileName(String prefix) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return prefix + timestamp + EXPORT_EXT;
    }

    // === Export All Data ===

    public String exportAllData(Context context) {
        try {
            SyncData data = new SyncData();
            data.version = "1.0";
            data.exportDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            // Bookmarks
            BookmarkManager bm = BookmarkManager.getInstance(context);
            data.bookmarks = new ArrayList<>();
            for (DatabaseHelper.BookmarkEntry entry : bm.getBookmarks()) {
                SyncData.BookmarkData bd = new SyncData.BookmarkData();
                bd.title = entry.title;
                bd.url = entry.url;
                bd.createdAt = entry.createdAt;
                data.bookmarks.add(bd);
            }

            // History
            HistoryManager hm = HistoryManager.getInstance(context);
            data.history = new ArrayList<>();
            for (DatabaseHelper.HistoryEntry entry : hm.getHistory()) {
                SyncData.HistoryData hd = new SyncData.HistoryData();
                hd.title = entry.title;
                hd.url = entry.url;
                hd.visitedAt = entry.visitedAt;
                data.history.add(hd);
            }

            // Search engine
            SearchEngineManager sem = SearchEngineManager.getInstance(context);
            data.searchEngine = sem.getCurrentEngine();

            // Quick links
            QuickLinkManager qlm = QuickLinkManager.getInstance(context);
            data.quickLinks = new ArrayList<>();
            for (DatabaseHelper.QuickLinkEntry entry : qlm.getLinks()) {
                SyncData.QuickLinkData qld = new SyncData.QuickLinkData();
                qld.title = entry.title;
                qld.url = entry.url;
                qld.position = entry.position;
                data.quickLinks.add(qld);
            }

            // Passwords (base64 encoded)
            PasswordManager pm = PasswordManager.getInstance(context);
            data.passwords = new ArrayList<>();
            for (DatabaseHelper.PasswordEntry entry : pm.getAllPasswords()) {
                SyncData.PasswordData pd = new SyncData.PasswordData();
                pd.domain = entry.domain;
                pd.username = entry.username;
                // Base64 encode the password
                pd.password = Base64.encodeToString(entry.password.getBytes(), Base64.NO_WRAP);
                pd.savedAt = entry.savedAt;
                data.passwords.add(pd);
            }

            // Settings (key-value pairs)
            DatabaseHelper db = DatabaseHelper.getInstance(context);
            data.settings = new ArrayList<>();
            // Read important settings
            String[] settingKeys = {
                "search_engine", "theme", "startup_mode", "homepage_url",
                "ad_block", "night_mode", "https_only", "doh", "dnt",
                "anti_fingerprint", "third_party_cookie", "tracking_protection"
            };
            for (String key : settingKeys) {
                String value = db.getSetting(key, null);
                if (value != null) {
                    SyncData.SettingData sd = new SyncData.SettingData();
                    sd.key = key;
                    sd.value = value;
                    data.settings.add(sd);
                }
            }

            String json = gson.toJson(data);
            File dir = getExportDir(context);
            String fileName = generateFileName(EXPORT_FILENAME);
            File file = new File(dir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
            writer.write(json);
            writer.close();
            fos.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // === Import All Data ===

    public boolean importAllData(Context context, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) return false;

            StringBuilder sb = new StringBuilder();
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            fis.close();

            SyncData data = gson.fromJson(sb.toString(), SyncData.class);
            if (data == null) return false;

            // Import bookmarks
            if (data.bookmarks != null) {
                BookmarkManager bm = BookmarkManager.getInstance(context);
                for (SyncData.BookmarkData bd : data.bookmarks) {
                    bm.addBookmark(bd.title, bd.url);
                }
            }

            // Import history
            if (data.history != null) {
                HistoryManager hm = HistoryManager.getInstance(context);
                for (SyncData.HistoryData hd : data.history) {
                    hm.addVisit(hd.title, hd.url);
                }
            }

            // Import search engine
            if (data.searchEngine != null) {
                SearchEngineManager sem = SearchEngineManager.getInstance(context);
                sem.setCurrentEngine(data.searchEngine);
            }

            // Import quick links
            if (data.quickLinks != null) {
                QuickLinkManager qlm = QuickLinkManager.getInstance(context);
                List<DatabaseHelper.QuickLinkEntry> links = new ArrayList<>();
                for (SyncData.QuickLinkData qld : data.quickLinks) {
                    DatabaseHelper.QuickLinkEntry entry = new DatabaseHelper.QuickLinkEntry();
                    entry.title = qld.title;
                    entry.url = qld.url;
                    entry.position = qld.position;
                    links.add(entry);
                }
                qlm.saveLinks(links);
            }

            // Import passwords
            if (data.passwords != null) {
                PasswordManager pm = PasswordManager.getInstance(context);
                for (SyncData.PasswordData pd : data.passwords) {
                    try {
                        String decodedPassword = new String(Base64.decode(pd.password, Base64.NO_WRAP));
                        pm.savePassword(pd.domain, pd.username, decodedPassword);
                    } catch (Exception e) {
                        // Skip invalid password entries
                    }
                }
            }

            // Import settings
            if (data.settings != null) {
                DatabaseHelper db = DatabaseHelper.getInstance(context);
                for (SyncData.SettingData sd : data.settings) {
                    if (sd.key != null && sd.value != null) {
                        db.setSetting(sd.key, sd.value);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // === Share Export File ===

    public void shareExportFile(Context context) {
        String filePath = exportAllData(context);
        if (filePath == null) {
            Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(filePath);
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "分享备份文件"));
    }

    // === Import From File (opens file picker) ===

    public void importFromFile(Context context) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Start via activity - caller should use startActivityForResult
        context.startActivity(intent);
    }

    // === Export Bookmarks Only ===

    public String exportBookmarks(Context context) {
        try {
            BookmarkManager bm = BookmarkManager.getInstance(context);
            List<SyncData.BookmarkData> bookmarks = new ArrayList<>();
            for (DatabaseHelper.BookmarkEntry entry : bm.getBookmarks()) {
                SyncData.BookmarkData bd = new SyncData.BookmarkData();
                bd.title = entry.title;
                bd.url = entry.url;
                bd.createdAt = entry.createdAt;
                bookmarks.add(bd);
            }

            String json = gson.toJson(bookmarks);
            File dir = getExportDir(context);
            String fileName = generateFileName("edge_bookmarks_");
            File file = new File(dir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
            writer.write(json);
            writer.close();
            fos.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // === Export Passwords Only (encrypted/base64) ===

    public String exportPasswords(Context context) {
        try {
            PasswordManager pm = PasswordManager.getInstance(context);
            List<SyncData.PasswordData> passwords = new ArrayList<>();
            for (DatabaseHelper.PasswordEntry entry : pm.getAllPasswords()) {
                SyncData.PasswordData pd = new SyncData.PasswordData();
                pd.domain = entry.domain;
                pd.username = entry.username;
                pd.password = Base64.encodeToString(entry.password.getBytes(), Base64.NO_WRAP);
                pd.savedAt = entry.savedAt;
                passwords.add(pd);
            }

            String json = gson.toJson(passwords);
            File dir = getExportDir(context);
            String fileName = generateFileName("edge_passwords_");
            File file = new File(dir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
            writer.write(json);
            writer.close();
            fos.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // === Data Model ===

    static class SyncData {
        String version;
        String exportDate;
        String searchEngine;
        List<BookmarkData> bookmarks;
        List<HistoryData> history;
        List<QuickLinkData> quickLinks;
        List<PasswordData> passwords;
        List<SettingData> settings;

        static class BookmarkData {
            String title;
            String url;
            long createdAt;
        }

        static class HistoryData {
            String title;
            String url;
            long visitedAt;
        }

        static class QuickLinkData {
            String title;
            String url;
            int position;
        }

        static class PasswordData {
            String domain;
            String username;
            String password;
            long savedAt;
        }

        static class SettingData {
            String key;
            String value;
        }
    }
}