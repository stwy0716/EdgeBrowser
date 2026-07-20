package com.edge.browser;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import androidx.multidex.MultiDexApplication;

import com.edge.browser.webview.ChromiumWebViewFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EdgeApplication extends MultiDexApplication {

    private static final String TAG = "EdgeApplication";
    private static EdgeApplication instance;
    private static MainActivity mainActivity;
    private static File crashLogFile;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // 最早的崩溃捕获点
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String crashInfo = formatCrash(throwable);
            Log.e(TAG, "FATAL CRASH: " + crashInfo);
            writeCrashLog(crashInfo);
            // 交给系统默认处理
            System.exit(1);
        });
        Log.i(TAG, "[STARTUP] attachBaseContext OK");
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "[STARTUP] onCreate begin");
        try {
            super.onCreate();
            Log.i(TAG, "[STARTUP] super.onCreate() OK");
        } catch (Throwable t) {
            Log.e(TAG, "[STARTUP] super.onCreate() FAILED", t);
            writeCrashLog(formatCrash(t));
            return;
        }
        instance = this;

        // 初始化崩溃日志文件
        try {
            crashLogFile = new File(getExternalFilesDir(null), "crash_log.txt");
            Log.i(TAG, "[STARTUP] crash log file: " + crashLogFile);
        } catch (Throwable t) {
            Log.e(TAG, "[STARTUP] crash log file init failed", t);
        }

        Log.i(TAG, "[STARTUP] init ChromiumWebViewFactory...");
        try {
            ChromiumWebViewFactory.getInstance().init(this);
            Log.i(TAG, "[STARTUP] ChromiumWebViewFactory OK");
        } catch (Throwable t) {
            Log.e(TAG, "[STARTUP] ChromiumWebViewFactory FAILED", t);
            writeCrashLog(formatCrash(t));
        }

        Log.i(TAG, "[STARTUP] init BrowserLogger...");
        try {
            BrowserLogger.getInstance().init(this);
            Log.i(TAG, "[STARTUP] BrowserLogger OK");
        } catch (Throwable t) {
            Log.e(TAG, "[STARTUP] BrowserLogger FAILED", t);
            writeCrashLog(formatCrash(t));
        }

        Log.i(TAG, "[STARTUP] onCreate done");
    }

    private static String formatCrash(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date()));
        pw.println("Thread: " + Thread.currentThread().getName());
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private static void writeCrashLog(String crashInfo) {
        try {
            File file = crashLogFile;
            if (file == null) {
                // 回退到外部存储
                file = new File(Environment.getExternalStorageDirectory(), "edge_browser_crash.txt");
            }
            FileWriter fw = new FileWriter(file, true);
            fw.write(crashInfo);
            fw.write("\n---\n");
            fw.flush();
            fw.close();
            Log.e(TAG, "Crash log written to: " + file.getAbsolutePath());
        } catch (Throwable ignored) {
            Log.e(TAG, "Failed to write crash log", ignored);
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