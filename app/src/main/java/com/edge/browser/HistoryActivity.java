package com.edge.browser;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.edge.browser.data.DatabaseHelper;
import com.edge.browser.history.HistoryManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecycler;
    private HistoryAdapter adapter;
    private List<DatabaseHelper.HistoryEntry> historyItems;
    private HistoryManager historyManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("历史记录");
        }

        historyManager = HistoryManager.getInstance(this);
        historyRecycler = findViewById(R.id.history_list);
        historyRecycler.setLayoutManager(new LinearLayoutManager(this));

        historyItems = loadHistory();
        adapter = new HistoryAdapter(historyItems);
        historyRecycler.setAdapter(adapter);

        if (historyItems.isEmpty()) {
            findViewById(R.id.empty_text).setVisibility(View.VISIBLE);
        }
    }

    private List<DatabaseHelper.HistoryEntry> loadHistory() {
        List<DatabaseHelper.HistoryEntry> items = historyManager.getHistory();
        return items != null ? items : new ArrayList<>();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        private List<DatabaseHelper.HistoryEntry> items;

        HistoryAdapter(List<DatabaseHelper.HistoryEntry> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DatabaseHelper.HistoryEntry item = items.get(position);
            holder.titleText.setText(item.title != null ? item.title : item.url);
            holder.urlText.setText(item.url);
            holder.timeText.setText(formatDate(item.visitedAt));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse(item.url));
                startActivity(intent);
                finish();
            });

            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(HistoryActivity.this)
                        .setTitle(item.title != null ? item.title : item.url)
                        .setMessage("删除此历史记录？")
                        .setPositiveButton("删除", (d, w) -> {
                            historyManager.removeEntry(item.id);
                            items.remove(position);
                            notifyItemRemoved(position);
                            if (items.isEmpty()) {
                                findViewById(R.id.empty_text).setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(HistoryActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private String formatDate(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleText, urlText, timeText;

            ViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.history_title);
                urlText = itemView.findViewById(R.id.history_url);
                timeText = itemView.findViewById(R.id.history_time);
            }
        }
    }
}