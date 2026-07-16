package com.edge.browser.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RssReaderManager {

    private static RssReaderManager instance;

    private RssReaderManager() {}

    public static synchronized RssReaderManager getInstance() {
        if (instance == null) {
            instance = new RssReaderManager();
        }
        return instance;
    }

    public List<RssItem> fetchFeed(String feedUrl) throws IOException {
        List<RssItem> items = new ArrayList<>();
        Document doc = Jsoup.connect(feedUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .ignoreContentType(true)
                .get();

        // RSS 2.0
        Elements rssItems = doc.select("item");
        if (!rssItems.isEmpty()) {
            for (Element item : rssItems) {
                RssItem rssItem = new RssItem();
                rssItem.title = getElementText(item, "title");
                rssItem.link = getElementText(item, "link");
                rssItem.description = getElementText(item, "description");
                rssItem.pubDate = getElementText(item, "pubDate");
                rssItem.author = getElementText(item, "author");
                items.add(rssItem);
            }
            return items;
        }

        // Atom feed
        Elements atomEntries = doc.select("entry");
        if (!atomEntries.isEmpty()) {
            for (Element entry : atomEntries) {
                RssItem rssItem = new RssItem();
                rssItem.title = getElementText(entry, "title");
                Element linkEl = entry.selectFirst("link[href]");
                rssItem.link = linkEl != null ? linkEl.attr("href") : "";
                rssItem.description = getElementText(entry, "summary");
                if (rssItem.description.isEmpty()) {
                    rssItem.description = getElementText(entry, "content");
                }
                rssItem.pubDate = getElementText(entry, "published");
                if (rssItem.pubDate.isEmpty()) {
                    rssItem.pubDate = getElementText(entry, "updated");
                }
                Element authorEl = entry.selectFirst("author > name");
                rssItem.author = authorEl != null ? authorEl.text() : "";
                items.add(rssItem);
            }
        }

        return items;
    }

    public List<RssItem> searchFeeds(String query) throws IOException {
        List<RssItem> results = new ArrayList<>();
        // Search for RSS/Atom feed links on a page
        Document doc = Jsoup.connect(query)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .get();

        // Look for RSS/Atom link elements
        Elements feedLinks = doc.select("link[type=application/rss+xml], link[type=application/atom+xml]");
        for (Element link : feedLinks) {
            RssItem item = new RssItem();
            item.title = link.attr("title");
            item.link = link.attr("href");
            if (item.link.startsWith("/")) {
                String base = doc.baseUri();
                if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
                item.link = base + item.link;
            }
            item.description = link.attr("type");
            results.add(item);
        }

        // Also try fetching common feed paths
        String[] commonPaths = {"/feed", "/rss", "/feed.xml", "/rss.xml", "/atom.xml", "/index.xml"};
        for (String path : commonPaths) {
            String feedUrl = query;
            if (query.endsWith("/")) feedUrl = query.substring(0, query.length() - 1);
            feedUrl += path;
            try {
                Document feedDoc = Jsoup.connect(feedUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .ignoreContentType(true)
                        .get();
                if (feedDoc.select("rss, feed").size() > 0) {
                    RssItem item = new RssItem();
                    item.title = path;
                    item.link = feedUrl;
                    item.description = "RSS/Atom Feed";
                    results.add(item);
                }
            } catch (IOException ignored) {
            }
        }

        return results;
    }

    private String getElementText(Element parent, String tag) {
        Element el = parent.selectFirst(tag);
        return el != null ? el.text() : "";
    }

    public static class RssItem {
        public String title;
        public String link;
        public String description;
        public String pubDate;
        public String author;

        public RssItem() {}

        public RssItem(String title, String link, String description, String pubDate, String author) {
            this.title = title;
            this.link = link;
            this.description = description;
            this.pubDate = pubDate;
            this.author = author;
        }

        public String getTitle() { return title; }
        public String getLink() { return link; }
        public String getDescription() { return description; }
        public String getPubDate() { return pubDate; }
        public String getAuthor() { return author; }
    }
}