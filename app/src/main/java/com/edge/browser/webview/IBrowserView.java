package com.edge.browser.webview;

import android.graphics.Bitmap;
import android.view.View;

/**
 * 浏览器视图统一接口
 * 屏蔽 Chromium WebView 和 GeckoView 的差异
 */
public interface IBrowserView {

    // === 导航 ===
    void loadUrl(String url);
    void reload();
    void stopLoading();
    boolean canGoBack();
    boolean canGoForward();
    void goBack();
    void goForward();

    // === 状态 ===
    String getUrl();
    String getTitle();
    String getCurrentUrl();
    String getCurrentTitle();
    int getProgress();
    boolean isLoading();

    // === 生命周期 ===
    void onPause();
    void onResume();
    void destroy();

    // === 睡眠标签 ===
    void setSleeping(boolean sleeping);
    boolean isSleeping();

    // === 回调 ===
    void setCallback(EdgeWebView.WebViewCallback callback);

    // === 获取原生视图 ===
    View getView();

    // === 引擎类型 ===
    String getEngineType();

    // === 夜间模式 ===
    void enableNightMode();
    void disableNightMode();

    // === JS/CSS 注入 ===
    void injectJavaScript(String js);
    void injectCSS(String css);

    // === 视频检测 ===
    void detectVideos();

    // === WebView 专有方法 (仅 Chromium 引擎支持) ===
    boolean isWebViewBased();
    android.webkit.WebView getWebView(); // 可能为 null (Gecko 引擎)
    android.print.PrintDocumentAdapter createPrintDocumentAdapter(String name);
    void evaluateJavascript(String script, android.webkit.ValueCallback<String> callback);
    void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl);
    android.webkit.WebSettings getSettings();
    void findNext(boolean forward);
    void findAllAsync(String query);
    void clearMatches();
    Bitmap captureBitmap();
    float getScale();
    int getContentHeight();
}