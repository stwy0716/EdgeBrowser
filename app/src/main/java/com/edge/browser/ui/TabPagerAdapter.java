package com.edge.browser.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edge.browser.tab.TabItem;
import com.edge.browser.webview.IBrowserView;

import java.util.ArrayList;
import java.util.List;

public class TabPagerAdapter extends RecyclerView.Adapter<TabPagerAdapter.TabViewHolder> {

    public interface WebViewProvider {
        IBrowserView getOrCreateWebView(TabItem tab);
        View getNewTabPage();
    }

    private final Context context;
    private final List<TabItem> tabs;
    private final WebViewProvider webViewProvider;

    public TabPagerAdapter(Context context) {
        this.context = context;
        this.tabs = new ArrayList<>();
        this.webViewProvider = (WebViewProvider) context;
    }

    public void addTab(TabItem tab) {
        tabs.add(tab);
        notifyItemInserted(tabs.size() - 1);
    }

    public void removeTab(TabItem tab) {
        int index = tabs.indexOf(tab);
        if (index >= 0) {
            tabs.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void updateTabs(List<TabItem> newTabs) {
        tabs.clear();
        tabs.addAll(newTabs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = new FrameLayout(context);
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return new TabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        TabItem tab = tabs.get(position);
        FrameLayout container = (FrameLayout) holder.itemView;
        container.removeAllViews();

        // 新标签页使用特殊页面
        if (isNewTab(tab)) {
            View newTabPage = webViewProvider.getNewTabPage();
            if (newTabPage != null) {
                if (newTabPage.getParent() != null) {
                    ((ViewGroup) newTabPage.getParent()).removeView(newTabPage);
                }
                container.addView(newTabPage, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
            }
            return;
        }

        IBrowserView webView = webViewProvider.getOrCreateWebView(tab);
        if (webView == null) return;

        View view = webView.getView();
        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        container.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 如果 WebView 是空白但标签页有 URL，自动加载
        String wvUrl = webView.getUrl();
        String tabUrl = tab.getUrl();
        if ((wvUrl == null || wvUrl.isEmpty() || "about:blank".equals(wvUrl))
                && tabUrl != null && !tabUrl.isEmpty()
                && !"about:blank".equals(tabUrl)) {
            webView.loadUrl(tabUrl);
        }
    }

    private boolean isNewTab(TabItem tab) {
        String url = tab.getUrl();
        return url == null || url.isEmpty() || "about:blank".equals(url);
    }

    @Override
    public int getItemCount() {
        return tabs.size();
    }

    static class TabViewHolder extends RecyclerView.ViewHolder {
        TabViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}