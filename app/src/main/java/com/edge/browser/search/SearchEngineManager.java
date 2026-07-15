package com.edge.browser.search;

import android.content.Context;

import com.edge.browser.data.DatabaseHelper;

import java.util.LinkedHashMap;
import java.util.Map;

public class SearchEngineManager {

    private static final String PREF_SEARCH_ENGINE = "search_engine";
    private static final String DEFAULT_ENGINE = "bing";

    private static SearchEngineManager instance;
    private final DatabaseHelper db;

    public static final Map<String, SearchEngine> ENGINES = new LinkedHashMap<>();

    static {
        ENGINES.put("bing", new SearchEngine("Bing", "https://www.bing.com/search?q="));
        ENGINES.put("google", new SearchEngine("Google", "https://www.google.com/search?q="));
        ENGINES.put("baidu", new SearchEngine("百度", "https://www.baidu.com/s?wd="));
        ENGINES.put("sogou", new SearchEngine("搜狗", "https://www.sogou.com/web?query="));
        ENGINES.put("duckduckgo", new SearchEngine("DuckDuckGo", "https://duckduckgo.com/?q="));
    }

    private SearchEngineManager(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static synchronized SearchEngineManager getInstance(Context context) {
        if (instance == null) {
            instance = new SearchEngineManager(context.getApplicationContext());
        }
        return instance;
    }

    public String getSearchUrl(String query) {
        String key = getCurrentEngine();
        SearchEngine engine = ENGINES.get(key);
        if (engine == null) engine = ENGINES.get(DEFAULT_ENGINE);
        return engine.searchUrl + query;
    }

    public String getCurrentEngine() {
        return db.getSetting(PREF_SEARCH_ENGINE, DEFAULT_ENGINE);
    }

    public void setCurrentEngine(String key) {
        db.setSetting(PREF_SEARCH_ENGINE, key);
    }

    public String getCurrentEngineName() {
        SearchEngine engine = ENGINES.get(getCurrentEngine());
        return engine != null ? engine.name : "Bing";
    }

    public static class SearchEngine {
        public final String name;
        public final String searchUrl;

        SearchEngine(String name, String searchUrl) {
            this.name = name;
            this.searchUrl = searchUrl;
        }
    }
}