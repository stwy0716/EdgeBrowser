package com.edge.browser.performance;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;

public class StartupBoostService extends Service {

    private static final String TAG = "StartupBoostService";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPreloaded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        // Preload core browser process
        preloadCore();
    }

    private void preloadCore() {
        if (!isPreloaded) {
            // Pre-initialize WebView engine on main thread
            handler.post(() -> {
                try {
                    android.webkit.WebView webView = new android.webkit.WebView(
                            getApplicationContext());
                    webView.destroy();
                    isPreloaded = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isPreloaded = false;
    }
}