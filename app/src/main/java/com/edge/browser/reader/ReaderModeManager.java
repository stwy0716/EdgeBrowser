package com.edge.browser.reader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.edge.browser.webview.EdgeWebView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReaderModeManager {

    private static ReaderModeManager instance;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isReaderMode = false;
    private ReaderModeListener listener;

    // Reader settings
    private String fontFamily = "sans-serif";
    private int fontSize = 16;
    private float lineSpacing = 1.6f;
    private String backgroundColor = "#F5F0E0"; // Light yellow
    private boolean isDarkMode = false;

    public interface ReaderModeListener {
        void onReaderModeReady(String html);
        void onReaderModeError(String error);
    }

    private ReaderModeManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized ReaderModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ReaderModeManager(context);
        }
        return instance;
    }

    public void setListener(ReaderModeListener listener) {
        this.listener = listener;
    }

    public boolean isReaderAvailable(String url) {
        return url != null && url.startsWith("http");
    }

    public void enableReaderMode(String url) {
        isReaderMode = true;
        executor.submit(() -> {
            try {
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .get();

                String readableContent = extractReadableContent(doc);
                String styledHtml = buildReaderHtml(readableContent, doc.title());

                handler.post(() -> {
                    if (listener != null) listener.onReaderModeReady(styledHtml);
                });
            } catch (Exception e) {
                handler.post(() -> {
                    if (listener != null) listener.onReaderModeError(e.getMessage());
                });
            }
        });
    }

    public void enableReaderMode(EdgeWebView webView) {
        isReaderMode = true;
        webView.evaluateJavascript(
                "(function() {" +
                        "  var article = document.querySelector('article') || document.querySelector('main') || " +
                        "    document.querySelector('[role=main]') || document.body;" +
                        "  return article.innerHTML;" +
                        "})()",
                value -> {
                    String html = value.replaceAll("^\"|\"$", "")
                            .replace("\\\"", "\"")
                            .replace("\\n", "\n")
                            .replace("\\t", "\t");
                    String styledHtml = buildReaderHtml(html, webView.getTitle());
                    webView.loadDataWithBaseURL(webView.getCurrentUrl(), styledHtml,
                            "text/html", "UTF-8", null);
                });
    }

    public void disableReaderMode() {
        isReaderMode = false;
    }

    public boolean isReaderMode() {
        return isReaderMode;
    }

    private String extractReadableContent(Document doc) {
        // Remove unwanted elements
        doc.select("script, style, nav, header, footer, aside, .ad, .advertisement, " +
                ".social-share, .comments, .sidebar, .nav, .menu, .popup, .modal").remove();

        // Try to find main content
        Element content = doc.selectFirst("article");
        if (content == null) content = doc.selectFirst("main");
        if (content == null) content = doc.selectFirst("[role=main]");
        if (content == null) content = doc.selectFirst(".post-content, .article-content, .entry-content");
        if (content == null) content = doc.body();

        return content != null ? content.html() : "";
    }

    private String buildReaderHtml(String content, String title) {
        String bgColor = isDarkMode ? "#1a1a1a" : backgroundColor;
        String textColor = isDarkMode ? "#e0e0e0" : "#333333";

        return "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: " + fontFamily + "; font-size: " + fontSize + "px; " +
                "  line-height: " + lineSpacing + "; background: " + bgColor + "; " +
                "  color: " + textColor + "; max-width: 800px; margin: 0 auto; padding: 20px; }" +
                "h1 { font-size: 1.8em; margin-bottom: 0.5em; }" +
                "img { max-width: 100%; height: auto; }" +
                "p { margin: 1em 0; }" +
                "a { color: #0078d4; }" +
                "</style></head><body>" +
                "<h1>" + (title != null ? title : "") + "</h1>" +
                content +
                "</body></html>";
    }

    // Reader settings
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }
    public void setLineSpacing(float lineSpacing) { this.lineSpacing = lineSpacing; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
    public void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
        backgroundColor = darkMode ? "#1a1a1a" : "#F5F0E0";
    }
    public String getFontFamily() { return fontFamily; }
    public int getFontSize() { return fontSize; }
    public float getLineSpacing() { return lineSpacing; }
    public String getBackgroundColor() { return backgroundColor; }
    public boolean isDarkMode() { return isDarkMode; }
}