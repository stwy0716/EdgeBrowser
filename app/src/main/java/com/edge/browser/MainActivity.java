package com.edge.browser;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.edge.browser.download.DownloadManager;
import com.edge.browser.media.MediaController;
import com.edge.browser.media.PictureInPictureManager;
import com.edge.browser.performance.PerformanceManager;
import com.edge.browser.performance.TaskManager;
import com.edge.browser.privacy.PrivacyManager;
import com.edge.browser.reader.ReadAloudManager;
import com.edge.browser.reader.ReaderModeManager;
import com.edge.browser.screenshot.ScreenshotManager;
import com.edge.browser.sidebar.SidebarManager;
import com.edge.browser.tab.SleepingTabManager;
import com.edge.browser.tab.TabGroupManager;
import com.edge.browser.tab.TabItem;
import com.edge.browser.tab.TabManager;
import com.edge.browser.theme.ThemeManager;
import com.edge.browser.tools.SideToolsManager;
import com.edge.browser.ui.TabListAdapter;
import com.edge.browser.ui.TabPagerAdapter;
import com.edge.browser.webview.ChromiumWebViewFactory;
import com.edge.browser.webview.EdgeWebView;
import com.edge.browser.webview.GeckoRuntimeManager;
import com.edge.browser.webview.GeckoWebView;
import com.edge.browser.webview.IBrowserView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements TabPagerAdapter.WebViewProvider {

    private static final String TAG = "MainActivity";
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Views
    private Toolbar toolbar;
    private EditText urlBar;
    private ImageButton btnBack, btnForward, btnRefresh, btnHome, btnTabs, btnMenu;
    private ProgressBar progressBar;
    private ViewPager2 viewPager;
    private DrawerLayout drawerLayout;
    private RecyclerView tabsRecyclerView;
    private View bottomSheet;

    // Managers
    private TabManager tabManager;
    private TabGroupManager tabGroupManager;
    private SleepingTabManager sleepingTabManager;
    private DownloadManager downloadManager;
    private PrivacyManager privacyManager;
    private PerformanceManager performanceManager;
    private TaskManager taskManager;
    private MediaController mediaController;
    private PictureInPictureManager pipManager;
    private ReaderModeManager readerModeManager;
    private ReadAloudManager readAloudManager;
    private ScreenshotManager screenshotManager;
    private SidebarManager sidebarManager;
    private SideToolsManager sideToolsManager;
    private ThemeManager themeManager;

    // Adapters
    private TabPagerAdapter pagerAdapter;
    private TabListAdapter tabListAdapter;

    // WebView cache
    private final Map<String, IBrowserView> webViewCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            initManagers();
            initViews();
            setupToolbar();
            setupViewPager();
            setupTabDrawer();
            setupBottomSheet();
            setupListeners();
            handleIntent(getIntent());
            checkGoogleWebView();
        } catch (Exception e) {
            BrowserLogger.getInstance().logCrash("MainActivity onCreate", e);
            Toast.makeText(this, "启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initManagers() {
        try {
            tabManager = TabManager.getInstance();
            tabGroupManager = TabGroupManager.getInstance();
            sleepingTabManager = SleepingTabManager.getInstance();
            downloadManager = DownloadManager.getInstance(this);
            privacyManager = PrivacyManager.getInstance(this);
            performanceManager = PerformanceManager.getInstance();
            taskManager = TaskManager.getInstance();
            mediaController = MediaController.getInstance();
            pipManager = PictureInPictureManager.getInstance();
            readerModeManager = ReaderModeManager.getInstance(this);
            readAloudManager = ReadAloudManager.getInstance(this);
            screenshotManager = ScreenshotManager.getInstance();
            sidebarManager = SidebarManager.getInstance();
            sideToolsManager = SideToolsManager.getInstance();
            themeManager = ThemeManager.getInstance();
            themeManager.init(this);
            themeManager.applyTheme(this);
            sleepingTabManager.start();
            privacyManager.applyTrackingProtection(PrivacyManager.TrackingLevel.BALANCED);
        } catch (Exception e) {
            BrowserLogger.getInstance().logCrash("initManagers", e);
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        urlBar = findViewById(R.id.url_bar);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnHome = findViewById(R.id.btn_home);
        btnTabs = findViewById(R.id.btn_tabs);
        btnMenu = findViewById(R.id.btn_menu);
        progressBar = findViewById(R.id.progress_bar);
        viewPager = findViewById(R.id.view_pager);
        drawerLayout = findViewById(R.id.drawer_layout);
        tabsRecyclerView = findViewById(R.id.tabs_recycler);
        bottomSheet = findViewById(R.id.bottom_sheet);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    private void setupViewPager() {
        pagerAdapter = new TabPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(true);

        // Create initial tab
        TabItem initialTab = tabManager.addTab("新标签页", "about:blank");
        pagerAdapter.addTab(initialTab);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                TabItem tab = tabManager.getTabAt(position);
                if (tab != null) {
                    tabManager.switchToTab(position);
                    IBrowserView wv = getCurrentWebView();
                    if (wv != null) {
                        updateUrlBar(wv.getUrl());
                        updateNavigationButtons(wv.canGoBack(), wv.canGoForward());
                    }
                }
            }
        });
    }

    private void setupTabDrawer() {
        tabListAdapter = new TabListAdapter(this, new ArrayList<>(), new TabListAdapter.TabListCallback() {
            @Override public void onTabClicked(int index) {
                tabManager.switchToTab(index);
                viewPager.setCurrentItem(index, false);
                drawerLayout.closeDrawer(GravityCompat.END);
            }
            @Override public void onTabClosed(int index) {
                tabManager.removeTab(index);
                tabListAdapter.updateTabs(tabManager.getAllTabs());
            }
            @Override public void onTabPinned(int index) {
                tabManager.pinTab(index);
            }
        });
        tabsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tabsRecyclerView.setAdapter(tabListAdapter);
    }

    private void setupBottomSheet() {
        // BottomSheet 在 DrawerLayout 中，不需要 CoordinatorLayout 的 Behavior
        if (bottomSheet != null) {
            bottomSheet.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            IBrowserView wv = getCurrentWebView();
            if (wv != null && wv.canGoBack()) wv.goBack();
        });
        btnForward.setOnClickListener(v -> {
            IBrowserView wv = getCurrentWebView();
            if (wv != null && wv.canGoForward()) wv.goForward();
        });
        btnRefresh.setOnClickListener(v -> {
            IBrowserView wv = getCurrentWebView();
            if (wv != null) {
                if (wv.isLoading()) wv.stopLoading();
                else wv.reload();
            }
        });
        btnHome.setOnClickListener(v -> navigateTo("https://www.google.com"));
        btnTabs.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                tabListAdapter.updateTabs(tabManager.getAllTabs());
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });
        btnMenu.setOnClickListener(v -> showMainMenu());

        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            String url = urlBar.getText().toString().trim();
            if (!url.isEmpty()) navigateTo(url);
            return true;
        });
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String url = intent.getDataString();
            if (url != null) navigateTo(url);
        }
    }

    // === TabPagerAdapter.WebViewProvider ===

    @Override
    public IBrowserView getOrCreateWebView(TabItem tab) {
        IBrowserView wv = webViewCache.get(tab.getId());
        if (wv == null) {
            wv = createBrowserView();
            final IBrowserView finalWv = wv;
            wv.setCallback(new EdgeWebView.WebViewCallback() {
                @Override public void onPageStarted(String url) {
                    handler.post(() -> urlBar.setText(url));
                }
                @Override public void onPageFinished(String url, String title) {
                    handler.post(() -> {
                        urlBar.setText(url);
                        updateNavigationButtons(finalWv.canGoBack(), finalWv.canGoForward());
                    });
                }
                @Override public void onProgressChanged(int progress) {
                    handler.post(() -> {
                        progressBar.setProgress(progress);
                        progressBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
                    });
                }
                @Override public void onReceivedIcon(Bitmap icon) {}
                @Override public void onReceivedError(int code, String desc, String url) {
                    handler.post(() -> Toast.makeText(MainActivity.this,
                            "加载失败: " + desc, Toast.LENGTH_SHORT).show());
                }
                @Override public void onTitleChanged(String title) {
                    handler.post(() -> {
                        if (toolbar != null) toolbar.setTitle(title != null ? title : "极速浏览器");
                    });
                }
                @Override public void onUrlChanged(String url) {
                    handler.post(() -> urlBar.setText(url));
                }
            });
            webViewCache.put(tab.getId(), wv);
        }
        return wv;
    }

    private IBrowserView createBrowserView() {
        try {
            if (GeckoRuntimeManager.getInstance().isInitialized()) {
                BrowserLogger.getInstance().i(TAG, BrowserLogger.LogCategory.SYSTEM, "使用 Gecko 引擎");
                return new GeckoWebView(this);
            }
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, BrowserLogger.LogCategory.SYSTEM,
                    "Gecko 不可用，回退 Chromium", e);
        }
        return new EdgeWebView(this);
    }

    // === Navigation ===

    private void navigateTo(String url) {
        if (url == null || url.isEmpty()) return;
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("about:")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://" + url;
            } else {
                url = "https://www.google.com/search?q=" + url;
            }
        }
        urlBar.setText(url);
        IBrowserView wv = getCurrentWebView();
        if (wv != null) wv.loadUrl(url);
    }

    private void newTab(String url) {
        TabItem tab = tabManager.addTab("新标签页", url);
        pagerAdapter.addTab(tab);
        viewPager.setCurrentItem(pagerAdapter.getItemCount() - 1, true);
    }

    private void updateUrlBar(String url) {
        if (url != null && !url.isEmpty() && !"about:blank".equals(url)) {
            urlBar.setText(url);
        }
    }

    private void updateNavigationButtons(boolean canBack, boolean canForward) {
        if (btnBack != null) btnBack.setEnabled(canBack);
        if (btnForward != null) btnForward.setEnabled(canForward);
    }

    private IBrowserView getCurrentWebView() {
        int pos = viewPager.getCurrentItem();
        TabItem tab = tabManager.getTabAt(pos);
        if (tab != null) return webViewCache.get(tab.getId());
        return null;
    }

    // === Main Menu ===

    private void showMainMenu() {
        IBrowserView wv = getCurrentWebView();
        String[] items = wv != null && wv.isWebViewBased() ?
                new String[]{"新标签", "阅读模式", "朗读", "截图", "添加到书签",
                        "下载管理", "历史记录", "隐私设置", "性能中心", "任务管理器", "设置", "日志查看"} :
                new String[]{"新标签", "截图", "添加到书签", "下载管理", "历史记录",
                        "隐私设置", "性能中心", "设置", "日志查看"};

        new AlertDialog.Builder(this)
                .setTitle("菜单")
                .setItems(items, (dialog, which) -> {
                    String item = items[which];
                    switch (item) {
                        case "新标签": newTab("about:blank"); break;
                        case "阅读模式": enableReaderMode(); break;
                        case "朗读": startReadAloud(); break;
                        case "截图": takeScreenshot(); break;
                        case "添加到书签": addBookmark(); break;
                        case "下载管理": startActivity(new Intent(this, DownloadActivity.class)); break;
                        case "历史记录": startActivity(new Intent(this, HistoryActivity.class)); break;
                        case "隐私设置": showPrivacyDialog(); break;
                        case "性能中心": startActivity(new Intent(this, PerformanceActivity.class)); break;
                        case "任务管理器": startActivity(new Intent(this, TaskManagerActivity.class)); break;
                        case "设置": startActivity(new Intent(this, SettingsActivity.class)); break;
                        case "日志查看": showLogViewer(); break;
                    }
                })
                .show();
    }

    // === Feature Methods ===

    private void enableReaderMode() {
        IBrowserView wv = getCurrentWebView();
        if (wv == null) return;
        if (!wv.isWebViewBased()) {
            Toast.makeText(this, "阅读模式仅支持 Chromium 引擎", Toast.LENGTH_SHORT).show();
            return;
        }
        readerModeManager.enableReaderMode((EdgeWebView) wv);
        Toast.makeText(this, "阅读模式已启用", Toast.LENGTH_SHORT).show();
    }

    private void startReadAloud() {
        IBrowserView wv = getCurrentWebView();
        if (wv == null) return;
        if (!wv.isWebViewBased()) {
            Toast.makeText(this, "朗读功能仅支持 Chromium 引擎", Toast.LENGTH_SHORT).show();
            return;
        }
        readAloudManager.readAloud((EdgeWebView) wv);
        Toast.makeText(this, "开始朗读", Toast.LENGTH_SHORT).show();
    }

    private void takeScreenshot() {
        IBrowserView wv = getCurrentWebView();
        if (wv == null) return;
        try {
            Bitmap bmp = wv.captureBitmap();
            if (bmp != null) {
                String path = screenshotManager.saveBitmap(bmp);
                if (path != null) {
                    Toast.makeText(this, "截图已保存: " + path, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "截图保存失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "截图失败: 页面未加载", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "截图失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addBookmark() {
        IBrowserView wv = getCurrentWebView();
        if (wv == null) return;
        String url = wv.getUrl();
        String title = wv.getTitle();
        if (url != null && !url.isEmpty()) {
            BrowserLogger.getInstance().i(TAG, BrowserLogger.LogCategory.BROWSER,
                    "添加书签: " + title + " - " + url);
            Toast.makeText(this, "已添加书签: " + (title != null ? title : url), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPrivacyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("隐私设置")
                .setItems(new String[]{"清除浏览数据", "跟踪保护(基础)", "跟踪保护(平衡)", "跟踪保护(严格)"},
                        (d, which) -> {
                            switch (which) {
                                case 0: privacyManager.clearBrowsingData(true, true, true);
                                    Toast.makeText(this, "浏览数据已清除", Toast.LENGTH_SHORT).show(); break;
                                case 1: privacyManager.applyTrackingProtection(PrivacyManager.TrackingLevel.BASIC); break;
                                case 2: privacyManager.applyTrackingProtection(PrivacyManager.TrackingLevel.BALANCED); break;
                                case 3: privacyManager.applyTrackingProtection(PrivacyManager.TrackingLevel.STRICT); break;
                            }
                        }).show();
    }

    // === Log Viewer ===

    private void showLogViewer() {
        String logs = BrowserLogger.getInstance().getAllLogs();
        StringBuilder info = new StringBuilder();
        info.append("=== 浏览器信息 ===\n");
        info.append(ChromiumWebViewFactory.getInstance().getChromiumInfo());
        info.append("\n当前引擎: ");
        IBrowserView wv = getCurrentWebView();
        info.append(wv != null ? wv.getEngineType() : "无");
        info.append("\n\n=== 运行日志 ===\n");
        info.append(logs);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("日志查看")
                .setMessage(info.toString())
                .setPositiveButton("导出日志", (d, which) -> {
                    File file = BrowserLogger.getInstance().exportLogs();
                    String path = file != null ? file.getAbsolutePath() : "导出失败";
                    Toast.makeText(this, "日志已导出到: " + path, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("清除日志", (d, which) -> {
                    BrowserLogger.getInstance().clearLogs();
                    Toast.makeText(this, "日志已清除", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("关闭", null)
                .create();
        dialog.show();
    }

    private void checkGoogleWebView() {
        ChromiumWebViewFactory factory = ChromiumWebViewFactory.getInstance();
        if (factory.shouldPromptInstallGoogleWebView()) {
            handler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    factory.showInstallGoogleWebViewDialog(this);
                }
            }, 500);
        }
    }

    // === Lifecycle ===

    @Override
    protected void onResume() {
        super.onResume();
        IBrowserView wv = getCurrentWebView();
        if (wv != null) wv.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        IBrowserView wv = getCurrentWebView();
        if (wv != null) wv.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (IBrowserView wv : webViewCache.values()) {
            try { wv.destroy(); } catch (Exception ignored) {}
        }
        webViewCache.clear();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            return;
        }
        IBrowserView wv = getCurrentWebView();
        if (wv != null && wv.canGoBack()) {
            wv.goBack();
        } else {
            super.onBackPressed();
        }
    }
}