package com.edge.browser.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edge.browser.tab.TabItem;
import com.edge.browser.webview.EdgeWebView;

import java.util.ArrayList;
import java.util.List;

public class TabPagerAdapter extends RecyclerView.Adapter<TabPagerAdapter.TabViewHolder> {

    public interface WebViewProvider {
        EdgeWebView getOrCreateWebView(TabItem tab);
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
        EdgeWebView webView = webViewProvider.getOrCreateWebView(tab);

        FrameLayout container = (FrameLayout) holder.itemView;
        container.removeAllViews();

        if (webView.getParent() != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
        }
        container.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
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