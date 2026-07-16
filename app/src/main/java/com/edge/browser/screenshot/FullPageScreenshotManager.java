package com.edge.browser.screenshot;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;

import com.edge.browser.BrowserLogger;
import com.edge.browser.BrowserLogger.LogCategory;
import com.edge.browser.webview.EdgeWebView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FullPageScreenshotManager {

    private static final String TAG = "FullPageScreenshotManager";
    private static final int SCROLL_DELAY_MS = 300;

    private static FullPageScreenshotManager instance;
    private final Handler mainHandler;
    private final ExecutorService executor;

    public interface FullPageCallback {
        void onProgress(int current, int total);
        void onComplete(Bitmap fullPageBitmap);
        void onError(String error);
    }

    private FullPageScreenshotManager() {
        mainHandler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();
        BrowserLogger.getInstance().d(TAG, LogCategory.SYSTEM, "FullPageScreenshotManager initialized");
    }

    public static synchronized FullPageScreenshotManager getInstance() {
        if (instance == null) {
            instance = new FullPageScreenshotManager();
        }
        return instance;
    }

    public void captureFullPage(EdgeWebView webView, FullPageCallback callback) {
        if (webView == null) {
            if (callback != null) callback.onError("WebView is null");
            return;
        }

        executor.submit(() -> {
            try {
                int webViewWidth = webView.getWidth();
                if (webViewWidth <= 0) {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError("WebView width is 0");
                    });
                    return;
                }

                float scale = webView.getScale();
                int webViewHeight = (int) (webView.getContentHeight() * scale);
                if (webViewHeight <= 0) {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError("WebView content height is 0");
                    });
                    return;
                }

                Bitmap fullBitmap = Bitmap.createBitmap(webViewWidth, webViewHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(fullBitmap);
                canvas.scale(scale, scale);

                int currentScrollY = webView.getScrollY();
                int viewportHeight = webView.getHeight();
                if (viewportHeight <= 0) {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError("Viewport height is 0");
                    });
                    return;
                }

                int totalChunks = (int) Math.ceil((float) webViewHeight / viewportHeight);
                int capturedHeight = 0;
                int chunkIndex = 0;

                while (chunkIndex < totalChunks) {
                    final int currentChunk = chunkIndex + 1;
                    int scrollTo = Math.min(capturedHeight, webViewHeight - 1);

                    final int finalCapturedHeight = capturedHeight;
                    mainHandler.post(() -> {
                        webView.scrollTo(0, scrollTo);
                    });

                    try {
                        Thread.sleep(SCROLL_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    Bitmap chunk = captureWebViewBitmap(webView);
                    if (chunk != null) {
                        canvas.drawBitmap(chunk, 0, finalCapturedHeight, null);
                        chunk.recycle();
                    }

                    capturedHeight += viewportHeight;
                    chunkIndex++;

                    final int progress = chunkIndex;
                    mainHandler.post(() -> {
                        if (callback != null) callback.onProgress(progress, totalChunks);
                    });
                }

                mainHandler.post(() -> webView.scrollTo(0, currentScrollY));

                mainHandler.post(() -> {
                    if (callback != null) callback.onComplete(fullBitmap);
                });

                BrowserLogger.getInstance().d(TAG, LogCategory.SYSTEM,
                        "Full page screenshot captured: " + webViewWidth + "x" + webViewHeight);
            } catch (Exception e) {
                BrowserLogger.getInstance().e(TAG, LogCategory.SYSTEM, "Full page screenshot failed", e);
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        });
    }

    private Bitmap captureWebViewBitmap(EdgeWebView webView) {
        try {
            int width = webView.getWidth();
            int height = webView.getHeight();
            if (width <= 0 || height <= 0) return null;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            webView.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, LogCategory.SYSTEM, "Failed to capture chunk", e);
            return null;
        }
    }
}