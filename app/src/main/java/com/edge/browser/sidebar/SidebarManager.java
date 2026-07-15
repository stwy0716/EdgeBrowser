package com.edge.browser.sidebar;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.edge.browser.webview.EdgeWebView;

import java.util.ArrayList;
import java.util.List;

public class SidebarManager {

    private static SidebarManager instance;
    private boolean isVisible = false;
    private boolean isGameBrowser = false;
    private final List<SidebarItem> items = new ArrayList<>();
    private SidebarListener listener;

    public static class SidebarItem {
        public String id;
        public String name;
        public String iconUrl;
        public String url;
        public boolean isBuiltIn;

        public SidebarItem(String id, String name, String url, boolean isBuiltIn) {
            this.id = id;
            this.name = name;
            this.url = url;
            this.isBuiltIn = isBuiltIn;
        }
    }

    public interface SidebarListener {
        void onSidebarOpened();
        void onSidebarClosed();
        void onItemSelected(SidebarItem item);
    }

    private SidebarManager() {
        initBuiltInItems();
    }

    public static synchronized SidebarManager getInstance() {
        if (instance == null) {
            instance = new SidebarManager();
        }
        return instance;
    }

    private void initBuiltInItems() {
        items.add(new SidebarItem("calculator", "计算器", "tools://calculator", true));
        items.add(new SidebarItem("translator", "翻译", "tools://translator", true));
        items.add(new SidebarItem("converter", "单位换算", "tools://converter", true));
        items.add(new SidebarItem("currency", "汇率换算", "tools://currency", true));
        items.add(new SidebarItem("todo", "待办清单", "tools://todo", true));
        items.add(new SidebarItem("screenshot", "截图", "tools://screenshot", true));
        items.add(new SidebarItem("search", "网页搜索", "tools://search", true));
        items.add(new SidebarItem("shopping", "购物比价", "tools://shopping", true));
        items.add(new SidebarItem("email", "邮箱", "tools://email", true));
    }

    public void setListener(SidebarListener listener) {
        this.listener = listener;
    }

    public void toggle() {
        isVisible = !isVisible;
        if (isVisible) {
            if (listener != null) listener.onSidebarOpened();
        } else {
            if (listener != null) listener.onSidebarClosed();
        }
    }

    public void show() {
        isVisible = true;
        if (listener != null) listener.onSidebarOpened();
    }

    public void hide() {
        isVisible = false;
        if (listener != null) listener.onSidebarClosed();
    }

    public boolean isVisible() {
        return isVisible;
    }

    public List<SidebarItem> getItems() {
        return new ArrayList<>(items);
    }

    public void addCustomItem(String name, String url) {
        String id = "custom_" + System.currentTimeMillis();
        items.add(new SidebarItem(id, name, url, false));
    }

    public void removeItem(String id) {
        items.removeIf(item -> item.id.equals(id) && !item.isBuiltIn);
    }

    public void selectItem(SidebarItem item) {
        if (listener != null) listener.onItemSelected(item);
    }

    // Game browser mode
    public void setGameBrowserMode(boolean enabled) {
        this.isGameBrowser = enabled;
    }

    public boolean isGameBrowser() {
        return isGameBrowser;
    }

    public void openGameBrowser(Context context, String url) {
        isGameBrowser = true;
        // Open a floating window for game browsing
    }
}