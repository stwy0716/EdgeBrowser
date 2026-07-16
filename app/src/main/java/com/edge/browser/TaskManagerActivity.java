package com.edge.browser;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.edge.browser.performance.TaskManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskManagerActivity extends AppCompatActivity {

    private RecyclerView processesRecycler;
    private ProcessAdapter adapter;
    private TaskManager taskManager;
    private TextView totalMemoryText;
    private Button btnKillAll;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_manager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("任务管理器");
        }

        taskManager = TaskManager.getInstance();
        processesRecycler = findViewById(R.id.task_list);
        totalMemoryText = findViewById(R.id.process_memory);
        btnKillAll = findViewById(R.id.btn_kill_all);

        processesRecycler.setLayoutManager(new LinearLayoutManager(this));

        taskManager.updateAllStats();
        List<TaskManager.BrowserProcess> processes = taskManager.getAllProcesses();
        adapter = new ProcessAdapter(processes);
        processesRecycler.setAdapter(adapter);

        updateTotalMemory();

        btnKillAll.setOnClickListener(v -> {
            taskManager.closeAllNonEssential();
            adapter.updateProcesses(taskManager.getAllProcesses());
            updateTotalMemory();
        });
    }

    private void updateTotalMemory() {
        long totalMem = taskManager.getTotalMemoryUsage();
        totalMemoryText.setText(String.format(Locale.getDefault(),
                "总内存占用: %.1f MB", totalMem / (1024.0 * 1024)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ProcessAdapter extends RecyclerView.Adapter<ProcessAdapter.ViewHolder> {

        private List<TaskManager.BrowserProcess> processes;

        ProcessAdapter(List<TaskManager.BrowserProcess> processes) {
            this.processes = new ArrayList<>(processes);
        }

        void updateProcesses(List<TaskManager.BrowserProcess> processes) {
            this.processes = new ArrayList<>(processes);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_process, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TaskManager.BrowserProcess process = processes.get(position);
            holder.nameText.setText(process.getName());
            holder.typeText.setText(getTypeName(process.getType()));
            holder.memoryText.setText(String.format(Locale.getDefault(),
                    "%.1f MB", process.getMemoryUsage() / (1024.0 * 1024)));
            holder.cpuText.setText(String.format(Locale.getDefault(),
                    "%.1f%%", process.getCpuUsage()));

            holder.btnKill.setOnClickListener(v -> {
                taskManager.killProcess(process.getId());
                processes.remove(position);
                notifyItemRemoved(position);
                updateTotalMemory();
            });
        }

        @Override
        public int getItemCount() {
            return processes.size();
        }

        private String getTypeName(TaskManager.ProcessType type) {
            switch (type) {
                case TAB: return "标签页";
                case EXTENSION: return "扩展";
                case BACKGROUND: return "后台";
                case PLUGIN: return "插件";
                case POPUP: return "弹窗";
                case GPU: return "GPU";
                case NETWORK: return "网络";
                default: return "其他";
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, typeText, memoryText, cpuText;
            Button btnKill;

            ViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.process_name);
                typeText = itemView.findViewById(R.id.process_type);
                memoryText = itemView.findViewById(R.id.process_memory);
                cpuText = itemView.findViewById(R.id.process_cpu);
                btnKill = itemView.findViewById(R.id.btn_kill_process);
            }
        }
    }
}