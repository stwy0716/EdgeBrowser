package com.edge.browser.ai;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AISearchManager {

    private static AISearchManager instance;

    private AISearchManager() {}

    public static synchronized AISearchManager getInstance() {
        if (instance == null) {
            instance = new AISearchManager();
        }
        return instance;
    }

    public List<String> getSuggestions(String query) {
        List<String> suggestions = new ArrayList<>();
        try {
            String url = "https://suggestqueries.google.com/complete/search?client=chrome&q="
                    + URLEncoder.encode(query, "UTF-8");
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .ignoreContentType(true)
                    .get();
            String body = doc.body().text();
            // Parse JSON array: ["query",["suggestion1","suggestion2",...]]
            Pattern pattern = Pattern.compile("\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(body);
            int count = 0;
            while (matcher.find() && count < 10) {
                String suggestion = matcher.group(1);
                if (!suggestion.equals(query)) {
                    suggestions.add(suggestion);
                    count++;
                }
            }
        } catch (Exception e) {
            // Fallback: generate simple suggestions
            suggestions.add(query + " 是什么意思");
            suggestions.add(query + " 最新消息");
            suggestions.add(query + " 教程");
            suggestions.add(query + " 官网");
        }
        return suggestions;
    }

    public String enhanceSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) return query;
        query = query.trim();

        StringBuilder enhanced = new StringBuilder();

        // Add quotes for multi-word queries
        if (query.contains(" ") && !query.startsWith("\"") && query.split("\\s+").length >= 3) {
            enhanced.append("\"").append(query).append("\"");
        } else {
            enhanced.append(query);
        }

        // Add related terms based on query type
        String lower = query.toLowerCase();
        if (containsAny(lower, "how", "how to", "怎样", "如何", "怎么")) {
            enhanced.append(" tutorial guide");
        } else if (containsAny(lower, "best", "top", "最好", "推荐", "最佳")) {
            enhanced.append(" review comparison");
        } else if (containsAny(lower, "download", "下载")) {
            enhanced.append(" official site");
        } else if (containsAny(lower, "error", "错误", "fix", "修复", "bug")) {
            enhanced.append(" solution fix");
        } else if (containsAny(lower, "price", "价格", "cost", "buy", "购买")) {
            enhanced.append(" price review");
        } else if (containsAny(lower, "news", "新闻", "latest")) {
            enhanced.append(" latest news");
        } else if (containsAny(lower, "definition", "定义", "what is", "什么是")) {
            enhanced.append(" definition meaning");
        }

        return enhanced.toString();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}