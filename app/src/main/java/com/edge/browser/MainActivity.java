package com.edge.browser;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
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
import com.edge.browser.ui.NewTabPage;
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

    // Views - 底部栏
    private TextView urlBarText;
    private ImageView urlLockIcon;
    private ImageView btnShare, btnRefreshBar;
    private ImageView btnBack, btnForward, btnHome, btnMenu;
    private FrameLayout btnTabsContainer;
    private TextView tabCountText;
    private ProgressBar progressBar;
    private ViewPager2 viewPager;
    private DrawerLayout drawerLayout;
    private RecyclerView tabsRecyclerView;
    private TextView tabCountLabel;

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

    // New tab page
    private NewTabPage newTabPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            initManagers();
            initViews();
            setupViewPager();
            setupTabDrawer();
            setupListeners();
            handleIntent(getIntent());
            checkGoogleWebView();
            updateTabCount();
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
        // 底部地址栏
        urlBarText = findViewById(R.id.url_bar_text);
        urlLockIcon = findViewById(R.id.url_lock_icon);
        btnShare = findViewById(R.id.btn_share);
        btnRefreshBar = findViewById(R.id.btn_refresh_bar);

        // 底部导航
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnHome = findViewById(R.id.btn_home);
        btnTabsContainer = findViewById(R.id.btn_tabs_container);
        tabCountText = findViewById(R.id.tab_count_text);
        btnMenu = findViewById(R.id.btn_menu);

        // 内容区
        progressBar = findViewById(R.id.progress_bar);
        viewPager = findViewById(R.id.view_pager);
        drawerLayout = findViewById(R.id.drawer_layout);
        tabsRecyclerView = findViewById(R.id.tabs_recycler);
        tabCountLabel = findViewById(R.id.tab_count_label);
    }

    private void setupViewPager() {
        pagerAdapter = new TabPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(true);

        // 创建初始标签页
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
                        updateLockIcon(wv.getUrl());
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
                pagerAdapter.updateTabs(tabManager.getAllTabs());
                tabListAdapter.updateTabs(tabManager.getAllTabs());
                updateTabCount();
            }
            @Override public void onTabPinned(int index) {
                tabManager.pinTab(index);
            }
        });
        tabsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tabsRecyclerView.setAdapter(tabListAdapter);

        // 新建 InPrivate 标签
        TextView btnIncognito = drawerLayout.findViewById(R.id.btn_new_incognito);
        if (btnIncognito != null) {
            btnIncognito.setOnClickListener(v -> {
                TabItem tab = tabManager.addTab("InPrivate", "about:blank");
                pagerAdapter.addTab(tab);
                viewPager.setCurrentItem(pagerAdapter.getItemCount() - 1, true);
                drawerLayout.closeDrawer(GravityCompat.END);
                updateTabCount();
            });
        }

        // 关闭所有标签
        TextView btnCloseAll = drawerLayout.findViewById(R.id.btn_close_all);
        if (btnCloseAll != null) {
            btnCloseAll.setOnClickListener(v -> {
                List<TabItem> tabs = tabManager.getAllTabs();
                for (int i = tabs.size() - 1; i >= 0; i--) {
                    tabManager.removeTab(i);
                }
                // 保留一个新标签页
                TabItem newTab = tabManager.addTab("新标签页", "about:blank");
                pagerAdapter.updateTabs(tabManager.getAllTabs());
                tabListAdapter.updateTabs(tabManager.getAllTabs());
                viewPager.setCurrentItem(0, false);
                drawerLayout.closeDrawer(GravityCompat.END);
                updateTabCount();
            });
        }
    }

    private void setupListeners() {
        // 地址栏点击 - 弹出 URL 输入框
        urlBarText.setOnClickListener(v -> showUrlInputDialog());

        // 后退
        btnBack.setOnClickListener(v -> {
            IBrowserView wv = getCurrentWebView();
            if (wv != null && wv.canGoBack()) wv.goBack();
        });

        // 前进
        btnForward.setOnClickListener(v -> {
            IBrowserView wv = getCurrentWebView();
            if (wv != null && wv.canGoForward()) wv.goForward();
        });

        // 主页
        btnHome.setOnClickListener(v -> navigateTo("https://www.google.com"));

        // 刷新 (地址栏右侧)
        btnRefreshBar.setOnClickListener(v -> {
            IBrowserView wv = getCurrentWebView();
            if (wv != null) {
                if (wv.isLoading()) wv.stopLoading();
                else wv.reload();
            }
        });

        // 分享
        btnShare.setOnClickListener(v -> {
            IBrowserView wv = getCurrentWebView();
            if (wv == null) return;
            String url = wv.getUrl();
            if (url != null && !url.isEmpty()) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, url);
                startActivity(Intent.createChooser(share, "分享链接"));
            }
        });

        // 标签页抽屉
        btnTabsContainer.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                tabListAdapter.updateTabs(tabManager.getAllTabs());
                updateTabCount();
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        // 菜单
        btnMenu.setOnClickListener(v -> showEdgeMenu());
    }

    // === URL 输入对话框 ===

    private void showUrlInputDialog() {
        EditText input = new EditText(this);
        input.setHint("搜索或输入网址");
        input.setText(urlBarText.getText().toString().replace("搜索或输入网址", ""));
        input.setPadding(32, 24, 32, 24);
        input.setTextSize(16);
        input.setSelectAllOnFocus(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("输入网址")
                .setView(input)
                .setPositiveButton("前往", (d, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        navigateTo(url);
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
        input.requestFocus();
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String url = intent.getDataString();
            if (url != null) navigateTo(url);
        }
    }

    // === TabPagerAdapter.WebViewProvider ===

    @Override
    public View getNewTabPage() {
        if (newTabPage == null) {
            newTabPage = new NewTabPage(this);
            newTabPage.setCallback(new NewTabPage.NewTabCallback() {
                @Override
                public void onSearch(String query) {
                    navigateTo(query);
                }

                @Override
                public void onQuickLinkClick(String title, String url) {
                    navigateTo(url);
                }

                @Override
                public void onNewsClick(String title, String url) {
                    navigateTo(url);
                }

                @Override
                public void onVoiceSearch() {
                    Toast.makeText(MainActivity.this, "语音搜索功能开发中", Toast.LENGTH_SHORT).show();
                }
            });
        }
        return newTabPage.getView();
    }

    @Override
    public IBrowserView getOrCreateWebView(TabItem tab) {
        IBrowserView wv = webViewCache.get(tab.getId());
        if (wv == null) {
            wv = createBrowserView();
            final IBrowserView finalWv = wv;
            wv.setCallback(new EdgeWebView.WebViewCallback() {
                @Override public void onPageStarted(String url) {
                    handler.post(() -> {
                        urlBarText.setText(url);
                        urlBarText.setTextColor(Color.parseColor("#212121"));
                        updateLockIcon(url);
                    });
                }
                @Override public void onPageFinished(String url, String title) {
                    handler.post(() -> {
                        urlBarText.setText(url);
                        urlBarText.setTextColor(Color.parseColor("#212121"));
                        updateNavigationButtons(finalWv.canGoBack(), finalWv.canGoForward());
                        updateLockIcon(url);
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
                @Override public void onTitleChanged(String title) {}
                @Override public void onUrlChanged(String url) {
                    handler.post(() -> {
                        urlBarText.setText(url);
                        urlBarText.setTextColor(Color.parseColor("#212121"));
                        updateLockIcon(url);
                    });
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
                url = "https://www.bing.com/search?q=" + url;
            }
        }
        urlBarText.setText(url);
        urlBarText.setTextColor(Color.parseColor("#212121"));

        // 更新当前标签页的 URL
        TabItem tab = tabManager.getTabAt(viewPager.getCurrentItem());
        if (tab != null) {
            tab.setUrl(url);
            tab.setTitle(url);
        }

        IBrowserView wv = getCurrentWebView();
        if (wv != null) {
            wv.loadUrl(url);
        } else {
            // 新标签页，创建新的 WebView
            newTab(url);
        }
    }

    private void newTab(String url) {
        TabItem tab = tabManager.addTab("新标签页", url);
        pagerAdapter.addTab(tab);
        viewPager.setCurrentItem(pagerAdapter.getItemCount() - 1, true);
        updateTabCount();
    }

    private void updateUrlBar(String url) {
        if (url != null && !url.isEmpty() && !"about:blank".equals(url)) {
            urlBarText.setText(url);
            urlBarText.setTextColor(Color.parseColor("#212121"));
        }
    }

    private void updateLockIcon(String url) {
        if (urlLockIcon == null) return;
        if (url != null && url.startsWith("https://")) {
            urlLockIcon.setVisibility(View.VISIBLE);
        } else {
            urlLockIcon.setVisibility(View.GONE);
        }
    }

    private void updateNavigationButtons(boolean canBack, boolean canForward) {
        if (btnBack != null) {
            btnBack.setAlpha(canBack ? 1.0f : 0.4f);
            btnBack.setEnabled(canBack);
        }
        if (btnForward != null) {
            btnForward.setAlpha(canForward ? 1.0f : 0.4f);
            btnForward.setEnabled(canForward);
        }
    }

    private void updateTabCount() {
        int count = tabManager.getTabCount();
        if (tabCountText != null) {
            if (count > 0) {
                tabCountText.setVisibility(View.VISIBLE);
                tabCountText.setText(String.valueOf(count));
            } else {
                tabCountText.setVisibility(View.GONE);
            }
        }
        if (tabCountLabel != null) {
            tabCountLabel.setText(count + " 个标签页");
        }
    }

    private IBrowserView getCurrentWebView() {
        int pos = viewPager.getCurrentItem();
        TabItem tab = tabManager.getTabAt(pos);
        if (tab != null) {
            String url = tab.getUrl();
            if (url == null || url.isEmpty() || "about:blank".equals(url)) return null;
            return webViewCache.get(tab.getId());
        }
        return null;
    }

    // === Edge 风格菜单 (BottomSheet) ===

    private void showEdgeMenu() {
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_menu, null);
        RecyclerView menuGrid = sheetView.findViewById(R.id.menu_grid);
        menuGrid.setLayoutManager(new GridLayoutManager(this, 4));

        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(new MenuItem("新标签页", R.drawable.ic_tabs, () -> {
            newTab("about:blank");
            updateTabCount();
        }));
        menuItems.add(new MenuItem("InPrivate", R.drawable.ic_edge_home, () -> {
            TabItem tab = tabManager.addTab("InPrivate", "about:blank");
            pagerAdapter.addTab(tab);
            viewPager.setCurrentItem(pagerAdapter.getItemCount() - 1, true);
            updateTabCount();
        }));
        menuItems.add(new MenuItem("阅读模式", R.drawable.ic_reader_mode, this::enableReaderMode));
        menuItems.add(new MenuItem("朗读", R.drawable.ic_volume_up, this::startReadAloud));
        menuItems.add(new MenuItem("截图", R.drawable.ic_pip, this::takeScreenshot));
        menuItems.add(new MenuItem("添加书签", R.drawable.ic_edge_search, this::addBookmark));
        menuItems.add(new MenuItem("下载", R.drawable.ic_download,
                () -> startActivity(new Intent(this, DownloadActivity.class))));
        menuItems.add(new MenuItem("历史", R.drawable.ic_refresh,
                () -> startActivity(new Intent(this, HistoryActivity.class))));
        menuItems.add(new MenuItem("隐私", R.drawable.ic_edge_lock,
                this::showPrivacyDialog));
        menuItems.add(new MenuItem("性能", R.drawable.ic_arrow_forward,
                () -> startActivity(new Intent(this, PerformanceActivity.class))));
        menuItems.add(new MenuItem("任务", R.drawable.ic_more_vertical,
                () -> startActivity(new Intent(this, TaskManagerActivity.class))));
        menuItems.add(new MenuItem("设置", R.drawable.ic_edge_home,
                () -> startActivity(new Intent(this, SettingsActivity.class))));
        menuItems.add(new MenuItem("日志", R.drawable.ic_edge_search,
                this::showLogViewer));
        menuItems.add(new MenuItem("分享", R.drawable.ic_edge_share, () -> {
            IBrowserView wv = getCurrentWebView();
            if (wv != null) {
                String url = wv.getUrl();
                if (url != null) {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, url);
                    startActivity(Intent.createChooser(share, "分享"));
                }
            }
        }));
        menuItems.add(new MenuItem("桌面版", R.drawable.ic_edge_search, () -> {
            IBrowserView wv = getCurrentWebView();
            if (wv != null && wv.isWebViewBased()) {
                EdgeWebView ewv = (EdgeWebView) wv;
                ewv.getSettings().setUserAgentString(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                ewv.reload();
                Toast.makeText(this, "已切换到桌面版", Toast.LENGTH_SHORT).show();
            }
        }));
        menuItems.add(new MenuItem("分屏", R.drawable.ic_pip, () -> {
            Toast.makeText(this, "分屏功能开发中", Toast.LENGTH_SHORT).show();
        }));

        MenuGridAdapter adapter = new MenuGridAdapter(menuItems);
        menuGrid.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(sheetView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
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
            Toast.makeText(this, "已添加书签", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPrivacyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("隐私设置")
                .setItems(new String[]{"清除浏览数据", "跟踪保护(基础)", "跟踪保护(平衡)", "跟踪保护(严格)"},
                        (d, which) -> {
                            switch (which) {
                                case 0: privacyManager.clearBrowsingData(true, true, true);
                                    Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show(); break;
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
                .setPositiveButton("导出", (d, which) -> {
                    File file = BrowserLogger.getInstance().exportLogs();
                    String path = file != null ? file.getAbsolutePath() : "导出失败";
                    Toast.makeText(this, "日志已导出: " + path, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("清除", (d, which) -> {
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

    // === Menu Grid Adapter ===

    private static class MenuItem {
        String label;
        int iconRes;
        Runnable action;

        MenuItem(String label, int iconRes, Runnable action) {
            this.label = label;
            this.iconRes = iconRes;
            this.action = action;
        }
    }

    private class MenuGridAdapter extends RecyclerView.Adapter<MenuGridAdapter.ViewHolder> {

        private final List<MenuItem> items;

        MenuGridAdapter(List<MenuItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_menu_grid, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MenuItem item = items.get(position);
            holder.icon.setImageResource(item.iconRes);
            holder.label.setText(item.label);
            holder.itemView.setOnClickListener(v -> {
                if (item != null && item.action != null) {
                    item.action.run();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView label;

            ViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.menu_item_icon);
                label = itemView.findViewById(R.id.menu_item_label);
            }
        }
    }
}