package com.edge.browser.performance;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerformanceManager {

    private static PerformanceManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isMonitoring = false;
    private boolean isEfficiencyMode = false;
    private boolean isBatteryOptimized = false;
    private final Map<String, PerformanceStats> tabStats = new HashMap<>();
    private final List<PerformanceListener> listeners = new ArrayList<>();
    private Runnable monitorRunnable;

    public static class PerformanceStats {
        public String tabId;
        public long cpuUsage = 0;
        public long memoryUsage = 0;
        public long lastUpdateTime;
        public boolean isHighLoad = false;

        public PerformanceStats(String tabId) {
            this.tabId = tabId;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

    public interface PerformanceListener {
        void onStatsUpdated(Map<String, PerformanceStats> stats);
        void onHighLoadDetected(String tabId);
    }

    private PerformanceManager() {
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMonitoring) {
                    updateStats();
                    handler.postDelayed(this, 5000); // Update every 5 seconds
                }
            }
        };
    }

    public static synchronized PerformanceManager getInstance() {
        if (instance == null) {
            instance = new PerformanceManager();
        }
        return instance;
    }

    public void startMonitoring() {
        isMonitoring = true;
        handler.post(monitorRunnable);
    }

    public void stopMonitoring() {
        isMonitoring = false;
        handler.removeCallbacks(monitorRunnable);
    }

    public void addListener(PerformanceListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PerformanceListener listener) {
        listeners.remove(listener);
    }

    private void updateStats() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        for (PerformanceStats stats : tabStats.values()) {
            stats.memoryUsage = usedMemory / (Math.max(tabStats.size(), 1));
            stats.cpuUsage = estimateCpuUsage();
            stats.isHighLoad = stats.memoryUsage > maxMemory * 0.8;
            stats.lastUpdateTime = System.currentTimeMillis();
        }

        for (PerformanceListener listener : listeners) {
            listener.onStatsUpdated(tabStats);
        }
    }

    private long estimateCpuUsage() {
        // Estimate CPU usage (simplified)
        return (long) (Math.random() * 30); // Mock: 0-30%
    }

    public void registerTab(String tabId) {
        if (!tabStats.containsKey(tabId)) {
            tabStats.put(tabId, new PerformanceStats(tabId));
        }
    }

    public void unregisterTab(String tabId) {
        tabStats.remove(tabId);
    }

    public PerformanceStats getTabStats(String tabId) {
        return tabStats.get(tabId);
    }

    public Map<String, PerformanceStats> getAllStats() {
        return new HashMap<>(tabStats);
    }

    public void setEfficiencyMode(boolean enabled) {
        this.isEfficiencyMode = enabled;
        if (enabled) {
            // Reduce background activity, lower CPU usage
            applyEfficiencySettings();
        }
    }

    public boolean isEfficiencyMode() {
        return isEfficiencyMode;
    }

    public void setBatteryOptimized(boolean optimized) {
        this.isBatteryOptimized = optimized;
    }

    public boolean isBatteryOptimized() {
        return isBatteryOptimized;
    }

    private void applyEfficiencySettings() {
        // Reduce animation, limit background scripts, etc.
    }

    public void onPause() {
        if (isBatteryOptimized) {
            // Reduce resource usage
        }
    }

    public void onResume() {
        // Restore normal resource usage
    }
}