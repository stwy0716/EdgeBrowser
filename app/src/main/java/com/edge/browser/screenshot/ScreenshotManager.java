package com.edge.browser.screenshot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ScreenshotManager {

    private static ScreenshotManager instance;
    public static final int REQUEST_SCREENSHOT = 2001;

    public enum ScreenshotMode {
        REGION,      // Region screenshot
        FULL_PAGE,   // Full page long screenshot
        FREE_SELECT  // Free selection
    }

    private ScreenshotMode currentMode = ScreenshotMode.REGION;
    private ScreenshotCallback callback;

    public interface ScreenshotCallback {
        void onScreenshotReady(Bitmap bitmap);
        void onScreenshotSaved(String filePath);
        void onScreenshotError(String error);
    }

    private ScreenshotManager() {}

    public static synchronized ScreenshotManager getInstance() {
        if (instance == null) {
            instance = new ScreenshotManager();
        }
        return instance;
    }

    public void setCallback(ScreenshotCallback callback) {
        this.callback = callback;
    }

    public void setMode(ScreenshotMode mode) {
        this.currentMode = mode;
    }

    public ScreenshotMode getMode() {
        return currentMode;
    }

    public void captureScreenshot(Activity activity, WebView webView) {
        if (webView == null) return;

        switch (currentMode) {
            case REGION:
                captureRegion(activity, webView);
                break;
            case FULL_PAGE:
                captureFullPage(webView);
                break;
            case FREE_SELECT:
                captureRegion(activity, webView);
                break;
        }
    }

    private void captureRegion(Activity activity, WebView webView) {
        Bitmap bitmap = captureWebViewBitmap(webView);
        if (bitmap != null) {
            if (callback != null) callback.onScreenshotReady(bitmap);
            saveBitmap(bitmap, "screenshot_region_" + System.currentTimeMillis() + ".png");
        }
    }

    private void captureFullPage(WebView webView) {
        // Capture full page by scrolling
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            float scale = webView.getScale();
            int webViewHeight = (int) (webView.getContentHeight() * scale);
            Bitmap fullBitmap = Bitmap.createBitmap(webView.getWidth(), webViewHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(fullBitmap);
            canvas.scale(scale, scale);

            int currentScrollY = webView.getScrollY();
            webView.scrollTo(0, 0);

            int capturedHeight = 0;
            int viewportHeight = webView.getHeight();

            while (capturedHeight < webViewHeight) {
                webView.scrollTo(0, capturedHeight);
                Bitmap chunk = captureWebViewBitmap(webView);
                if (chunk != null) {
                    canvas.drawBitmap(chunk, 0, capturedHeight, null);
                    capturedHeight += viewportHeight;
                } else {
                    break;
                }
            }

            webView.scrollTo(0, currentScrollY);

            if (callback != null) callback.onScreenshotReady(fullBitmap);
            saveBitmap(fullBitmap, "screenshot_full_" + System.currentTimeMillis() + ".png");
        }
    }

    private Bitmap captureWebViewBitmap(WebView webView) {
        Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        webView.draw(canvas);
        return bitmap;
    }

    public void saveBitmap(Bitmap bitmap, String fileName) {
        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "EdgeBrowser");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, fileName);
            OutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            if (callback != null) callback.onScreenshotSaved(file.getAbsolutePath());
        } catch (Exception e) {
            if (callback != null) callback.onScreenshotError(e.getMessage());
        }
    }

    public void shareScreenshot(Context context, Bitmap bitmap) {
        try {
            File dir = new File(context.getCacheDir(), "screenshots");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "share_screenshot.png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Uri uri = androidx.core.content.FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "分享截图"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleScreenshotResult(int resultCode, Intent data) {
        // Handle screenshot result from external activity
    }
}