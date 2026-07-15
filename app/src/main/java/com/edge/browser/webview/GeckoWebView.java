package com.edge.browser.webview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.print.PrintDocumentAdapter;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edge.browser.BrowserLogger;
import com.edge.browser.BrowserLogger.LogCategory;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebRequestError;

/**
 * GeckoWebView - 内置 Mozilla Gecko 引擎 (Firefox 内核)
 * 完全独立于系统 WebView
 */
public class GeckoWebView extends FrameLayout implements IBrowserView {

    private static final String TAG = "GeckoWebView";
    private GeckoView geckoView;
    private GeckoSession session;
    private EdgeWebView.WebViewCallback callback;
    private String currentUrl = "";
    private String currentTitle = "";
    private boolean isSleeping = false;
    private boolean canGoBack = false;
    private boolean canGoForward = false;
    private int progress = 0;
    private boolean loading = false;

    public GeckoWebView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public GeckoWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GeckoWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        try {
            GeckoRuntime runtime = GeckoRuntimeManager.getInstance().getRuntime(context);
            if (runtime == null) {
                BrowserLogger.getInstance().e(TAG, LogCategory.SYSTEM, "GeckoRuntime 未初始化");
                return;
            }
            session = new GeckoSession();
            session.open(runtime);
            geckoView = new GeckoView(context);
            geckoView.setSession(session);
            geckoView.setLayoutParams(new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            addView(geckoView);
            setupDelegates();
            BrowserLogger.getInstance().i(TAG, LogCategory.SYSTEM, "GeckoView 引擎初始化成功");
        } catch (Exception e) {
            BrowserLogger.getInstance().logCrash("GeckoWebView init", e);
        }
    }

    private void setupDelegates() {
        if (session == null) return;

        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override public void onPageStart(@NonNull GeckoSession s, @NonNull String url) {
                currentUrl = url;
                loading = true;
                progress = 0;
                BrowserLogger.getInstance().d(TAG, LogCategory.NAVIGATION, "Gecko 开始加载: " + url);
                if (callback != null) callback.onPageStarted(url);
            }
            @Override public void onPageStop(@NonNull GeckoSession s, boolean success) {
                loading = false;
                progress = 100;
                BrowserLogger.getInstance().d(TAG, LogCategory.NAVIGATION,
                        "Gecko 加载" + (success ? "完成" : "失败") + ": " + currentUrl);
                if (callback != null) callback.onPageFinished(currentUrl, currentTitle);
            }
            @Override public void onProgressChange(@NonNull GeckoSession s, int p) {
                progress = p;
                if (callback != null) callback.onProgressChanged(p);
            }
            @Override public void onSecurityChange(@NonNull GeckoSession s,
                                                    @NonNull SecurityInformation info) {}
        });

        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override public void onLocationChange(@NonNull GeckoSession s, @Nullable String url,
                    @NonNull java.util.List<GeckoSession.PermissionDelegate.ContentPermission> perms,
                    @NonNull Boolean sameDocument) {
                if (url != null) { currentUrl = url; if (callback != null) callback.onUrlChanged(url); }
            }
            @Override public void onCanGoBack(@NonNull GeckoSession s, boolean cb) { canGoBack = cb; }
            @Override public void onCanGoForward(@NonNull GeckoSession s, boolean cf) { canGoForward = cf; }
            @Override public @Nullable GeckoResult<AllowOrDeny> onLoadRequest(
                    @NonNull GeckoSession s, @NonNull LoadRequest r) {
                return GeckoResult.fromValue(AllowOrDeny.ALLOW);
            }
            @Override public @Nullable GeckoResult<String> onLoadError(
                    @NonNull GeckoSession s, @Nullable String url, @NonNull WebRequestError e) {
                BrowserLogger.getInstance().logPageError(url, e.code, e.category + "");
                if (callback != null) callback.onReceivedError(e.code, e.category + "", url);
                return null;
            }
        });

        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override public void onTitleChange(@NonNull GeckoSession s, @Nullable String title) {
                currentTitle = title;
                if (callback != null) callback.onTitleChanged(title);
            }
        });
    }

    // === IBrowserView 实现 ===

    @Override public View getView() { return this; }
    @Override public String getEngineType() { return "Gecko (Firefox 内核)"; }
    @Override public boolean isWebViewBased() { return false; }
    @Override public android.webkit.WebView getWebView() { return null; }

    @Override public void loadUrl(String url) { if (session != null) session.loadUri(url); }
    @Override public void reload() { if (session != null) session.reload(); }
    @Override public void stopLoading() { if (session != null) session.stop(); }
    @Override public boolean canGoBack() { return canGoBack; }
    @Override public boolean canGoForward() { return canGoForward; }
    @Override public void goBack() { if (session != null) session.goBack(); }
    @Override public void goForward() { if (session != null) session.goForward(); }
    @Override public String getUrl() { return currentUrl; }
    @Override public String getTitle() { return currentTitle; }
    @Override public String getCurrentUrl() { return currentUrl; }
    @Override public String getCurrentTitle() { return currentTitle; }
    @Override public int getProgress() { return progress; }
    @Override public boolean isLoading() { return loading; }

    @Override public void setSleeping(boolean sleeping) {
        this.isSleeping = sleeping;
        if (geckoView != null) geckoView.setVisibility(sleeping ? GONE : VISIBLE);
    }
    @Override public boolean isSleeping() { return isSleeping; }

    @Override public void setCallback(EdgeWebView.WebViewCallback cb) { this.callback = cb; }

    @Override public void onPause() { if (session != null) session.setActive(false); }
    @Override public void onResume() { if (session != null) session.setActive(true); }
    @Override public void destroy() {
        if (session != null) { session.close(); session = null; }
        if (geckoView != null) { geckoView.releaseSession(); geckoView = null; }
        removeAllViews();
    }

    // === WebView 专有方法 (Gecko 不支持，返回 null / 空) ===

    @Override public PrintDocumentAdapter createPrintDocumentAdapter(String name) { return null; }
    @Override public void evaluateJavascript(String script, ValueCallback<String> cb) {
        // Gecko 不支持 evaluateJavascript，通过 WebExtension 实现
        if (cb != null) cb.onReceiveValue(null);
    }
    @Override public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        if (session != null) session.loadUri("data:" + mimeType + ";base64," +
                android.util.Base64.encodeToString(data.getBytes(), android.util.Base64.NO_WRAP));
    }
    @Override public WebSettings getSettings() { return null; }
    @Override public void findNext(boolean forward) {}
    @Override public void findAllAsync(String query) {}
    @Override public void clearMatches() {}

    // === 夜间模式 ===

    @Override public void enableNightMode() {
        // Gecko 引擎暂不支持夜间模式
    }

    @Override public void disableNightMode() {
        // Gecko 引擎暂不支持夜间模式
    }

    // === JS/CSS 注入 ===

    @Override public void injectJavaScript(String js) {
        // Gecko 引擎暂不支持 JS 注入
    }

    @Override public void injectCSS(String css) {
        // Gecko 引擎暂不支持 CSS 注入
    }

    // === 视频检测 ===

    @Override public void detectVideos() {
        // Gecko 引擎暂不支持视频检测
    }

    @Override public Bitmap captureBitmap() {
        if (geckoView == null) return null;
        int w = geckoView.getWidth();
        int h = geckoView.getHeight();
        if (w <= 0 || h <= 0) return null;
        try {
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            geckoView.draw(new Canvas(bmp));
            return bmp;
        } catch (Exception e) { return null; }
    }
    @Override public float getScale() { return 1.0f; }
    @Override public int getContentHeight() { return geckoView != null ? geckoView.getHeight() : 0; }
}