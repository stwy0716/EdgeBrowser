package com.edge.browser;

import android.content.Intent;
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
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecycler;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyItems;

    public static class HistoryItem {
        String title;
        String url;
        long timestamp;

        public HistoryItem(String title, String url, long timestamp) {
            this.title = title;
            this.url = url;
            this.timestamp = timestamp;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("历史记录");
        }

        historyRecycler = findViewById(R.id.history_recycler);
        historyRecycler.setLayoutManager(new LinearLayoutManager(this));

        historyItems = loadHistory();
        adapter = new HistoryAdapter(historyItems);
        historyRecycler.setAdapter(adapter);
    }

    private List<HistoryItem> loadHistory() {
        List<HistoryItem> items = new ArrayList<>();
        // Load from database (simplified)
        return items;
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

        private List<HistoryItem> items;

        HistoryAdapter(List<HistoryItem> items) {
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
            HistoryItem item = items.get(position);
            holder.titleText.setText(item.title);
            holder.urlText.setText(item.url);
            holder.timeText.setText(formatDate(item.timestamp));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse(item.url));
                startActivity(intent);
                finish();
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