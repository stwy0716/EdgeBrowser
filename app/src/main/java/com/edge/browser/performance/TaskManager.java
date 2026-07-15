package com.edge.browser.performance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskManager {

    private static TaskManager instance;
    private final Map<String, BrowserProcess> processes;

    public enum ProcessType {
        TAB, EXTENSION, BACKGROUND, PLUGIN, POPUP, GPU, NETWORK
    }

    public static class BrowserProcess {
        private String id;
        private String name;
        private ProcessType type;
        private long memoryUsage;
        private double cpuUsage;
        private boolean isRunning;

        public BrowserProcess(String id, String name, ProcessType type) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.isRunning = true;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public ProcessType getType() { return type; }
        public long getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }
        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
        public boolean isRunning() { return isRunning; }
        public void setRunning(boolean running) { isRunning = running; }
    }

    private TaskManager() {
        processes = new HashMap<>();
    }

    public static synchronized TaskManager getInstance() {
        if (instance == null) {
            instance = new TaskManager();
        }
        return instance;
    }

    public void registerProcess(String id, String name, ProcessType type) {
        BrowserProcess process = new BrowserProcess(id, name, type);
        processes.put(id, process);
        updateProcessStats(process);
    }

    public void unregisterProcess(String id) {
        processes.remove(id);
    }

    public void killProcess(String id) {
        BrowserProcess process = processes.get(id);
        if (process != null) {
            process.setRunning(false);
            processes.remove(id);
        }
    }

    public List<BrowserProcess> getAllProcesses() {
        return new ArrayList<>(processes.values());
    }

    public List<BrowserProcess> getProcessesByType(ProcessType type) {
        List<BrowserProcess> result = new ArrayList<>();
        for (BrowserProcess process : processes.values()) {
            if (process.getType() == type) {
                result.add(process);
            }
        }
        return result;
    }

    public BrowserProcess getHighestMemoryProcess() {
        BrowserProcess highest = null;
        long maxMem = 0;
        for (BrowserProcess process : processes.values()) {
            if (process.getMemoryUsage() > maxMem) {
                maxMem = process.getMemoryUsage();
                highest = process;
            }
        }
        return highest;
    }

    public void updateAllStats() {
        for (BrowserProcess process : processes.values()) {
            updateProcessStats(process);
        }
    }

    private void updateProcessStats(BrowserProcess process) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        // Estimate per-process memory (simplified)
        process.setMemoryUsage(usedMemory / Math.max(processes.size(), 1));
        process.setCpuUsage(Math.random() * 20); // Mock
    }

    public long getTotalMemoryUsage() {
        long total = 0;
        for (BrowserProcess process : processes.values()) {
            total += process.getMemoryUsage();
        }
        return total;
    }

    public void closeAllNonEssential() {
        List<String> toRemove = new ArrayList<>();
        for (BrowserProcess process : processes.values()) {
            if (process.getType() != ProcessType.TAB) {
                toRemove.add(process.getId());
            }
        }
        for (String id : toRemove) {
            processes.remove(id);
        }
    }
}