package com.edge.browser;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.edge.browser.adblock.AdBlocker;
import com.edge.browser.data.DatabaseHelper;
import com.edge.browser.engine.EnginePreferences;
import com.edge.browser.extensions.ExtensionInfo;
import com.edge.browser.extensions.ExtensionManager;
import com.edge.browser.nightmode.NightModeManager;
import com.edge.browser.password.PasswordManager;
import com.edge.browser.privacy.PrivacyManager;
import com.edge.browser.quicklinks.QuickLinkManager;
import com.edge.browser.reading.ReadingListManager;
import com.edge.browser.search.SearchEngineManager;
import com.edge.browser.startup.StartupConfigManager;
import com.edge.browser.stats.StatsManager;
import com.edge.browser.theme.ThemeManager;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private SearchEngineManager searchEngineManager;
    private EnginePreferences enginePreferences;
    private ThemeManager themeManager;
    private PrivacyManager privacyManager;
    private AdBlocker adBlocker;
    private StartupConfigManager startupConfigManager;
    private NightModeManager nightModeManager;
    private StatsManager statsManager;
    private PasswordManager passwordManager;
    private ReadingListManager readingListManager;
    private ExtensionManager extensionManager;
    private QuickLinkManager quickLinkManager;

    private RadioGroup searchEngineGroup;
    private RadioGroup engineGroup;
    private RadioGroup themeGroup;
    private RadioGroup startupGroup;
    private EditText homepageUrl;
    private SwitchCompat adBlockSwitch;
    private SwitchCompat nightModeSwitch;
    private TextView btnClearData;
    private TextView btnPrivacyLevel;
    private TextView btnStats;
    private TextView btnPasswords;
    private TextView btnReadingList;
    private TextView btnExtensions;
    private TextView btnQuickLinks;

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
        startupConfigManager = StartupConfigManager.getInstance(this);
        nightModeManager = NightModeManager.getInstance();
        statsManager = StatsManager.getInstance(this);
        passwordManager = PasswordManager.getInstance(this);
        readingListManager = ReadingListManager.getInstance(this);
        extensionManager = ExtensionManager.getInstance(this);
        quickLinkManager = QuickLinkManager.getInstance(this);
    }

    private void initViews() {
        searchEngineGroup = findViewById(R.id.search_engine_group);
        engineGroup = findViewById(R.id.engine_group);
        themeGroup = findViewById(R.id.theme_group);
        startupGroup = findViewById(R.id.startup_group);
        homepageUrl = findViewById(R.id.homepage_url);
        adBlockSwitch = findViewById(R.id.ad_block_switch);
        nightModeSwitch = findViewById(R.id.night_mode_switch);
        btnClearData = findViewById(R.id.btn_clear_data);
        btnPrivacyLevel = findViewById(R.id.btn_privacy_level);
        btnStats = findViewById(R.id.btn_stats);
        btnPasswords = findViewById(R.id.btn_passwords);
        btnReadingList = findViewById(R.id.btn_reading_list);
        btnExtensions = findViewById(R.id.btn_extensions);
        btnQuickLinks = findViewById(R.id.btn_quick_links);

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

        // 填充启动页选项
        String[] startupLabels = {"新标签页", "继续上次", "指定主页"};
        String[] startupValues = {"newtab", "restore", "homepage"};
        for (int i = 0; i < startupLabels.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(startupLabels[i]);
            rb.setTag(startupValues[i]);
            rb.setPadding(0, 16, 0, 16);
            rb.setTextSize(15);
            startupGroup.addView(rb);
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

        // 启动页
        String currentStartup = startupConfigManager.getStartupMode();
        for (int i = 0; i < startupGroup.getChildCount(); i++) {
            RadioButton rb = (RadioButton) startupGroup.getChildAt(i);
            if (currentStartup.equals(rb.getTag())) {
                rb.setChecked(true);
                break;
            }
        }
        homepageUrl.setText(startupConfigManager.getHomepage());
        homepageUrl.setVisibility("homepage".equals(currentStartup) ? View.VISIBLE : View.GONE);

        // 夜间模式
        nightModeManager.loadState(DatabaseHelper.getInstance(this));
        nightModeSwitch.setChecked(nightModeManager.isEnabled());
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

        // 启动页
        startupGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            if (rb != null && rb.getTag() != null) {
                String mode = rb.getTag().toString();
                startupConfigManager.setStartupMode(mode);
                homepageUrl.setVisibility("homepage".equals(mode) ? View.VISIBLE : View.GONE);
                Toast.makeText(this, "启动模式已切换", Toast.LENGTH_SHORT).show();
            }
        });

        // 夜间模式
        nightModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            nightModeManager.setEnabled(isChecked);
            nightModeManager.saveState(DatabaseHelper.getInstance(this));
            Toast.makeText(this, isChecked ? "夜间模式已开启" : "夜间模式已关闭", Toast.LENGTH_SHORT).show();
        });

        // 查看统计
        btnStats.setOnClickListener(v -> {
            List<DatabaseHelper.StatEntry> stats = statsManager.getStats();
            StringBuilder sb = new StringBuilder();
            if (stats.isEmpty()) {
                sb.append("暂无统计数据");
            } else {
                for (DatabaseHelper.StatEntry entry : stats) {
                    sb.append(entry.date).append("  ")
                            .append("页面：").append(entry.pagesVisited).append("  ")
                            .append("数据：").append(formatBytes(entry.dataReceived)).append("  ")
                            .append("时长：").append(formatTime(entry.timeSpent))
                            .append("\n");
                }
            }
            new AlertDialog.Builder(this)
                    .setTitle("浏览统计")
                    .setMessage(sb.toString())
                    .setPositiveButton("确定", null)
                    .show();
        });

        // 管理密码
        btnPasswords.setOnClickListener(v -> {
            List<DatabaseHelper.PasswordEntry> passwords = passwordManager.getAllPasswords();
            StringBuilder sb = new StringBuilder();
            if (passwords.isEmpty()) {
                sb.append("暂无保存的密码");
            } else {
                for (DatabaseHelper.PasswordEntry entry : passwords) {
                    sb.append("域名：").append(entry.domain).append("\n")
                            .append("用户名：").append(entry.username).append("\n")
                            .append("密码：").append(entry.password).append("\n\n");
                }
            }
            new AlertDialog.Builder(this)
                    .setTitle("已保存密码")
                    .setMessage(sb.toString())
                    .setPositiveButton("确定", null)
                    .show();
        });

        // 管理阅读列表
        btnReadingList.setOnClickListener(v -> {
            List<DatabaseHelper.ReadingItem> items = readingListManager.getItems();
            if (items.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("阅读列表")
                        .setMessage("暂无阅读项目")
                        .setPositiveButton("确定", null)
                        .show();
            } else {
                String[] itemNames = new String[items.size()];
                for (int i = 0; i < items.size(); i++) {
                    DatabaseHelper.ReadingItem item = items.get(i);
                    itemNames[i] = (item.isRead ? "[已读] " : "") + item.title;
                }
                new AlertDialog.Builder(this)
                        .setTitle("阅读列表")
                        .setItems(itemNames, (d, which) -> {
                            DatabaseHelper.ReadingItem item = items.get(which);
                            new AlertDialog.Builder(this)
                                    .setTitle(item.title)
                                    .setMessage("URL: " + item.url + "\n已读: " + (item.isRead ? "是" : "否"))
                                    .setPositiveButton("标记已读", (di, w) -> {
                                        readingListManager.markAsRead(item.id);
                                        Toast.makeText(this, "已标记为已读", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("删除", (di, w) -> {
                                        readingListManager.removeItem(item.id);
                                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNeutralButton("取消", null)
                                    .show();
                        })
                        .setNegativeButton("关闭", null)
                        .show();
            }
        });

        // 管理扩展
        btnExtensions.setOnClickListener(v -> {
            List<ExtensionInfo> extensions = extensionManager.getExtensions();
            if (extensions.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("扩展管理")
                        .setMessage("暂无已安装扩展")
                        .setPositiveButton("确定", null)
                        .show();
            } else {
                String[] extNames = new String[extensions.size()];
                for (int i = 0; i < extensions.size(); i++) {
                    ExtensionInfo ext = extensions.get(i);
                    extNames[i] = (ext.enabled ? "[启用] " : "[禁用] ") + ext.name;
                }
                new AlertDialog.Builder(this)
                        .setTitle("扩展管理")
                        .setItems(extNames, (d, which) -> {
                            ExtensionInfo ext = extensions.get(which);
                            new AlertDialog.Builder(this)
                                    .setTitle(ext.name)
                                    .setMessage("版本: " + ext.version + "\n" + ext.description + "\n状态: " + (ext.enabled ? "已启用" : "已禁用"))
                                    .setPositiveButton(ext.enabled ? "禁用" : "启用", (di, w) -> {
                                        extensionManager.setEnabled(ext.id, !ext.enabled);
                                        Toast.makeText(this, ext.enabled ? "已禁用" : "已启用", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("卸载", (di, w) -> {
                                        extensionManager.uninstallExtension(ext.id);
                                        Toast.makeText(this, "已卸载", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNeutralButton("取消", null)
                                    .show();
                        })
                        .setNegativeButton("关闭", null)
                        .show();
            }
        });

        // 管理快捷链接
        btnQuickLinks.setOnClickListener(v -> {
            List<DatabaseHelper.QuickLinkEntry> links = quickLinkManager.getLinks();
            if (links.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("快捷链接")
                        .setMessage("暂无快捷链接")
                        .setPositiveButton("确定", null)
                        .show();
            } else {
                String[] linkNames = new String[links.size()];
                for (int i = 0; i < links.size(); i++) {
                    linkNames[i] = links.get(i).title + " (" + links.get(i).url + ")";
                }
                new AlertDialog.Builder(this)
                        .setTitle("管理快捷链接")
                        .setItems(linkNames, (d, which) -> {
                            DatabaseHelper.QuickLinkEntry link = links.get(which);
                            new AlertDialog.Builder(this)
                                    .setTitle(link.title)
                                    .setMessage("URL: " + link.url)
                                    .setPositiveButton("删除", (di, w) -> {
                                        quickLinkManager.removeLink(link.id);
                                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        })
                        .setNegativeButton("关闭", null)
                        .show();
            }
        });
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatTime(long ms) {
        if (ms < 60000) return (ms / 1000) + "秒";
        if (ms < 3600000) return (ms / 60000) + "分钟";
        return String.format("%.1f小时", ms / 3600000.0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}