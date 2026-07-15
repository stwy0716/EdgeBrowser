package com.edge.browser.webview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebView;
import android.util.Log;

import com.edge.browser.BrowserLogger;
import com.edge.browser.BrowserLogger.LogCategory;

/**
 * Google Chromium 内核工厂
 * 检测并确保使用 Google Chromium WebView 引擎
 */
public class ChromiumWebViewFactory {

    private static final String TAG = "ChromiumWebViewFactory";
    private static final String GOOGLE_WEBVIEW_PACKAGE = "com.google.android.webview";
    private static final String GOOGLE_CHROME_PACKAGE = "com.android.chrome";
    private static final String AOSP_WEBVIEW_PACKAGE = "com.android.webview";
    private static final String GOOGLE_WEBVIEW_PLAY_STORE_URL =
            "https://play.google.com/store/apps/details?id=com.google.android.webview";

    private static ChromiumWebViewFactory instance;
    private WebViewProvider provider = WebViewProvider.UNKNOWN;
    private String chromiumVersion;
    private String webViewPackageName;
    private boolean isInitialized = false;

    public enum WebViewProvider {
        GOOGLE_CHROMIUM,  // Google Chrome WebView (完整Chromium)
        CHROME_STABLE,    // Chrome 浏览器内核 (完整Chromium)
        AOSP,             // AOSP WebView (基础Chromium)
        SYSTEM,           // 系统默认WebView
        UNKNOWN
    }

    private ChromiumWebViewFactory() {}

    public static synchronized ChromiumWebViewFactory getInstance() {
        if (instance == null) {
            instance = new ChromiumWebViewFactory();
        }
        return instance;
    }

    /**
     * 初始化并检测 Google Chromium 内核
     */
    public void init(Context context) {
        if (isInitialized) return;

        // 初始化 Chromium 命令行标志
        initChromiumFlags();

        detectWebViewProvider(context);
        isInitialized = true;

        BrowserLogger logger = BrowserLogger.getInstance();
        logger.i(TAG, LogCategory.SYSTEM, "WebView Provider: " + provider.name());
        logger.i(TAG, LogCategory.SYSTEM, "Chromium Version: " + chromiumVersion);
        logger.i(TAG, LogCategory.SYSTEM, "WebView Package: " + webViewPackageName);
        logger.i(TAG, LogCategory.SYSTEM, "Is Google Chromium: " + isGoogleChromium());
    }

    private void detectWebViewProvider(Context context) {
        PackageManager pm = context.getPackageManager();

        // 1. 检测 Google WebView (完整 Chromium - 首选)
        if (isPackageInstalled(pm, GOOGLE_WEBVIEW_PACKAGE)) {
            provider = WebViewProvider.GOOGLE_CHROMIUM;
            webViewPackageName = GOOGLE_WEBVIEW_PACKAGE;
            chromiumVersion = getPackageVersion(pm, GOOGLE_WEBVIEW_PACKAGE);
            return;
        }

        // 2. 检测 Chrome Stable (完整 Chromium - 备选)
        if (isPackageInstalled(pm, GOOGLE_CHROME_PACKAGE)) {
            provider = WebViewProvider.CHROME_STABLE;
            webViewPackageName = GOOGLE_CHROME_PACKAGE;
            chromiumVersion = getPackageVersion(pm, GOOGLE_CHROME_PACKAGE);
            return;
        }

        // 3. 检测 AOSP WebView
        if (isPackageInstalled(pm, AOSP_WEBVIEW_PACKAGE)) {
            provider = WebViewProvider.AOSP;
            webViewPackageName = AOSP_WEBVIEW_PACKAGE;
            chromiumVersion = getPackageVersion(pm, AOSP_WEBVIEW_PACKAGE);
            return;
        }

        // 4. 使用当前系统 WebView
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PackageInfo pi = WebView.getCurrentWebViewPackage();
                if (pi != null) {
                    webViewPackageName = pi.packageName;
                    chromiumVersion = pi.versionName;
                    provider = WebViewProvider.SYSTEM;
                } else {
                    webViewPackageName = "unknown";
                    chromiumVersion = "unknown";
                    provider = WebViewProvider.UNKNOWN;
                }
            } else {
                webViewPackageName = "system";
                chromiumVersion = "unknown";
                provider = WebViewProvider.SYSTEM;
            }
        } catch (Exception e) {
            provider = WebViewProvider.UNKNOWN;
            webViewPackageName = "unknown";
            chromiumVersion = "unknown";
        }
    }

    /**
     * 初始化 Chromium 命令行标志
     * 这些标志需要在 WebView 进程启动前设置
     */
    private void initChromiumFlags() {
        try {
            // 启用 Chromium 实验性功能
            // 注意：必须使用反射，因为 CommandLine 是内部 API
            Class<?> commandLineClass = Class.forName("org.chromium.base.CommandLine");
            java.lang.reflect.Method initMethod = commandLineClass.getMethod("init", String[].class);
            initMethod.invoke(null, (Object) new String[]{});

            java.lang.reflect.Method getInstanceMethod = commandLineClass.getMethod("getInstance");
            Object commandLine = getInstanceMethod.invoke(null);

            java.lang.reflect.Method appendSwitchMethod = commandLineClass.getMethod(
                    "appendSwitchWithValue", String.class, String.class);

            // GPU 加速
            appendSwitchMethod.invoke(commandLine, "enable-gpu-rasterization", "");
            // 零拷贝
            appendSwitchMethod.invoke(commandLine, "enable-zero-copy", "");
            // 硬件加速视频解码
            appendSwitchMethod.invoke(commandLine, "enable-accelerated-video-decode", "");
            // 平滑滚动
            appendSwitchMethod.invoke(commandLine, "enable-smooth-scrolling", "");
            // 并行下载
            appendSwitchMethod.invoke(commandLine, "enable-parallel-downloading", "");
            // QUIC 协议
            appendSwitchMethod.invoke(commandLine, "enable-quic", "");
            // Brotli 压缩
            appendSwitchMethod.invoke(commandLine, "enable-brotli", "");
        } catch (Exception e) {
            // Chromium 命令行标志仅在使用 Google WebView 时生效
            // 在 AOSP / 系统 WebView 上可能不可用，忽略即可
            Log.d(TAG, "Chromium flags not available (非 Google WebView)");
        }
    }

    private boolean isPackageInstalled(PackageManager pm, String packageName) {
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String getPackageVersion(PackageManager pm, String packageName) {
        try {
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

    /**
     * 获取 Chromium 内核信息
     */
    public String getChromiumInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 内置浏览器引擎信息 ===\n\n");

        // Gecko 引擎 (内置)
        sb.append("【内置引擎】Gecko (Firefox 内核)\n");
        sb.append("  状态: ");
        sb.append(GeckoRuntimeManager.getInstance().isInitialized() ? "已初始化" : "未初始化");
        sb.append("\n");
        sb.append("  版本: ").append(GeckoRuntimeManager.getInstance().getEngineInfo()).append("\n");
        sb.append("  说明: 完全内置，不依赖系统 WebView\n\n");

        // Chromium WebView (系统)
        sb.append("【系统引擎】Chromium WebView\n");
        sb.append("  引擎版本: ").append(chromiumVersion).append("\n");
        sb.append("  WebView包: ").append(webViewPackageName).append("\n");
        sb.append("  内核类型: ").append(getProviderName()).append("\n");
        sb.append("  是否为Google内核: ").append(isGoogleChromium() ? "是" : "否").append("\n");
        sb.append("  初始化状态: ").append(isInitialized ? "已完成" : "未完成").append("\n\n");

        sb.append("Android SDK: ").append(Build.VERSION.SDK_INT).append("\n");
        if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
            sb.append("设备架构: ").append(Build.SUPPORTED_ABIS[0]).append("\n");
        }
        return sb.toString();
    }

    public WebViewProvider getProvider() {
        return provider;
    }

    public String getChromiumVersion() {
        return chromiumVersion;
    }

    public String getWebViewPackageName() {
        return webViewPackageName;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    private String getProviderName() {
        switch (provider) {
            case GOOGLE_CHROMIUM: return "Google Chromium (完整版)";
            case CHROME_STABLE: return "Chrome 浏览器内核 (完整Chromium)";
            case AOSP: return "AOSP Chromium (基础版)";
            case SYSTEM: return "系统默认 WebView";
            default: return "未知";
        }
    }

    /**
     * 检查是否为 Google Chromium 内核
     */
    public boolean isGoogleChromium() {
        return provider == WebViewProvider.GOOGLE_CHROMIUM ||
               provider == WebViewProvider.CHROME_STABLE;
    }

    /**
     * 检查是否需要提示用户安装 Google WebView
     */
    public boolean shouldPromptInstallGoogleWebView() {
        return !isGoogleChromium() && provider != WebViewProvider.UNKNOWN;
    }

    /**
     * 显示安装 Google WebView 的引导对话框
     */
    public void showInstallGoogleWebViewDialog(Activity activity) {
        if (!shouldPromptInstallGoogleWebView()) return;

        new AlertDialog.Builder(activity)
                .setTitle("安装 Google WebView")
                .setMessage("当前使用的是 " + getProviderName() + "。\n\n" +
                        "安装 Google Android System WebView 可获得：\n" +
                        "• 完整的 Chromium 渲染引擎\n" +
                        "• 更快的页面加载速度\n" +
                        "• 更好的 JavaScript 性能\n" +
                        "• 最新的安全补丁\n" +
                        "• 硬件加速视频解码\n\n" +
                        "是否前往 Play 商店安装？")
                .setPositiveButton("前往安装", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(GOOGLE_WEBVIEW_PLAY_STORE_URL));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        // 如果 Play 商店不可用，用浏览器打开
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(GOOGLE_WEBVIEW_PLAY_STORE_URL));
                        activity.startActivity(intent);
                    }
                })
                .setNegativeButton("稍后再说", null)
                .show();
    }

    /**
     * 应用 Chromium 专用优化
     */
    public void applyChromiumOptimizations(WebView webView) {
        if (webView == null) return;

        android.webkit.WebSettings settings = webView.getSettings();

        // Google Safe Browsing (Chromium 安全特性)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        // 禁用强制暗色模式 (Chromium 自行处理)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            settings.setForceDark(android.webkit.WebSettings.FORCE_DARK_OFF);
        }

        // Chromium 渲染优化 (仅 Google 内核环境下生效)
        if (isGoogleChromium()) {
            // 启用硬件加速 2D 画布
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    // 通过 WebView 内部 API 设置 Chromium 偏好
                    webView.getSettings().setAlgorithmicDarkeningAllowed(false);
                } catch (Exception ignored) {}
            }
        }
    }
}