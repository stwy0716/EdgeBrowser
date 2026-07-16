package com.edge.browser.devtools;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.edge.browser.BrowserLogger;
import com.edge.browser.webview.EdgeWebView;

import java.util.ArrayList;
import java.util.List;

public class DevToolsManager {

    private static DevToolsManager instance;
    private final List<String> consoleMessages = new ArrayList<>();
    private int networkRequestCount = 0;
    private String customUserAgent = null;

    private DevToolsManager() {}

    public static synchronized DevToolsManager getInstance() {
        if (instance == null) {
            instance = new DevToolsManager();
        }
        return instance;
    }

    public String getPageSource(EdgeWebView webView) {
        if (webView == null) return "No WebView available";
        try {
            final String[] result = {""};
            final Object lock = new Object();
            webView.evaluateJavascript(
                    "(function(){" +
                    "  return '<html>' + document.documentElement.outerHTML + '</html>';" +
                    "})()",
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            result[0] = value;
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    });
            synchronized (lock) {
                try { lock.wait(5000); } catch (InterruptedException ignored) {}
            }
            if (result[0] != null && !result[0].isEmpty()) {
                String source = result[0];
                if (source.startsWith("\"") && source.endsWith("\"")) {
                    source = source.substring(1, source.length() - 1);
                }
                source = source.replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\t", "\t")
                        .replace("\\/", "/");
                return source;
            }
            return "Unable to get page source";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public void showConsole(Activity activity, EdgeWebView webView) {
        if (webView == null) return;
        injectConsoleCapture(webView);
    }

    public String getUserAgent() {
        return System.getProperty("http.agent", "Unknown");
    }

    public void setUserAgent(EdgeWebView webView, String ua) {
        if (webView == null) return;
        this.customUserAgent = ua;
        webView.getSettings().setUserAgentString(ua);
    }

    public void injectConsoleCapture(EdgeWebView webView) {
        if (webView == null) return;
        consoleMessages.clear();
        String js = "(function(){" +
                "  var origLog = console.log;" +
                "  var origWarn = console.warn;" +
                "  var origError = console.error;" +
                "  var origInfo = console.info;" +
                "  console.log = function() {" +
                "    var args = Array.prototype.slice.call(arguments);" +
                "    var msg = args.map(function(a) { return typeof a === 'object' ? JSON.stringify(a) : String(a); }).join(' ');" +
                "    console._edgeMessages.push('[LOG] ' + msg);" +
                "    origLog.apply(console, arguments);" +
                "  };" +
                "  console.warn = function() {" +
                "    var args = Array.prototype.slice.call(arguments);" +
                "    var msg = args.map(function(a) { return typeof a === 'object' ? JSON.stringify(a) : String(a); }).join(' ');" +
                "    console._edgeMessages.push('[WARN] ' + msg);" +
                "    origWarn.apply(console, arguments);" +
                "  };" +
                "  console.error = function() {" +
                "    var args = Array.prototype.slice.call(arguments);" +
                "    var msg = args.map(function(a) { return typeof a === 'object' ? JSON.stringify(a) : String(a); }).join(' ');" +
                "    console._edgeMessages.push('[ERROR] ' + msg);" +
                "    origError.apply(console, arguments);" +
                "  };" +
                "  console.info = function() {" +
                "    var args = Array.prototype.slice.call(arguments);" +
                "    var msg = args.map(function(a) { return typeof a === 'object' ? JSON.stringify(a) : String(a); }).join(' ');" +
                "    console._edgeMessages.push('[INFO] ' + msg);" +
                "    origInfo.apply(console, arguments);" +
                "  };" +
                "  console._edgeMessages = [];" +
                "  console.getEdgeMessages = function() { return console._edgeMessages; };" +
                "})();";
        webView.injectJavaScript(js);
    }

    public List<String> getConsoleMessages(EdgeWebView webView) {
        if (webView == null) return new ArrayList<>();
        final List<String> messages = new ArrayList<>();
        final Object lock = new Object();
        webView.evaluateJavascript(
                "console.getEdgeMessages ? JSON.stringify(console.getEdgeMessages()) : '[]'",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (value != null && !value.isEmpty() && !"[]".equals(value)) {
                            try {
                                String json = value;
                                if (json.startsWith("\"") && json.endsWith("\"")) {
                                    json = json.substring(1, json.length() - 1);
                                }
                                json = json.replace("\\\"", "\"");
                                com.google.gson.Gson gson = new com.google.gson.Gson();
                                String[] arr = gson.fromJson(json, String[].class);
                                if (arr != null) {
                                    for (String msg : arr) {
                                        messages.add(msg);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                });
        synchronized (lock) {
            try { lock.wait(3000); } catch (InterruptedException ignored) {}
        }
        return messages;
    }

    public void incrementNetworkRequestCount() {
        networkRequestCount++;
    }

    public int getNetworkRequestCount() {
        return networkRequestCount;
    }

    public void resetNetworkRequestCount() {
        networkRequestCount = 0;
    }

    public String getResourceInfo(EdgeWebView webView) {
        if (webView == null) return "No WebView";
        StringBuilder sb = new StringBuilder();
        sb.append("URL: ").append(webView.getUrl()).append("\n");
        sb.append("Title: ").append(webView.getTitle()).append("\n");
        sb.append("Progress: ").append(webView.getProgress()).append("%\n");
        sb.append("User Agent: ").append(webView.getSettings().getUserAgentString()).append("\n");
        sb.append("Network Requests: ").append(networkRequestCount).append("\n");
        return sb.toString();
    }

    public static final String[] UA_OPTIONS = {
            "Android (Default)",
            "Desktop (Windows)",
            "Desktop (macOS)",
            "iPhone",
            "iPad"
    };

    public static final String[] UA_VALUES = {
            null, // default
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
    };
}