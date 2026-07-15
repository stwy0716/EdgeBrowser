package com.edge.browser.download;

import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DownloadManager {

    private static DownloadManager instance;
    private final Context context;
    private final CopyOnWriteArrayList<DownloadService.DownloadTask> downloads;
    private final List<DownloadListener> listeners;

    public interface DownloadListener {
        void onDownloadStarted(DownloadService.DownloadTask task);
        void onDownloadProgress(DownloadService.DownloadTask task);
        void onDownloadCompleted(DownloadService.DownloadTask task);
        void onDownloadFailed(DownloadService.DownloadTask task);
        void onDownloadCancelled(DownloadService.DownloadTask task);
    }

    private DownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.downloads = new CopyOnWriteArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public static synchronized DownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadManager(context);
        }
        return instance;
    }

    public void addListener(DownloadListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DownloadListener listener) {
        listeners.remove(listener);
    }

    public void startDownload(String url, String fileName, String category) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra("action", "download");
        intent.putExtra("url", url);
        intent.putExtra("filename", fileName);
        intent.putExtra("category", category);
        context.startService(intent);
    }

    public void pauseAll() {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra("action", "pause_all");
        context.startService(intent);
    }

    public void resumeAll() {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra("action", "resume_all");
        context.startService(intent);
    }

    public void cancelDownload(String downloadId) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra("action", "cancel");
        intent.putExtra("download_id", downloadId);
        context.startService(intent);
    }

    public void onDownloadStarted(DownloadService.DownloadTask task) {
        downloads.add(task);
        for (DownloadListener listener : listeners) {
            listener.onDownloadStarted(task);
        }
    }

    public void onDownloadProgress(DownloadService.DownloadTask task) {
        for (DownloadListener listener : listeners) {
            listener.onDownloadProgress(task);
        }
    }

    public void onDownloadCompleted(DownloadService.DownloadTask task) {
        for (DownloadListener listener : listeners) {
            listener.onDownloadCompleted(task);
        }
    }

    public void onDownloadFailed(DownloadService.DownloadTask task) {
        for (DownloadListener listener : listeners) {
            listener.onDownloadFailed(task);
        }
    }

    public void onDownloadCancelled(DownloadService.DownloadTask task) {
        downloads.remove(task);
        for (DownloadListener listener : listeners) {
            listener.onDownloadCancelled(task);
        }
    }

    public List<DownloadService.DownloadTask> getAllDownloads() {
        return new ArrayList<>(downloads);
    }

    public List<DownloadService.DownloadTask> getActiveDownloads() {
        List<DownloadService.DownloadTask> active = new ArrayList<>();
        for (DownloadService.DownloadTask task : downloads) {
            if (!task.isCompleted() && !task.isFailed() && !task.isCancelled()) {
                active.add(task);
            }
        }
        return active;
    }

    public List<DownloadService.DownloadTask> getCompletedDownloads() {
        List<DownloadService.DownloadTask> completed = new ArrayList<>();
        for (DownloadService.DownloadTask task : downloads) {
            if (task.isCompleted()) completed.add(task);
        }
        return completed;
    }

    public void clearHistory() {
        downloads.clear();
    }
}