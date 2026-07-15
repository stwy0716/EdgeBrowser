package com.edge.browser.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.edge.browser.MainActivity;
import com.edge.browser.R;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DownloadService extends Service {

    private static final String CHANNEL_ID = "edge_downloads";
    private static final int NOTIFICATION_ID = 1001;
    private NotificationManager notificationManager;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ConcurrentHashMap<String, DownloadTask> activeTasks = new ConcurrentHashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if ("download".equals(action)) {
                String url = intent.getStringExtra("url");
                String fileName = intent.getStringExtra("filename");
                String category = intent.getStringExtra("category");
                startDownload(url, fileName, category);
            } else if ("pause_all".equals(action)) {
                pauseAllDownloads();
            } else if ("resume_all".equals(action)) {
                resumeAllDownloads();
            } else if ("cancel".equals(action)) {
                String downloadId = intent.getStringExtra("download_id");
                cancelDownload(downloadId);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        return START_STICKY;
    }

    private void startDownload(String url, String fileName, String category) {
        DownloadTask task = new DownloadTask(url, fileName, category);
        activeTasks.put(task.getId(), task);
        Future<?> future = executor.submit(() -> executeDownload(task));
        task.setFuture(future);
        DownloadManager.getInstance(this).onDownloadStarted(task);
    }

    private void executeDownload(DownloadTask task) {
        try {
            URL url = new URL(task.getUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            // Support parallel download
            long fileSize = connection.getContentLengthLong();
            task.setTotalSize(fileSize);

            if (task.getResumePosition() > 0) {
                connection.setRequestProperty("Range", "bytes=" + task.getResumePosition() + "-");
            }

            File outputDir = getDownloadDir(task.getCategory());
            if (!outputDir.exists()) outputDir.mkdirs();

            File outputFile = new File(outputDir, task.getFileName());
            if (task.getResumePosition() == 0 && outputFile.exists()) {
                outputFile.delete();
            }

            InputStream inputStream = connection.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(outputFile, task.getResumePosition() > 0);

            byte[] buffer = new byte[8192];
            int bytesRead;
            long downloaded = task.getResumePosition();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (task.isPaused()) {
                    task.setResumePosition(downloaded);
                    inputStream.close();
                    outputStream.close();
                    return;
                }

                outputStream.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                task.setDownloadedSize(downloaded);

                final long finalDownloaded = downloaded;
                final long finalFileSize = fileSize;
                handler.post(() -> {
                    task.setProgress((int) ((finalDownloaded * 100) / Math.max(finalFileSize, 1)));
                    DownloadManager.getInstance(DownloadService.this).onDownloadProgress(task);
                });
            }

            inputStream.close();
            outputStream.close();
            task.setCompleted(true);
            handler.post(() -> DownloadManager.getInstance(DownloadService.this).onDownloadCompleted(task));
            activeTasks.remove(task.getId());

        } catch (Exception e) {
            task.setFailed(true);
            task.setErrorMessage(e.getMessage());
            handler.post(() -> DownloadManager.getInstance(DownloadService.this).onDownloadFailed(task));
            activeTasks.remove(task.getId());
        }
    }

    private File getDownloadDir(String category) {
        File baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File edgeDir = new File(baseDir, "EdgeBrowser");
        if (category != null) {
            switch (category) {
                case "images": return new File(edgeDir, "Images");
                case "documents": return new File(edgeDir, "Documents");
                case "packages": return new File(edgeDir, "Packages");
                default: return edgeDir;
            }
        }
        return edgeDir;
    }

    private void pauseAllDownloads() {
        for (DownloadTask task : activeTasks.values()) {
            task.setPaused(true);
        }
    }

    private void resumeAllDownloads() {
        for (DownloadTask task : activeTasks.values()) {
            if (task.isPaused()) {
                task.setPaused(false);
                Future<?> future = executor.submit(() -> executeDownload(task));
                task.setFuture(future);
            }
        }
    }

    private void cancelDownload(String downloadId) {
        DownloadTask task = activeTasks.get(downloadId);
        if (task != null) {
            if (task.getFuture() != null) {
                task.getFuture().cancel(true);
            }
            task.setCancelled(true);
            activeTasks.remove(downloadId);
            DownloadManager.getInstance(this).onDownloadCancelled(task);
        }
    }

    private Notification buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Edge 下载管理器")
                .setContentText(activeTasks.size() + " 个下载任务")
                .setSmallIcon(R.drawable.ic_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        Intent intent = new Intent(this, MainActivity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "下载通知", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // === DownloadTask ===

    public static class DownloadTask {
        private String id;
        private String url;
        private String fileName;
        private String category;
        private long totalSize;
        private long downloadedSize;
        private long resumePosition;
        private int progress;
        private boolean isPaused;
        private boolean isCompleted;
        private boolean isFailed;
        private boolean isCancelled;
        private String errorMessage;
        private Future<?> future;
        private long createdAt;

        public DownloadTask(String url, String fileName, String category) {
            this.id = java.util.UUID.randomUUID().toString();
            this.url = url;
            this.fileName = fileName != null ? fileName : getFileNameFromUrl(url);
            this.category = category;
            this.createdAt = System.currentTimeMillis();
            this.progress = 0;
        }

        private String getFileNameFromUrl(String url) {
            try {
                String path = new URL(url).getPath();
                String name = path.substring(path.lastIndexOf('/') + 1);
                if (name.isEmpty()) name = "download_" + System.currentTimeMillis();
                return name;
            } catch (Exception e) {
                return "download_" + System.currentTimeMillis();
            }
        }

        // Getters and Setters
        public String getId() { return id; }
        public String getUrl() { return url; }
        public String getFileName() { return fileName; }
        public String getCategory() { return category; }
        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
        public long getDownloadedSize() { return downloadedSize; }
        public void setDownloadedSize(long downloadedSize) { this.downloadedSize = downloadedSize; }
        public long getResumePosition() { return resumePosition; }
        public void setResumePosition(long resumePosition) { this.resumePosition = resumePosition; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public boolean isPaused() { return isPaused; }
        public void setPaused(boolean paused) { isPaused = paused; }
        public boolean isCompleted() { return isCompleted; }
        public void setCompleted(boolean completed) { isCompleted = completed; }
        public boolean isFailed() { return isFailed; }
        public void setFailed(boolean failed) { isFailed = failed; }
        public boolean isCancelled() { return isCancelled; }
        public void setCancelled(boolean cancelled) { isCancelled = cancelled; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Future<?> getFuture() { return future; }
        public void setFuture(Future<?> future) { this.future = future; }
        public long getCreatedAt() { return createdAt; }
    }
}