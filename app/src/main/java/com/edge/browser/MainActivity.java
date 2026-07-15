package com.edge.browser;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.print.PrintManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.edge.browser.download.DownloadManager;
import com.edge.browser.download.DownloadService;
import com.edge.browser.media.MediaController;
import com.edge.browser.media.PictureInPictureManager;
import com.edge.browser.performance.PerformanceManager;
import com.edge.browser.performance.TaskManager;
import com.edge.browser.privacy.PrivacyManager;
import com.edge.browser.reader.ReaderModeManager;
import com.edge.browser.reader.ReadAloudManager;
import com.edge.browser.screenshot.ScreenshotManager;
import com.edge.browser.sidebar.SidebarManager;
import com.edge.browser.tab.*;
import com.edge.browser.theme.ThemeManager;
import com.edge.browser.tools.SideToolsManager;
import com.edge.browser.ui.*;
import com.edge.browser.webview.EdgeWebView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements TabPagerAdapter.WebViewProvider {

    // UI Components
    private Toolbar toolbar;
    private TextInputEditText urlBar;
    private ImageView btnBack, btnForward, btnRefresh, btnHome, btnTabs;
    private ImageView btnMenu, btnReaderMode, btnReadAloud, btnPiP;
    private ProgressBar progressBar;
    private ViewPager2 viewPager;
    private TabLayout tabIndicator;
    private DrawerLayout drawerLayout;
    private RecyclerView tabsRecyclerView;
    private FrameLayout mainContainer;
    private FrameLayout splitContainer;
    private View splitDivider;

    // Bottom Sheet
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private View bottomSheet;
    private RecyclerView bottomSheetRecycler;

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
    private TabPagerAdapter tabPagerAdapter;
    private TabListAdapter tabListAdapter;

    // State
    private Map<String, EdgeWebView> webViewCache = new HashMap<>();
    private boolean isSplitScreen = false;
    private boolean isVerticalTabs = false;
    private boolean isIncognitoMode = false;
    private boolean isReaderMode = false;
    private String pendingUrl = null;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initManagers();
        initViews();
        setupToolbar();
        setupViewPager();
        setupTabDrawer();
        setupBottomSheet();
        setupListeners();
        handleIntent(getIntent());
    }

    private void initManagers() {
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
        btnReaderMode = findViewById(R.id.btn_reader_mode);
        btnReadAloud = findViewById(R.id.btn_read_aloud);
        btnPiP = findViewById(R.id.btn_pip);
        progressBar = findViewById(R.id.progress_bar);
        viewPager = findViewById(R.id.view_pager);
        tabIndicator = findViewById(R.id.tab_indicator);
        drawerLayout = findViewById(R.id.drawer_layout);
        tabsRecyclerView = findViewById(R.id.tabs_recycler);
        mainContainer = findViewById(R.id.main_container);
        splitContainer = findViewById(R.id.split_container);
        splitDivider = findViewById(R.id.split_divider);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetRecycler = findViewById(R.id.bottom_sheet_recycler);
    }

    private void setupToolbar() {
        btnBack.setOnClickListener(v -> {
            EdgeWebView webView = getCurrentWebView();
            if (webView != null && webView.canGoBack()) webView.goBack();
        });

        btnForward.setOnClickListener(v -> {
            EdgeWebView webView = getCurrentWebView();
            if (webView != null && webView.canGoForward()) webView.goForward();
        });

        btnRefresh.setOnClickListener(v -> {
            EdgeWebView webView = getCurrentWebView();
            if (webView != null) {
                if (webView.getProgress() < 100) {
                    webView.stopLoading();
                } else {
                    webView.reload();
                }
            }
        });

        btnHome.setOnClickListener(v -> navigateTo("about:blank"));
        btnTabs.setOnClickListener(v -> toggleTabDrawer());
        btnMenu.setOnClickListener(v -> showMainMenu());

        btnReaderMode.setOnClickListener(v -> toggleReaderMode());
        btnReadAloud.setOnClickListener(v -> startReadAloud());
        btnPiP.setOnClickListener(v -> pipManager.enterPictureInPicture(this));

        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH) {
                String input = urlBar.getText().toString().trim();
                navigateTo(input);
                urlBar.clearFocus();
                return true;
            }
            return false;
        });

        urlBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                urlBar.selectAll();
            }
        });
    }

    private void setupViewPager() {
        tabPagerAdapter = new TabPagerAdapter(this);
        viewPager.setAdapter(tabPagerAdapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setUserInputEnabled(true);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tabManager.switchToTab(position);
                updateUIForCurrentTab();
            }
        });

        new TabLayoutMediator(tabIndicator, viewPager, (tab, position) -> {
            TabItem tabItem = tabManager.getTabAt(position);
            if (tabItem != null) {
                tab.setText(tabItem.getTitle());
            }
        }).attach();
    }

    private void setupTabDrawer() {
        tabListAdapter = new TabListAdapter(this, tabManager.getAllTabs(),
                new TabListAdapter.TabListCallback() {
                    @Override
                    public void onTabClicked(int index) {
                        viewPager.setCurrentItem(index, false);
                        drawerLayout.closeDrawer(GravityCompat.END);
                    }

                    @Override
                    public void onTabClosed(int index) {
                        tabManager.removeTab(index);
                        tabPagerAdapter.notifyDataSetChanged();
                        tabListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onTabPinned(int index) {
                        tabManager.pinTab(index);
                    }
                });

        tabsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tabsRecyclerView.setAdapter(tabListAdapter);
    }

    private void setupBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setPeekHeight(0);
    }

    private void setupListeners() {
        tabManager.setListener(new TabManager.TabChangeListener() {
            @Override
            public void onTabAdded(TabItem tab) {
                tabPagerAdapter.addTab(tab);
                tabListAdapter.updateTabs(tabManager.getAllTabs());
                viewPager.setCurrentItem(tabPagerAdapter.getItemCount() - 1, false);
            }

            @Override
            public void onTabRemoved(TabItem tab) {
                tabPagerAdapter.removeTab(tab);
                tabListAdapter.updateTabs(tabManager.getAllTabs());
            }

            @Override
            public void onTabSwitched(TabItem tab, int index) {
                updateUIForTab(tab);
                tabListAdapter.updateTabs(tabManager.getAllTabs());
            }

            @Override
            public void onTabUpdated(TabItem tab) {
                tabListAdapter.updateTabs(tabManager.getAllTabs());
            }
        });

        sleepingTabManager.addListener(new SleepingTabManager.SleepListener() {
            @Override
            public void onTabShouldSleep(TabItem tab) {
                EdgeWebView webView = webViewCache.get(tab.getId());
                if (webView != null) {
                    webView.setSleeping(true);
                }
            }

            @Override
            public void onTabShouldWake(TabItem tab) {
                EdgeWebView webView = webViewCache.get(tab.getId());
                if (webView != null) {
                    webView.setSleeping(false);
                }
            }
        });
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                navigateTo(data.toString());
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    // === Navigation ===

    public void navigateTo(String input) {
        if (input == null || input.isEmpty()) return;

        String url = input.trim();
        if (url.equals("about:blank")) {
            // New tab page
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://" + url;
            } else {
                url = "https://www.bing.com/search?q=" + Uri.encode(url);
            }
        }

        // Check for read: prefix
        if (url.startsWith("read:")) {
            String targetUrl = url.substring(5).trim();
            if (!targetUrl.startsWith("http")) targetUrl = "https://" + targetUrl;
            readerModeManager.enableReaderMode(targetUrl);
            url = targetUrl;
        }

        pendingUrl = url;
        TabItem currentTab = tabManager.getCurrentTab();
        if (currentTab == null || currentTab.getUrl().equals("about:blank")) {
            if (currentTab != null) {
                currentTab.setUrl(url);
                currentTab.setTitle(url);
            } else {
                tabManager.addTab(url, url);
            }
        } else {
            tabManager.addTab(url, url);
        }

        EdgeWebView webView = getCurrentWebView();
        if (webView != null) {
            webView.loadUrl(url);
        }
        urlBar.setText(url);
        updateNavigationButtons();
        privacyManager.checkHttpsUpgrade(url);
    }

    // === Reader Mode ===

    private void toggleReaderMode() {
        if (isReaderMode) {
            readerModeManager.disableReaderMode();
            isReaderMode = false;
            btnReaderMode.setImageResource(R.drawable.ic_reader_mode);
        } else {
            EdgeWebView webView = getCurrentWebView();
            if (webView != null) {
                readerModeManager.enableReaderMode(webView.getCurrentUrl());
                isReaderMode = true;
                btnReaderMode.setImageResource(R.drawable.ic_reader_mode_active);
            }
        }
    }

    private void startReadAloud() {
        EdgeWebView webView = getCurrentWebView();
        if (webView != null) {
            readAloudManager.readAloud(webView);
        }
    }

    // === Tab Drawer ===

    private void toggleTabDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            tabListAdapter.updateTabs(tabManager.getAllTabs());
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    // === Main Menu ===

    private void showMainMenu() {
        PopupMenu popup = new PopupMenu(this, btnMenu);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_new_tab) {
                tabManager.addTab("新标签页", "about:blank");
            } else if (id == R.id.action_new_incognito_tab) {
                openIncognitoTab();
            } else if (id == R.id.action_split_screen) {
                toggleSplitScreen();
            } else if (id == R.id.action_collections) {
                startActivity(new Intent(this, CollectionActivity.class));
            } else if (id == R.id.action_downloads) {
                startActivity(new Intent(this, DownloadActivity.class));
            } else if (id == R.id.action_bookmarks) {
                startActivity(new Intent(this, BookmarkActivity.class));
            } else if (id == R.id.action_history) {
                startActivity(new Intent(this, HistoryActivity.class));
            } else if (id == R.id.action_task_manager) {
                startActivity(new Intent(this, TaskManagerActivity.class));
            } else if (id == R.id.action_performance) {
                startActivity(new Intent(this, PerformanceActivity.class));
            } else if (id == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.action_reader_mode) {
                toggleReaderMode();
            } else if (id == R.id.action_read_aloud) {
                startReadAloud();
            } else if (id == R.id.action_screenshot) {
                screenshotManager.captureScreenshot(this, getCurrentWebView());
            } else if (id == R.id.action_translate) {
                translatePage();
            } else if (id == R.id.action_find_in_page) {
                showFindInPage();
            } else if (id == R.id.action_desktop_site) {
                toggleDesktopSite();
            } else if (id == R.id.action_add_to_collections) {
                addToCollections();
            } else if (id == R.id.action_save_as_pdf) {
                saveAsPdf();
            } else if (id == R.id.action_share) {
                sharePage();
            } else if (id == R.id.action_print) {
                printPage();
            }
            return true;
        });

        popup.show();
    }

    // === Split Screen ===

    private void toggleSplitScreen() {
        isSplitScreen = !isSplitScreen;
        if (isSplitScreen) {
            splitContainer.setVisibility(View.VISIBLE);
            splitDivider.setVisibility(View.VISIBLE);
            // Setup split screen WebViews
            setupSplitScreen();
        } else {
            splitContainer.setVisibility(View.GONE);
            splitDivider.setVisibility(View.GONE);
        }
    }

    private void setupSplitScreen() {
        // Implement split screen with two WebViews
        FrameLayout leftPane = findViewById(R.id.split_left);
        FrameLayout rightPane = findViewById(R.id.split_right);

        EdgeWebView leftWebView = new EdgeWebView(this);
        EdgeWebView rightWebView = new EdgeWebView(this);

        leftPane.addView(leftWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rightPane.addView(rightWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        leftWebView.loadUrl(tabManager.getCurrentTab().getUrl());
        rightWebView.loadUrl("https://www.bing.com");
    }

    // === Feature Methods ===

    private void openIncognitoTab() {
        TabItem incognitoTab = new TabItem("InPrivate", "about:blank");
        incognitoTab.setIncognito(true);
        tabManager.addTab(incognitoTab);
    }

    private void translatePage() {
        EdgeWebView webView = getCurrentWebView();
        if (webView != null) {
            String url = webView.getCurrentUrl();
            String translateUrl = "https://translate.google.com/translate?sl=auto&tl=zh-CN&u=" + Uri.encode(url);
            webView.loadUrl(translateUrl);
        }
    }

    private void showFindInPage() {
        // Show find in page dialog
        EdgeWebView webView = getCurrentWebView();
        if (webView != null) {
            webView.findNext(true);
        }
    }

    private void toggleDesktopSite() {
        EdgeWebView webView = getCurrentWebView();
        if (webView != null) {
            WebSettings settings = webView.getSettings();
            String currentUA = settings.getUserAgentString();
            if (currentUA.contains("Mobile")) {
                settings.setUserAgentString(currentUA.replace("Mobile", "").replace("Android", "Windows NT 10.0"));
            } else {
                settings.setUserAgentString(null); // Reset to default
            }
            webView.reload();
        }
    }

    private void addToCollections() {
        EdgeWebView webView = getCurrentWebView();
        if (webView != null) {
            Intent intent = new Intent(this, CollectionActivity.class);
            intent.putExtra("url", webView.getCurrentUrl());
            intent.putExtra("title", webView.getCurrentTitle());
            startActivity(intent);
        }
    }

    private void saveAsPdf() {
        EdgeWebView webView = getCurrentWebView();
        if (webView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);
            printManager.print("EdgeBrowser PDF", webView.createPrintDocumentAdapter("webpage"), null);
        }
    }

    private void sharePage() {
        EdgeWebView webView = getCurrentWebView();
        if (webView != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, webView.getCurrentTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, webView.getCurrentUrl());
            startActivity(Intent.createChooser(shareIntent, "分享到"));
        }
    }

    private void printPage() {
        EdgeWebView webView = getCurrentWebView();
        if (webView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
            pm.print("EdgeBrowser Print", webView.createPrintDocumentAdapter("document"), null);
        }
    }

    // === UI Helpers ===

    private EdgeWebView getCurrentWebView() {
        TabItem currentTab = tabManager.getCurrentTab();
        if (currentTab == null) return null;
        return webViewCache.get(currentTab.getId());
    }

    public EdgeWebView getOrCreateWebView(TabItem tab) {
        EdgeWebView webView = webViewCache.get(tab.getId());
        if (webView == null) {
            webView = new EdgeWebView(this);
            webViewCache.put(tab.getId(), webView);

            webView.setCallback(new EdgeWebView.WebViewCallback() {
                @Override
                public void onPageStarted(String url) {
                    progressBar.setVisibility(View.VISIBLE);
                    urlBar.setText(url);
                }

                @Override
                public void onPageFinished(String url, String title) {
                    progressBar.setVisibility(View.GONE);
                    tab.setTitle(title);
                    tab.setUrl(url);
                    urlBar.setText(url);
                    updateNavigationButtons();
                    updateReaderModeButton();
                }

                @Override
                public void onProgressChanged(int progress) {
                    progressBar.setProgress(progress);
                    if (progress == 100) progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onReceivedIcon(Bitmap icon) {
                    // Update tab icon
                }

                @Override
                public void onReceivedError(int errorCode, String description, String failingUrl) {
                    Toast.makeText(MainActivity.this, "加载失败: " + description, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onTitleChanged(String title) {
                    tab.setTitle(title);
                    urlBar.setText(tab.getUrl());
                }

                @Override
                public void onUrlChanged(String url) {
                    urlBar.setText(url);
                }
            });

            if (tab.getUrl() != null && !tab.getUrl().equals("about:blank")) {
                webView.loadUrl(tab.getUrl());
            }
        }
        return webView;
    }

    private void updateUIForCurrentTab() {
        TabItem tab = tabManager.getCurrentTab();
        if (tab != null) {
            updateUIForTab(tab);
        }
    }

    private void updateUIForTab(TabItem tab) {
        urlBar.setText(tab.getUrl());
        updateNavigationButtons();
        updateReaderModeButton();
    }

    private void updateNavigationButtons() {
        EdgeWebView webView = getCurrentWebView();
        if (webView != null) {
            btnBack.setAlpha(webView.canGoBack() ? 1.0f : 0.4f);
            btnForward.setAlpha(webView.canGoForward() ? 1.0f : 0.4f);
            btnRefresh.setImageResource(
                    webView.getProgress() < 100 ? R.drawable.ic_close : R.drawable.ic_refresh);
        }
    }

    private void updateReaderModeButton() {
        EdgeWebView webView = getCurrentWebView();
        if (webView != null) {
            btnReaderMode.setVisibility(
                    readerModeManager.isReaderAvailable(webView.getCurrentUrl()) ? View.VISIBLE : View.GONE);
        }
    }

    // === Lifecycle ===

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            return;
        }
        EdgeWebView webView = getCurrentWebView();
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            if (tabManager.getTabCount() > 1) {
                tabManager.removeTab(tabManager.getCurrentTabIndex());
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        EdgeWebView webView = getCurrentWebView();
        if (webView != null) webView.onPause();
        performanceManager.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EdgeWebView webView = getCurrentWebView();
        if (webView != null) webView.onResume();
        performanceManager.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sleepingTabManager.stop();
        for (EdgeWebView webView : webViewCache.values()) {
            webView.destroy();
        }
        webViewCache.clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ScreenshotManager.REQUEST_SCREENSHOT) {
            screenshotManager.handleScreenshotResult(resultCode, data);
        }
    }
}