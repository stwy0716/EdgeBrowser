package com.edge.browser.sync;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import com.edge.browser.R;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SyncActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "sync_prefs";
    private static final String KEY_LAST_SYNC = "last_sync_time";

    private SyncManager syncManager;
    private android.widget.TextView statusText;
    private final ActivityResultLauncher<Intent> importFileLauncher;

    public SyncActivity() {
        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            importFromUri(uri);
                        }
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        syncManager = SyncManager.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("数据同步");
        }

        statusText = findViewById(R.id.sync_status_text);

        // Export all data
        findViewById(R.id.btn_export_all).setOnClickListener(v -> {
            syncManager.shareExportFile(this);
            updateLastSyncTime();
        });

        // Import data
        findViewById(R.id.btn_import_data).setOnClickListener(v -> {
            syncManager.importFromFile(this);
        });

        // Export bookmarks only
        findViewById(R.id.btn_export_bookmarks).setOnClickListener(v -> {
            String path = syncManager.exportBookmarks(this);
            if (path != null) {
                Toast.makeText(this, "书签已导出: " + path, Toast.LENGTH_LONG).show();
                updateLastSyncTime();
            } else {
                Toast.makeText(this, "导出书签失败", Toast.LENGTH_SHORT).show();
            }
        });

        // Export passwords only
        findViewById(R.id.btn_export_passwords).setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("警告")
                    .setMessage("密码将以 Base64 编码格式导出，请妥善保管导出的文件。\n\n确定要继续吗？")
                    .setPositiveButton("确定", (d, w) -> {
                        String path = syncManager.exportPasswords(this);
                        if (path != null) {
                            Toast.makeText(this, "密码已导出: " + path, Toast.LENGTH_LONG).show();
                            updateLastSyncTime();
                        } else {
                            Toast.makeText(this, "导出密码失败", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        updateStatusText();
    }

    private void importFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, "无法读取文件", Toast.LENGTH_SHORT).show();
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            inputStream.close();

            // Save to temp file and import
            java.io.File tempFile = new java.io.File(getCacheDir(), "temp_import.json");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            fos.write(sb.toString().getBytes("UTF-8"));
            fos.close();

            boolean success = syncManager.importAllData(this, tempFile.getAbsolutePath());
            tempFile.delete();

            if (success) {
                Toast.makeText(this, "数据导入成功", Toast.LENGTH_SHORT).show();
                updateLastSyncTime();
            } else {
                Toast.makeText(this, "数据导入失败，请检查文件格式", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLastSyncTime() {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_LAST_SYNC, time)
                .apply();
        updateStatusText();
    }

    private void updateStatusText() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastSync = prefs.getString(KEY_LAST_SYNC, null);
        if (lastSync != null) {
            statusText.setText("上次同步时间: " + lastSync);
        } else {
            statusText.setText("尚未进行过数据同步");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}