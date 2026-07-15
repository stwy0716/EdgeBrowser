package com.edge.browser;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 浏览器日志系统
 * 同时输出到 logcat 和文件
 */
public class BrowserLogger {

    private static final String TAG = "EdgeBrowser";
    private static final String LOG_DIR = "EdgeBrowser/logs";
    private static final int MAX_LOG_FILES = 10;
    private static final long MAX_LOG_SIZE = 5 * 1024 * 1024; // 5MB

    private static BrowserLogger instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile File logDir;
    private volatile File currentLogFile;
    private volatile boolean isEnabled = true;
    private volatile boolean isFileLogging = true;
    private volatile LogLevel minimumLevel = LogLevel.DEBUG;

    public enum LogLevel {
        VERBOSE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4);
        final int level;
        LogLevel(int level) { this.level = level; }
    }

    public enum LogCategory {
        BROWSER,      // 浏览器核心
        TAB,          // 标签页操作
        NAVIGATION,   // 页面导航
        DOWNLOAD,     // 下载
        PERFORMANCE,  // 性能
        PRIVACY,      // 隐私安全
        MEDIA,        // 媒体播放
        EXTENSION,    // 扩展
        SYSTEM,       // 系统
        CRASH         // 崩溃
    }

    private BrowserLogger() {}

    public static synchronized BrowserLogger getInstance() {
        if (instance == null) {
            instance = new BrowserLogger();
        }
        return instance;
    }

    public void init(Context context) {
        executor.submit(() -> {
            try {
                File baseDir = null;
                try {
                    baseDir = context.getExternalFilesDir(null);
                } catch (Exception e) {
                    Log.w(TAG, "getExternalFilesDir failed, using internal storage", e);
                }
                if (baseDir == null) {
                    baseDir = context.getFilesDir();
                }
                if (baseDir == null) {
                    Log.e(TAG, "Cannot get files directory, file logging disabled");
                    isFileLogging = false;
                    return;
                }
                logDir = new File(baseDir, LOG_DIR);
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }
                rotateLogFile();
                i(TAG, LogCategory.SYSTEM, "=== 浏览器日志系统启动 ===");
            } catch (Exception e) {
                Log.e(TAG, "日志系统初始化失败", e);
                isFileLogging = false;
            }
        });
    }

    private void rotateLogFile() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = "browser_" + sdf.format(new Date()) + ".log";
        currentLogFile = new File(logDir, fileName);

        // 清理旧日志文件
        File[] files = logDir.listFiles((dir, name) -> name.startsWith("browser_") && name.endsWith(".log"));
        if (files != null && files.length > MAX_LOG_FILES) {
            // 删除最旧的文件
            java.util.Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            for (int i = 0; i < files.length - MAX_LOG_FILES; i++) {
                files[i].delete();
            }
        }
    }

    public void v(String tag, LogCategory category, String message) {
        log(LogLevel.VERBOSE, tag, category, message, null);
    }

    public void d(String tag, LogCategory category, String message) {
        log(LogLevel.DEBUG, tag, category, message, null);
    }

    public void i(String tag, LogCategory category, String message) {
        log(LogLevel.INFO, tag, category, message, null);
    }

    public void w(String tag, LogCategory category, String message) {
        log(LogLevel.WARN, tag, category, message, null);
    }

    public void e(String tag, LogCategory category, String message, Throwable throwable) {
        log(LogLevel.ERROR, tag, category, message, throwable);
    }

    public void e(String tag, LogCategory category, String message) {
        log(LogLevel.ERROR, tag, category, message, null);
    }

    private void log(LogLevel level, String tag, LogCategory category, String message, Throwable throwable) {
        if (!isEnabled) return;
        if (level.level < minimumLevel.level) return;

        final String logLine = formatLogLine(level, tag, category, message, throwable);

        // 输出到 logcat
        switch (level) {
            case VERBOSE: Log.v(tag, logLine); break;
            case DEBUG: Log.d(tag, logLine); break;
            case INFO: Log.i(tag, logLine); break;
            case WARN: Log.w(tag, logLine); break;
            case ERROR: Log.e(tag, logLine); break;
        }

        // 写入文件
        if (isFileLogging) {
            executor.submit(() -> writeToFile(logLine));
        }
    }

    private String formatLogLine(LogLevel level, String tag, LogCategory category,
                                  String message, Throwable throwable) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        sb.append(sdf.format(new Date()));
        sb.append(" [").append(level.name()).append("]");
        sb.append(" [").append(category.name()).append("]");
        sb.append(" [").append(tag).append("] ");
        sb.append(message);

        if (throwable != null) {
            sb.append("\n").append(getStackTrace(throwable));
        }

        return sb.toString();
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private void writeToFile(String logLine) {
        try {
            if (currentLogFile == null) return;

            // 检查文件大小
            if (currentLogFile.length() > MAX_LOG_SIZE) {
                rotateLogFile();
                writeToFile("=== 日志文件滚动，继续记录 ===");
            }

            FileWriter writer = new FileWriter(currentLogFile, true);
            writer.write(logLine);
            writer.write("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "写入日志文件失败", e);
        }
    }

    // 便捷方法

    /** 记录页面加载 */
    public void logPageLoad(String url, long loadTimeMs) {
        i(TAG, LogCategory.NAVIGATION,
                String.format("页面加载: %s (耗时: %dms)", url, loadTimeMs));
    }

    /** 记录页面加载失败 */
    public void logPageError(String url, int errorCode, String description) {
        e(TAG, LogCategory.NAVIGATION,
                String.format("页面加载失败: %s (错误: %d - %s)", url, errorCode, description));
    }

    /** 记录标签操作 */
    public void logTabAction(String action, String tabTitle) {
        i(TAG, LogCategory.TAB,
                String.format("标签操作: %s - %s", action, tabTitle));
    }

    /** 记录下载 */
    public void logDownload(String action, String fileName, long size) {
        i(TAG, LogCategory.DOWNLOAD,
                String.format("下载: %s - %s (%.2f MB)", action, fileName, size / (1024.0 * 1024)));
    }

    /** 记录崩溃 */
    public void logCrash(String context, Throwable throwable) {
        e(TAG, LogCategory.CRASH,
                String.format("崩溃: %s", context), throwable);
    }

    /** 记录性能 */
    public void logPerformance(String metric, long value) {
        d(TAG, LogCategory.PERFORMANCE,
                String.format("性能: %s = %d", metric, value));
    }

    /** 获取日志文件列表 */
    public File[] getLogFiles() {
        if (logDir == null) return new File[0];
        return logDir.listFiles((dir, name) -> name.endsWith(".log"));
    }

    /** 获取当前日志文件路径 */
    public String getCurrentLogPath() {
        return currentLogFile != null ? currentLogFile.getAbsolutePath() : "";
    }

    /** 获取所有日志内容 */
    public String getAllLogs() {
        StringBuilder sb = new StringBuilder();
        File[] files = getLogFiles();
        if (files != null) {
            for (File file : files) {
                sb.append("=== ").append(file.getName()).append(" ===\n");
                try {
                    sb.append(new String(java.nio.file.Files.readAllBytes(file.toPath())));
                } catch (Exception e) {
                    sb.append("无法读取: ").append(e.getMessage());
                }
                sb.append("\n\n");
            }
        }
        return sb.toString();
    }

    /** 清除所有日志 */
    public void clearLogs() {
        executor.submit(() -> {
            File[] files = getLogFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            rotateLogFile();
        });
    }

    /** 导出日志 */
    public File exportLogs() {
        try {
            File exportDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            File exportFile = new File(exportDir,
                    "EdgeBrowser_logs_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            .format(new Date()) + ".txt");
            FileWriter writer = new FileWriter(exportFile);
            writer.write(getAllLogs());
            writer.flush();
            writer.close();
            return exportFile;
        } catch (Exception e) {
            Log.e(TAG, "导出日志失败", e);
            return null;
        }
    }

    // 设置
    public void setEnabled(boolean enabled) { this.isEnabled = enabled; }
    public boolean isEnabled() { return isEnabled; }
    public void setFileLogging(boolean fileLogging) { this.isFileLogging = fileLogging; }
    public boolean isFileLogging() { return isFileLogging; }
    public void setMinimumLevel(LogLevel level) { this.minimumLevel = level; }
    public LogLevel getMinimumLevel() { return minimumLevel; }
}