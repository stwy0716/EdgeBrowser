package com.edge.browser.ai;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AISummaryManager {

    private static AISummaryManager instance;

    private AISummaryManager() {}

    public static synchronized AISummaryManager getInstance() {
        if (instance == null) {
            instance = new AISummaryManager();
        }
        return instance;
    }

    public String generateSummary(String url) {
        try {
            String articleText = extractArticleText(url);
            if (articleText == null || articleText.trim().isEmpty()) {
                return "无法提取页面内容";
            }
            return summarizeText(articleText);
        } catch (Exception e) {
            return "摘要生成失败: " + e.getMessage();
        }
    }

    public String extractArticleText(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .get();

        // Remove non-content elements
        doc.select("script, style, nav, header, footer, aside, .sidebar, .advertisement, " +
                ".comments, .social-share, .nav, .menu, .footer, .header, iframe, form").remove();

        // Try to find main content area
        Element article = doc.selectFirst("article");
        if (article == null) {
            article = doc.selectFirst("[role=main]");
        }
        if (article == null) {
            article = doc.selectFirst(".post-content, .article-content, .entry-content, " +
                    ".content, #content, .main-content, #main-content, .post, .article-body");
        }
        if (article == null) {
            article = doc.body();
        }

        // Extract text from paragraphs
        Elements paragraphs = article.select("p");
        StringBuilder sb = new StringBuilder();
        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.length() > 20) {
                sb.append(text).append(" ");
            }
        }

        // If no paragraphs found, extract all text nodes
        if (sb.length() == 0 && article != null) {
            String text = article.text().trim();
            if (text.length() > 50) {
                sb.append(text);
            }
        }

        return sb.toString().trim();
    }

    private String summarizeText(String text) {
        if (text == null || text.isEmpty()) return "";

        // Split into sentences
        String[] sentences = text.split("(?<=[。！？.!?])\\s*");
        List<String> validSentences = new ArrayList<>();
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.length() > 10) {
                validSentences.add(trimmed);
            }
        }

        if (validSentences.isEmpty()) {
            return text.length() > 200 ? text.substring(0, 200) + "..." : text;
        }

        if (validSentences.size() <= 5) {
            StringBuilder sb = new StringBuilder();
            for (String s : validSentences) {
                sb.append(s).append(" ");
            }
            return sb.toString().trim();
        }

        // TF-IDF based key sentence extraction
        List<String> summary = extractKeySentences(validSentences, Math.min(5, validSentences.size()));
        StringBuilder sb = new StringBuilder();
        for (String s : summary) {
            sb.append(s).append(" ");
        }
        return sb.toString().trim();
    }

    private List<String> extractKeySentences(List<String> sentences, int count) {
        // Simple TF-based scoring
        Map<String, Integer> wordFreq = new HashMap<>();
        Pattern wordPattern = Pattern.compile("[\\u4e00-\\u9fff]+|[a-zA-Z]+");

        for (String sentence : sentences) {
            java.util.regex.Matcher m = wordPattern.matcher(sentence.toLowerCase());
            while (m.find()) {
                String word = m.group();
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }

        List<SentenceScore> scored = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            double score = 0;
            java.util.regex.Matcher m = wordPattern.matcher(sentence.toLowerCase());
            int wordCount = 0;
            while (m.find()) {
                String word = m.group();
                score += wordFreq.getOrDefault(word, 0);
                wordCount++;
            }
            if (wordCount > 0) {
                score /= wordCount;
            }
            // Boost first sentences
            if (i < 3) score *= 1.5;
            scored.add(new SentenceScore(sentence, score, i));
        }

        Collections.sort(scored, Comparator.comparingDouble((SentenceScore s) -> -s.score)
                .thenComparingInt(s -> s.index));

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, scored.size()); i++) {
            result.add(scored.get(i).sentence);
        }

        // Sort by original order
        Collections.sort(result, Comparator.comparingInt(s -> {
            for (int j = 0; j < sentences.size(); j++) {
                if (sentences.get(j).equals(s)) return j;
            }
            return 0;
        }));

        return result;
    }

    private static class SentenceScore {
        String sentence;
        double score;
        int index;

        SentenceScore(String sentence, double score, int index) {
            this.sentence = sentence;
            this.score = score;
            this.index = index;
        }
    }
}