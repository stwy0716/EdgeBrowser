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
import com.edge.browser.reading.ReadingListManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CollectionActivity extends AppCompatActivity {

    private RecyclerView collectionsRecycler;
    private CollectionAdapter adapter;
    private List<DatabaseHelper.ReadingItem> collections;
    private ReadingListManager readingListManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("集锦");
        }

        readingListManager = ReadingListManager.getInstance(this);
        collectionsRecycler = findViewById(R.id.collection_list);
        collectionsRecycler.setLayoutManager(new LinearLayoutManager(this));

        // Handle incoming URL from "add to collections"
        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        if (url != null && title != null) {
            readingListManager.addItem(title, url, null);
            Toast.makeText(this, "已添加到集锦", Toast.LENGTH_SHORT).show();
        }

        collections = loadCollections();
        adapter = new CollectionAdapter(collections);
        collectionsRecycler.setAdapter(adapter);

        if (collections.isEmpty()) {
            findViewById(R.id.empty_text).setVisibility(View.VISIBLE);
        }
    }

    private List<DatabaseHelper.ReadingItem> loadCollections() {
        List<DatabaseHelper.ReadingItem> items = readingListManager.getItems();
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

    private class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {

        private List<DatabaseHelper.ReadingItem> items;

        CollectionAdapter(List<DatabaseHelper.ReadingItem> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_collection, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DatabaseHelper.ReadingItem item = items.get(position);
            holder.titleText.setText(item.title);
            holder.noteText.setText(item.url);
            holder.countText.setText(item.isRead ? "已读" : "未读");

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(CollectionActivity.this, MainActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse(item.url));
                startActivity(intent);
                finish();
            });

            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(CollectionActivity.this)
                        .setTitle(item.title)
                        .setMessage("URL: " + item.url)
                        .setPositiveButton(item.isRead ? "标记未读" : "标记已读", (d, w) -> {
                            readingListManager.markAsRead(item.id);
                            item.isRead = !item.isRead;
                            notifyItemChanged(position);
                        })
                        .setNegativeButton("删除", (d, w) -> {
                            readingListManager.removeItem(item.id);
                            items.remove(position);
                            notifyItemRemoved(position);
                            if (items.isEmpty()) {
                                findViewById(R.id.empty_text).setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(CollectionActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNeutralButton("取消", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleText, noteText, countText;

            ViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.collection_title);
                noteText = itemView.findViewById(R.id.collection_note);
                countText = itemView.findViewById(R.id.collection_count);
            }
        }
    }
}