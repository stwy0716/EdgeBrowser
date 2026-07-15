package com.edge.browser.webview;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class EdgeWebView extends WebView {

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
        WebSettings settings = getSettings();

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

        // Hardware acceleration
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Enable smooth scrolling
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        setVerticalScrollBarEnabled(true);
        setHorizontalScrollBarEnabled(true);

        setWebViewClient(new EdgeWebViewClient());
        setWebChromeClient(new EdgeWebChromeClient());
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
            if (callback != null) callback.onPageStarted(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            currentUrl = url;
            currentTitle = view.getTitle();
            if (callback != null) callback.onPageFinished(url, currentTitle);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (callback != null && request.isForMainFrame()) {
                callback.onReceivedError(error.getErrorCode(),
                        error.getDescription().toString(),
                        request.getUrl().toString());
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return true;
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
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
}