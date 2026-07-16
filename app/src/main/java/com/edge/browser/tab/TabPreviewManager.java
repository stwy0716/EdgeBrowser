package com.edge.browser.tab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.View;

import com.edge.browser.BrowserLogger;
import com.edge.browser.BrowserLogger.LogCategory;

import java.io.File;
import java.io.FileOutputStream;

public class TabPreviewManager {

    private static final String TAG = "TabPreviewManager";
    private static final String PREVIEW_DIR = "tab_previews";
    private static final int PREVIEW_WIDTH = 240;
    private static final int PREVIEW_HEIGHT = 160;

    private final File previewDir;

    public TabPreviewManager(Context context) {
        previewDir = new File(context.getFilesDir(), PREVIEW_DIR);
        if (!previewDir.exists()) {
            previewDir.mkdirs();
        }
        BrowserLogger.getInstance().d(TAG, LogCategory.TAB, "TabPreviewManager initialized");
    }

    public Bitmap captureTabPreview(View webView) {
        if (webView == null) return null;

        try {
            int width = webView.getWidth();
            int height = webView.getHeight();
            if (width <= 0 || height <= 0) return null;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            webView.draw(canvas);

            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, PREVIEW_WIDTH, PREVIEW_HEIGHT, true);
            if (scaled != bitmap) {
                bitmap.recycle();
            }
            return scaled;
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, LogCategory.TAB, "Failed to capture tab preview", e);
            return null;
        }
    }

    public void savePreview(String tabId, Bitmap bitmap) {
        if (tabId == null || bitmap == null) return;

        FileOutputStream fos = null;
        try {
            File file = new File(previewDir, tabId + ".png");
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, fos);
            fos.flush();
            BrowserLogger.getInstance().d(TAG, LogCategory.TAB, "Preview saved: " + tabId);
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, LogCategory.TAB, "Failed to save preview: " + tabId, e);
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (Exception ignored) {}
            }
        }
    }

    public Bitmap loadPreview(String tabId) {
        if (tabId == null) return null;

        File file = new File(previewDir, tabId + ".png");
        if (!file.exists()) return null;

        try {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, LogCategory.TAB, "Failed to load preview: " + tabId, e);
            return null;
        }
    }

    public void deletePreview(String tabId) {
        if (tabId == null) return;

        File file = new File(previewDir, tabId + ".png");
        if (file.exists()) {
            file.delete();
            BrowserLogger.getInstance().d(TAG, LogCategory.TAB, "Preview deleted: " + tabId);
        }
    }
}