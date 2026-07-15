package com.edge.browser;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.edge.browser.performance.PerformanceManager;
import com.edge.browser.tab.TabItem;
import com.edge.browser.tab.TabManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PerformanceActivity extends AppCompatActivity {

    private RecyclerView statsRecycler;
    private StatsAdapter adapter;
    private PerformanceManager performanceManager;
    private TabManager tabManager;
    private TextView summaryText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("性能中心");
        }

        performanceManager = PerformanceManager.getInstance();
        tabManager = TabManager.getInstance();
        statsRecycler = findViewById(R.id.stats_recycler);
        summaryText = findViewById(R.id.summary_text);

        statsRecycler.setLayoutManager(new LinearLayoutManager(this));

        List<TabStatsItem> statsItems = buildStatsItems();
        adapter = new StatsAdapter(statsItems);
        statsRecycler.setAdapter(adapter);

        updateSummary();

        performanceManager.startMonitoring();
        performanceManager.addListener(new PerformanceManager.PerformanceListener() {
            @Override
            public void onStatsUpdated(Map<String, PerformanceManager.PerformanceStats> stats) {
                runOnUiThread(() -> {
                    adapter.updateItems(buildStatsItems());
                    updateSummary();
                });
            }

            @Override
            public void onHighLoadDetected(String tabId) {
                // Handle high load
            }
        });
    }

    private List<TabStatsItem> buildStatsItems() {
        List<TabStatsItem> items = new ArrayList<>();
        List<TabItem> tabs = tabManager.getAllTabs();
        Map<String, PerformanceManager.PerformanceStats> stats = performanceManager.getAllStats();

        for (TabItem tab : tabs) {
            PerformanceManager.PerformanceStats stat = stats.get(tab.getId());
            TabStatsItem item = new TabStatsItem();
            item.title = tab.getTitle();
            item.url = tab.getUrl();
            item.isSleeping = tab.isSleeping();
            if (stat != null) {
                item.memoryUsage = stat.memoryUsage;
                item.cpuUsage = stat.cpuUsage;
                item.isHighLoad = stat.isHighLoad;
            }
            items.add(item);
        }
        return items;
    }

    private void updateSummary() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        summaryText.setText(String.format(Locale.getDefault(),
                "已用内存: %.1f MB / %.1f MB | 标签数: %d",
                usedMemory / (1024.0 * 1024), totalMemory / (1024.0 * 1024),
                tabManager.getTabCount()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        performanceManager.stopMonitoring();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static class TabStatsItem {
        String title;
        String url;
        long memoryUsage;
        long cpuUsage;
        boolean isSleeping;
        boolean isHighLoad;
    }

    private class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.ViewHolder> {

        private List<TabStatsItem> items;

        StatsAdapter(List<TabStatsItem> items) {
            this.items = items;
        }

        void updateItems(List<TabStatsItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_performance, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TabStatsItem item = items.get(position);
            holder.titleText.setText(item.title);
            holder.memoryText.setText(String.format(Locale.getDefault(),
                    "内存: %.1f MB", item.memoryUsage / (1024.0 * 1024)));
            holder.cpuText.setText(String.format(Locale.getDefault(),
                    "CPU: %.1f%%", item.cpuUsage));
            holder.statusText.setText(item.isSleeping ? "休眠中" : "活跃");
            holder.statusText.setTextColor(item.isHighLoad ?
                    getColor(android.R.color.holo_red_dark) :
                    getColor(android.R.color.holo_green_dark));

            holder.btnSleep.setOnClickListener(v -> {
                TabItem tab = tabManager.getTabAt(position);
                if (tab != null) {
                    tab.setSleeping(!tab.isSleeping());
                    notifyItemChanged(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleText, memoryText, cpuText, statusText;
            Button btnSleep;

            ViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.perf_title);
                memoryText = itemView.findViewById(R.id.perf_memory);
                cpuText = itemView.findViewById(R.id.perf_cpu);
                statusText = itemView.findViewById(R.id.perf_status);
                btnSleep = itemView.findViewById(R.id.btn_sleep_tab);
            }
        }
    }
}