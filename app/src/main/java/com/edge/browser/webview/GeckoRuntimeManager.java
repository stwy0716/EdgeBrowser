package com.edge.browser.webview;

import android.content.Context;
import android.util.Log;

import com.edge.browser.BrowserLogger;
import com.edge.browser.BrowserLogger.LogCategory;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;

/**
 * GeckoRuntime 管理器
 * 全局单例，管理 Gecko 引擎生命周期
 */
public class GeckoRuntimeManager {

    private static final String TAG = "GeckoRuntimeManager";
    private static GeckoRuntimeManager instance;
    private GeckoRuntime runtime;
    private boolean isInitialized = false;

    private GeckoRuntimeManager() {}

    public static synchronized GeckoRuntimeManager getInstance() {
        if (instance == null) {
            instance = new GeckoRuntimeManager();
        }
        return instance;
    }

    /**
     * 初始化 Gecko 引擎 (GeckoView = Firefox 内核)
     */
    public synchronized void init(Context context) {
        if (isInitialized || runtime != null) return;

        try {
            GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder()
                    .javaScriptEnabled(true)
                    .remoteDebuggingEnabled(false)
                    .consoleOutput(true)
                    .aboutConfigEnabled(false)
                    .automaticFontSizeAdjustment(true)
                    .fontSizeFactor(1.0f)
                    .inputAutoZoomEnabled(true)
                    .loginAutofillEnabled(true)
                    .build();

            runtime = GeckoRuntime.create(context, settings);

            BrowserLogger.getInstance().i(TAG, LogCategory.SYSTEM,
                    "Gecko 引擎初始化成功 (Firefox 内核)");
            isInitialized = true;
        } catch (Throwable t) {
            Log.e(TAG, "Gecko 引擎初始化失败", t);
            try {
                BrowserLogger.getInstance().logCrash("GeckoRuntime init", t instanceof Exception ? (Exception) t : new Exception(t));
            } catch (Throwable ignored) {}
            isInitialized = false;
        }
    }

    public GeckoRuntime getRuntime(Context context) {
        if (runtime == null && !isInitialized) {
            init(context);
        }
        return runtime;
    }

    public boolean isInitialized() {
        return isInitialized && runtime != null;
    }

    public String getEngineInfo() {
        if (runtime == null) return "Gecko 引擎未初始化";
        try {
            return "Gecko 152 (Firefox 内核)";
        } catch (Exception e) {
            return "Gecko Engine (Firefox)";
        }
    }

    public void shutdown() {
        if (runtime != null) {
            runtime.shutdown();
            runtime = null;
        }
        isInitialized = false;
    }
}