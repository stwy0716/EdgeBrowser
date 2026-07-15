package com.edge.browser.tab;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

public class SleepingTabManager {

    private static SleepingTabManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long sleepTimeoutMs = 60000; // Default 1 minute
    private boolean isEnabled = true;
    private boolean isEfficiencyMode = false;
    private final List<SleepListener> listeners = new ArrayList<>();
    private Runnable sleepCheckRunnable;
    private boolean isRunning = false;

    public interface SleepListener {
        void onTabShouldSleep(TabItem tab);
        void onTabShouldWake(TabItem tab);
    }

    private SleepingTabManager() {
        sleepCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isEnabled && isRunning) {
                    checkAndSleepTabs();
                    handler.postDelayed(this, 10000); // Check every 10 seconds
                }
            }
        };
    }

    public static synchronized SleepingTabManager getInstance() {
        if (instance == null) {
            instance = new SleepingTabManager();
        }
        return instance;
    }

    public void start() {
        isRunning = true;
        handler.post(sleepCheckRunnable);
    }

    public void stop() {
        isRunning = false;
        handler.removeCallbacks(sleepCheckRunnable);
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (enabled && !isRunning) {
            start();
        } else if (!enabled && isRunning) {
            stop();
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setSleepTimeout(long milliseconds) {
        this.sleepTimeoutMs = milliseconds;
    }

    public long getSleepTimeout() {
        return sleepTimeoutMs;
    }

    public void setEfficiencyMode(boolean enabled) {
        this.isEfficiencyMode = enabled;
    }

    public boolean isEfficiencyMode() {
        return isEfficiencyMode;
    }

    public void addListener(SleepListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SleepListener listener) {
        listeners.remove(listener);
    }

    private void checkAndSleepTabs() {
        TabManager tabManager = TabManager.getInstance();
        List<TabItem> allTabs = tabManager.getAllTabs();
        int currentIndex = tabManager.getCurrentTabIndex();
        long now = System.currentTimeMillis();

        for (int i = 0; i < allTabs.size(); i++) {
            TabItem tab = allTabs.get(i);
            if (i == currentIndex) continue; // Skip current tab
            if (tab.isPinned()) continue; // Skip pinned tabs
            if (tab.isSleeping()) continue; // Already sleeping

            long idleTime = now - tab.getLastAccessTime();
            if (idleTime > sleepTimeoutMs) {
                tab.setSleeping(true);
                for (SleepListener listener : listeners) {
                    listener.onTabShouldSleep(tab);
                }
            }
        }
    }

    public void wakeTab(TabItem tab) {
        if (tab.isSleeping()) {
            tab.setSleeping(false);
            tab.touch();
            for (SleepListener listener : listeners) {
                listener.onTabShouldWake(tab);
            }
        }
    }
}