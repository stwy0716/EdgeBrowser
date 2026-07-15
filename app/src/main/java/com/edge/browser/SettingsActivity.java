package com.edge.browser;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.edge.browser.adblock.AdBlocker;
import com.edge.browser.engine.EnginePreferences;
import com.edge.browser.privacy.PrivacyManager;
import com.edge.browser.search.SearchEngineManager;
import com.edge.browser.theme.ThemeManager;

public class SettingsActivity extends AppCompatActivity {

    private SearchEngineManager searchEngineManager;
    private EnginePreferences enginePreferences;
    private ThemeManager themeManager;
    private PrivacyManager privacyManager;
    private AdBlocker adBlocker;

    private RadioGroup searchEngineGroup;
    private RadioGroup engineGroup;
    private RadioGroup themeGroup;
    private SwitchCompat adBlockSwitch;
    private TextView btnClearData;
    private TextView btnPrivacyLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设置");
        }

        initManagers();
        initViews();
        loadSettings();
        setupListeners();
    }

    private void initManagers() {
        searchEngineManager = SearchEngineManager.getInstance(this);
        enginePreferences = EnginePreferences.getInstance();
        themeManager = ThemeManager.getInstance();
        privacyManager = PrivacyManager.getInstance(this);
        adBlocker = AdBlocker.getInstance();
    }

    private void initViews() {
        searchEngineGroup = findViewById(R.id.search_engine_group);
        engineGroup = findViewById(R.id.engine_group);
        themeGroup = findViewById(R.id.theme_group);
        adBlockSwitch = findViewById(R.id.ad_block_switch);
        btnClearData = findViewById(R.id.btn_clear_data);
        btnPrivacyLevel = findViewById(R.id.btn_privacy_level);

        // 填充搜索引擎选项
        for (String key : SearchEngineManager.ENGINES.keySet()) {
            SearchEngineManager.SearchEngine engine = SearchEngineManager.ENGINES.get(key);
            if (engine == null) continue;
            RadioButton rb = new RadioButton(this);
            rb.setText(engine.name);
            rb.setTag(key);
            rb.setPadding(0, 16, 0, 16);
            rb.setTextSize(15);
            searchEngineGroup.addView(rb);
        }

        // 填充内核选项
        for (EnginePreferences.EngineType type : EnginePreferences.EngineType.values()) {
            RadioButton rb = new RadioButton(this);
            rb.setText(type.getDisplayName());
            rb.setTag(type.name());
            rb.setPadding(0, 16, 0, 16);
            rb.setTextSize(15);
            engineGroup.addView(rb);
        }

        // 填充主题选项
        String[] themes = {"浅色", "深色", "跟随系统"};
        String[] themeValues = {"light", "dark", "system"};
        for (int i = 0; i < themes.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(themes[i]);
            rb.setTag(themeValues[i]);
            rb.setPadding(0, 16, 0, 16);
            rb.setTextSize(15);
            themeGroup.addView(rb);
        }
    }

    private void loadSettings() {
        // 搜索引擎
        String currentEngine = searchEngineManager.getCurrentEngine();
        for (int i = 0; i < searchEngineGroup.getChildCount(); i++) {
            RadioButton rb = (RadioButton) searchEngineGroup.getChildAt(i);
            if (currentEngine.equals(rb.getTag())) {
                rb.setChecked(true);
                break;
            }
        }

        // 内核
        String currentEngineType = enginePreferences.getCurrentEngine().name();
        for (int i = 0; i < engineGroup.getChildCount(); i++) {
            RadioButton rb = (RadioButton) engineGroup.getChildAt(i);
            if (currentEngineType.equals(rb.getTag())) {
                rb.setChecked(true);
                break;
            }
        }

        // 主题
        ThemeManager.ThemeMode currentTheme = themeManager.getCurrentTheme();
        for (int i = 0; i < themeGroup.getChildCount(); i++) {
            RadioButton rb = (RadioButton) themeGroup.getChildAt(i);
            if (currentTheme.name().equalsIgnoreCase((String) rb.getTag())) {
                rb.setChecked(true);
                break;
            }
        }

        // 广告拦截
        adBlockSwitch.setChecked(adBlocker.isEnabled());
    }

    private void setupListeners() {
        // 搜索引擎
        searchEngineGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            if (rb != null && rb.getTag() != null) {
                searchEngineManager.setCurrentEngine(rb.getTag().toString());
                Toast.makeText(this, "搜索引擎已切换", Toast.LENGTH_SHORT).show();
            }
        });

        // 内核
        engineGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            if (rb != null && rb.getTag() != null) {
                EnginePreferences.EngineType type = EnginePreferences.EngineType.valueOf(rb.getTag().toString());
                enginePreferences.setCurrentEngine(type);
                enginePreferences.saveState(com.edge.browser.data.DatabaseHelper.getInstance(this));
                Toast.makeText(this, "内核已切换，重启应用生效", Toast.LENGTH_LONG).show();
            }
        });

        // 主题
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            if (rb != null && rb.getTag() != null) {
                themeManager.setCurrentTheme(ThemeManager.ThemeMode.valueOf(((String) rb.getTag()).toUpperCase()));
                android.app.Activity activity = this;
                if (activity != null) {
                    activity.recreate();
                }
            }
        });

        // 广告拦截
        adBlockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adBlocker.setEnabled(isChecked);
            adBlocker.saveState(com.edge.browser.data.DatabaseHelper.getInstance(this));
            Toast.makeText(this, isChecked ? "广告拦截已开启" : "广告拦截已关闭", Toast.LENGTH_SHORT).show();
        });

        // 清除数据
        btnClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("清除浏览数据")
                    .setMultiChoiceItems(new String[]{"历史记录", "书签", "Cookie"},
                            new boolean[]{true, false, true}, (dialog, which, isChecked) -> {})
                    .setPositiveButton("清除", (d, which) -> {
                        privacyManager.clearBrowsingData(true, true, true);
                        Toast.makeText(this, "数据已清除", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 隐私级别
        btnPrivacyLevel.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("跟踪保护级别")
                    .setItems(new String[]{"基础", "平衡（推荐）", "严格"}, (d, which) -> {
                        PrivacyManager.TrackingLevel[] levels = {
                                PrivacyManager.TrackingLevel.BASIC,
                                PrivacyManager.TrackingLevel.BALANCED,
                                PrivacyManager.TrackingLevel.STRICT
                        };
                        privacyManager.applyTrackingProtection(levels[which]);
                        String[] labels = {"基础", "平衡", "严格"};
                        btnPrivacyLevel.setText("跟踪保护：" + labels[which]);
                        Toast.makeText(this, "跟踪保护级别已更新", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}