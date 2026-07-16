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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CollectionActivity extends AppCompatActivity {

    private RecyclerView collectionsRecycler;
    private CollectionAdapter adapter;
    private List<CollectionItem> collections;

    public static class CollectionItem {
        String id;
        String title;
        String note;
        String url;
        int itemCount;
        long createdAt;

        public CollectionItem(String id, String title, String note, int itemCount) {
            this.id = id;
            this.title = title;
            this.note = note;
            this.itemCount = itemCount;
            this.createdAt = System.currentTimeMillis();
        }
    }

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

        collectionsRecycler = findViewById(R.id.collection_list);
        collectionsRecycler.setLayoutManager(new LinearLayoutManager(this));

        // Handle incoming URL from "add to collections"
        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");

        collections = loadCollections();
        if (url != null && title != null) {
            collections.add(0, new CollectionItem(
                    String.valueOf(System.currentTimeMillis()), title, url, 1));
        }

        adapter = new CollectionAdapter(collections);
        collectionsRecycler.setAdapter(adapter);
    }

    private List<CollectionItem> loadCollections() {
        List<CollectionItem> items = new ArrayList<>();
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

    private class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {

        private List<CollectionItem> items;

        CollectionAdapter(List<CollectionItem> items) {
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
            CollectionItem item = items.get(position);
            holder.titleText.setText(item.title);
            holder.noteText.setText(item.note);
            holder.countText.setText(item.itemCount + " 项");
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