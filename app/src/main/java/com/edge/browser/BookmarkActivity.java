package com.edge.browser;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.edge.browser.bookmark.BookmarkManager;
import com.edge.browser.data.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookmarkActivity extends AppCompatActivity {

    private RecyclerView bookmarksRecycler;
    private BookmarkAdapter adapter;
    private List<DatabaseHelper.BookmarkEntry> bookmarks;
    private BookmarkManager bookmarkManager;

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

        bookmarkManager = BookmarkManager.getInstance(this);
        bookmarksRecycler = findViewById(R.id.bookmark_list);
        bookmarksRecycler.setLayoutManager(new LinearLayoutManager(this));

        bookmarks = loadBookmarks();
        adapter = new BookmarkAdapter(bookmarks);
        bookmarksRecycler.setAdapter(adapter);

        if (bookmarks.isEmpty()) {
            findViewById(R.id.empty_text).setVisibility(View.VISIBLE);
        }
    }

    private List<DatabaseHelper.BookmarkEntry> loadBookmarks() {
        List<DatabaseHelper.BookmarkEntry> items = bookmarkManager.getBookmarks();
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

    private class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

        private List<DatabaseHelper.BookmarkEntry> items;

        BookmarkAdapter(List<DatabaseHelper.BookmarkEntry> items) {
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
            DatabaseHelper.BookmarkEntry item = items.get(position);
            holder.titleText.setText(item.title);
            holder.urlText.setText(item.url);
            holder.dateText.setText(formatDate(item.createdAt));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(BookmarkActivity.this, MainActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse(item.url));
                startActivity(intent);
                finish();
            });

            holder.itemView.setOnLongClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(BookmarkActivity.this)
                        .setTitle(item.title)
                        .setMessage("删除此书签？")
                        .setPositiveButton("删除", (d, w) -> {
                            bookmarkManager.removeBookmark(item.id);
                            items.remove(position);
                            notifyItemRemoved(position);
                            if (items.isEmpty()) {
                                findViewById(R.id.empty_text).setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(BookmarkActivity.this, "已删除", Toast.LENGTH_SHORT).show();
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