package com.edge.browser.ai;

import com.edge.browser.tab.TabItem;
import com.edge.browser.tab.TabManager;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmartTabManager {

    private static SmartTabManager instance;

    private SmartTabManager() {}

    public static synchronized SmartTabManager getInstance() {
        if (instance == null) {
            instance = new SmartTabManager();
        }
        return instance;
    }

    public void autoGroupTabs() {
        TabManager tabManager = TabManager.getInstance();
        List<TabItem> tabs = tabManager.getAllTabs();
        if (tabs == null || tabs.isEmpty()) return;

        Map<String, String> domainToGroup = new HashMap<>();
        int[] colors = new int[]{
                0xFF0078D4, 0xFFE81123, 0xFF10893E, 0xFF6B69D6,
                0xFF00B7C3, 0xFFFF8C00, 0xFFCA5010, 0xFFFFB900
        };
        int colorIndex = 0;

        for (TabItem tab : tabs) {
            String domain = extractDomain(tab.getUrl());
            if (domain == null || domain.isEmpty()) continue;

            String groupId = domainToGroup.get(domain);
            if (groupId == null) {
                groupId = "group_" + domain;
                domainToGroup.put(domain, groupId);
            }
            tab.setGroupId(groupId);
            tab.setGroupColor(colors[colorIndex % colors.length]);
            colorIndex++;

            if (tab.getTitle() != null && tab.getTitle().contains("新标签页")) {
                tab.setTitle(capitalizeDomain(domain));
            }
        }
    }

    public List<String> detectDuplicateTabs() {
        TabManager tabManager = TabManager.getInstance();
        List<TabItem> tabs = tabManager.getAllTabs();
        List<String> duplicates = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < tabs.size(); i++) {
            TabItem tab = tabs.get(i);
            String url = normalizeUrl(tab.getUrl());
            if (url == null || url.isEmpty() || "about:blank".equals(url)) continue;

            if (seen.contains(url)) {
                duplicates.add(tab.getUrl());
            } else {
                seen.add(url);
            }
        }
        return duplicates;
    }

    public void closeDuplicates() {
        TabManager tabManager = TabManager.getInstance();
        List<TabItem> tabs = new ArrayList<>(tabManager.getAllTabs());
        Set<String> seen = new HashSet<>();

        for (int i = tabs.size() - 1; i >= 0; i--) {
            TabItem tab = tabs.get(i);
            String url = normalizeUrl(tab.getUrl());
            if (url == null || url.isEmpty() || "about:blank".equals(url)) continue;

            if (seen.contains(url)) {
                tabManager.removeTab(i);
            } else {
                seen.add(url);
            }
        }
    }

    private String extractDomain(String url) {
        if (url == null) return null;
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                URI uri = new URI(url);
                String host = uri.getHost();
                if (host != null) {
                    if (host.startsWith("www.")) host = host.substring(4);
                    return host;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeUrl(String url) {
        if (url == null) return null;
        url = url.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.startsWith("https://www.")) url = "https://" + url.substring(12);
        else if (url.startsWith("http://www.")) url = "http://" + url.substring(11);
        return url;
    }

    private String capitalizeDomain(String domain) {
        if (domain == null || domain.isEmpty()) return domain;
        String[] parts = domain.split("\\.");
        if (parts.length > 0) {
            parts[0] = parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(".");
            sb.append(parts[i]);
        }
        return sb.toString();
    }
}