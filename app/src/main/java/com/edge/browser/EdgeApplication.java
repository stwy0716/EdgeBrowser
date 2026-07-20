package com.edge.browser;

import android.app.Application;
import android.util.Log;
import androidx.multidex.MultiDexApplication;

import com.edge.browser.webview.ChromiumWebViewFactory;

public class EdgeApplication extends MultiDexApplication {

    private static final String TAG = "EdgeApplication";
    private static EdgeApplication instance;
    private static MainActivity mainActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 初始化 Chromium 内核检测
        try {
            ChromiumWebViewFactory.getInstance().init(this);
        } catch (Throwable t) {
            Log.e(TAG, "ChromiumWebViewFactory init failed", t);
        }

        // 初始化日志系统
        try {
            BrowserLogger.getInstance().init(this);
        } catch (Throwable t) {
            Log.e(TAG, "BrowserLogger init failed", t);
        }
    }

    public static EdgeApplication getInstance() {
        return instance;
    }

    public static void setMainActivity(MainActivity activity) {
        mainActivity = activity;
    }

    public static MainActivity getMainActivity() {
        return mainActivity;
    }
}