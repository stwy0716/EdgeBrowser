package com.edge.browser.webview;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edge.browser.BrowserLogger;
import com.edge.browser.BrowserLogger.LogCategory;

public class EdgeWebView extends WebView implements IBrowserView {

    private static final String TAG = "EdgeWebView";
    private boolean isHardwareAccelerated = true;
    private boolean isSleeping = false;
    private WebViewCallback callback;
    private String currentUrl = "";
    private String currentTitle = "";

    public interface WebViewCallback {
        void onPageStarted(String url);
        void onPageFinished(String url, String title);
        void onProgressChanged(int progress);
        void onReceivedIcon(Bitmap icon);
        void onReceivedError(int errorCode, String description, String failingUrl);
        void onTitleChanged(String title);
        void onUrlChanged(String url);
    }

    public EdgeWebView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public EdgeWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EdgeWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        try {
            WebSettings settings = getSettings();

            // Google Chromium 内核优化
            ChromiumWebViewFactory.getInstance().applyChromiumOptimizations(this);

            if (settings != null) {
                // Basic settings
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setDatabaseEnabled(true);
                settings.setLoadWithOverviewMode(true);
                settings.setUseWideViewPort(true);
                settings.setSupportZoom(true);
                settings.setBuiltInZoomControls(true);
                settings.setDisplayZoomControls(false);
                settings.setAllowFileAccess(true);
                settings.setAllowContentAccess(true);
                settings.setSaveFormData(true);
                settings.setGeolocationEnabled(true);
                settings.setMediaPlaybackRequiresUserGesture(false);
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            }

            // Hardware acceleration
            setLayerType(View.LAYER_TYPE_HARDWARE, null);

            // Smooth scrolling
            setVerticalScrollBarEnabled(true);
            setHorizontalScrollBarEnabled(true);

            setWebViewClient(new EdgeWebViewClient());
            setWebChromeClient(new EdgeWebChromeClient());
        } catch (Exception e) {
            // Graceful fallback - avoid crash on WebView init failure
            BrowserLogger.getInstance().e(TAG, LogCategory.SYSTEM, "WebView init failed", e);
        }
    }

    public void setCallback(WebViewCallback callback) {
        this.callback = callback;
    }

    public void setHardwareAccelerated(boolean accelerated) {
        this.isHardwareAccelerated = accelerated;
        setLayerType(accelerated ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_SOFTWARE, null);
    }

    public boolean isSleeping() {
        return isSleeping;
    }

    public void setSleeping(boolean sleeping) {
        this.isSleeping = sleeping;
        if (sleeping) {
            pauseTimers();
            onPause();
        } else {
            resumeTimers();
            onResume();
        }
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    private class EdgeWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            currentUrl = url;
            BrowserLogger.getInstance().d(TAG, LogCategory.NAVIGATION, "开始加载: " + url);
            if (callback != null) callback.onPageStarted(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            currentUrl = url;
            currentTitle = view.getTitle();
            BrowserLogger.getInstance().d(TAG, LogCategory.NAVIGATION, "加载完成: " + url);
            if (callback != null) callback.onPageFinished(url, currentTitle);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (callback != null && request != null && request.isForMainFrame() && error != null) {
                BrowserLogger.getInstance().logPageError(request.getUrl().toString(),
                        error.getErrorCode(), error.getDescription() != null ? error.getDescription().toString() : "unknown");
                callback.onReceivedError(error.getErrorCode(),
                        error.getDescription() != null ? error.getDescription().toString() : "unknown",
                        request.getUrl().toString());
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            // Handle special schemes
            if (url.startsWith("intent://") || url.startsWith("market://") ||
                url.startsWith("tel:") || url.startsWith("mailto:") ||
                url.startsWith("sms:")) {
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, request.getUrl());
                    view.getContext().startActivity(intent);
                } catch (Exception ignored) {}
                return true;
            }
            // Let WebView handle normal URLs
            return false;
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            if (request != null && errorResponse != null) {
                BrowserLogger.getInstance().w(TAG, LogCategory.NAVIGATION,
                        "HTTP错误: " + request.getUrl() + " - " + errorResponse.getStatusCode());
            }
        }
    }

    private class EdgeWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (callback != null) callback.onProgressChanged(newProgress);
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);
            if (callback != null) callback.onReceivedIcon(icon);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            currentTitle = title;
            if (callback != null) callback.onTitleChanged(title);
        }
    }

    // === IBrowserView 实现 ===

    @Override
    public View getView() { return this; }

    @Override
    public String getEngineType() { return "Chromium WebView"; }

    @Override
    public boolean isWebViewBased() { return true; }

    @Override
    public android.webkit.WebView getWebView() { return this; }

    @Override
    public android.print.PrintDocumentAdapter createPrintDocumentAdapter(String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.createPrintDocumentAdapter(name);
        }
        return null;
    }

    @Override
    public void evaluateJavascript(String script, ValueCallback<String> callback) {
        super.evaluateJavascript(script, callback);
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    @Override
    public Bitmap captureBitmap() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return null;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        draw(new android.graphics.Canvas(bmp));
        return bmp;
    }

    @Override
    public float getScale() { return super.getScale(); }

    @Override
    public int getContentHeight() { return (int) (super.getContentHeight() * getScale()); }

    @Override
    public boolean isLoading() { return getProgress() < 100; }
}