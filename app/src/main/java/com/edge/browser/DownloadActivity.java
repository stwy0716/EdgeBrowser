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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;

import com.edge.browser.download.DownloadManager;
import com.edge.browser.download.DownloadService;

import java.util.ArrayList;
import java.util.List;

public class DownloadActivity extends AppCompatActivity {

    private RecyclerView downloadsRecycler;
    private DownloadAdapter adapter;
    private DownloadManager downloadManager;
    private TextView emptyText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("下载管理");
        }

        downloadsRecycler = findViewById(R.id.downloads_recycler);
        emptyText = findViewById(R.id.empty_text);
        downloadsRecycler.setLayoutManager(new LinearLayoutManager(this));

        downloadManager = DownloadManager.getInstance(this);
        List<DownloadService.DownloadTask> downloads = downloadManager.getAllDownloads();
        adapter = new DownloadAdapter(downloads);
        downloadsRecycler.setAdapter(adapter);

        updateEmptyState();

        downloadManager.addListener(new DownloadManager.DownloadListener() {
            @Override
            public void onDownloadStarted(DownloadService.DownloadTask task) {
                adapter.addTask(task);
                updateEmptyState();
            }

            @Override
            public void onDownloadProgress(DownloadService.DownloadTask task) {
                adapter.updateTask(task);
            }

            @Override
            public void onDownloadCompleted(DownloadService.DownloadTask task) {
                adapter.updateTask(task);
            }

            @Override
            public void onDownloadFailed(DownloadService.DownloadTask task) {
                adapter.updateTask(task);
            }

            @Override
            public void onDownloadCancelled(DownloadService.DownloadTask task) {
                adapter.removeTask(task);
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        emptyText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {

        private List<DownloadService.DownloadTask> tasks;

        DownloadAdapter(List<DownloadService.DownloadTask> tasks) {
            this.tasks = new ArrayList<>(tasks);
        }

        void addTask(DownloadService.DownloadTask task) {
            tasks.add(0, task);
            notifyItemInserted(0);
        }

        void updateTask(DownloadService.DownloadTask task) {
            int index = -1;
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).getId().equals(task.getId())) {
                    index = i;
                    tasks.set(i, task);
                    break;
                }
            }
            if (index >= 0) notifyItemChanged(index);
        }

        void removeTask(DownloadService.DownloadTask task) {
            int index = -1;
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).getId().equals(task.getId())) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                tasks.remove(index);
                notifyItemRemoved(index);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_download, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DownloadService.DownloadTask task = tasks.get(position);
            holder.fileName.setText(task.getFileName());
            holder.fileSize.setText(formatSize(task.getTotalSize()));
            holder.progressBar.setProgress(task.getProgress());

            if (task.isCompleted()) {
                holder.statusText.setText("已完成");
                holder.progressBar.setVisibility(View.GONE);
            } else if (task.isFailed()) {
                holder.statusText.setText("失败: " + task.getErrorMessage());
                holder.progressBar.setVisibility(View.GONE);
            } else if (task.isPaused()) {
                holder.statusText.setText("已暂停");
            } else if (task.isCancelled()) {
                holder.statusText.setText("已取消");
                holder.progressBar.setVisibility(View.GONE);
            } else {
                holder.statusText.setText(task.getProgress() + "%");
            }

            holder.btnCancel.setOnClickListener(v -> {
                downloadManager.cancelDownload(task.getId());
            });
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }

        private String formatSize(long bytes) {
            if (bytes <= 0) return "未知大小";
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView fileName, fileSize, statusText;
            ProgressBar progressBar;
            Button btnCancel;

            ViewHolder(View itemView) {
                super(itemView);
                fileName = itemView.findViewById(R.id.download_name);
                fileSize = itemView.findViewById(R.id.download_size);
                statusText = itemView.findViewById(R.id.download_status);
                progressBar = itemView.findViewById(R.id.download_progress);
                btnCancel = itemView.findViewById(R.id.btn_cancel);
            }
        }
    }
}