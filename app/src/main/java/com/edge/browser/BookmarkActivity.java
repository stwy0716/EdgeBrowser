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

import com.edge.browser.tab.TabItem;
import com.edge.browser.tab.TabManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookmarkActivity extends AppCompatActivity {

    private RecyclerView bookmarksRecycler;
    private BookmarkAdapter adapter;
    private List<BookmarkItem> bookmarks;

    public static class BookmarkItem {
        String title;
        String url;
        long timestamp;
        String folder;

        public BookmarkItem(String title, String url, long timestamp, String folder) {
            this.title = title;
            this.url = url;
            this.timestamp = timestamp;
            this.folder = folder;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("收藏夹");
        }

        bookmarksRecycler = findViewById(R.id.bookmarks_recycler);
        bookmarksRecycler.setLayoutManager(new LinearLayoutManager(this));

        bookmarks = loadBookmarks();
        adapter = new BookmarkAdapter(bookmarks);
        bookmarksRecycler.setAdapter(adapter);
    }

    private List<BookmarkItem> loadBookmarks() {
        // Load from SharedPreferences or database
        List<BookmarkItem> items = new ArrayList<>();
        // Mock data
        items.add(new BookmarkItem("Google", "https://www.google.com", System.currentTimeMillis(), "常用"));
        items.add(new BookmarkItem("Bing", "https://www.bing.com", System.currentTimeMillis(), "常用"));
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

    private class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

        private List<BookmarkItem> items;

        BookmarkAdapter(List<BookmarkItem> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bookmark, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BookmarkItem item = items.get(position);
            holder.titleText.setText(item.title);
            holder.urlText.setText(item.url);
            holder.dateText.setText(formatDate(item.timestamp));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(BookmarkActivity.this, MainActivity.class);
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
            TextView titleText, urlText, dateText;

            ViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.bookmark_title);
                urlText = itemView.findViewById(R.id.bookmark_url);
                dateText = itemView.findViewById(R.id.bookmark_date);
            }
        }
    }
}