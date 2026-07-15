package com.edge.browser.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edge.browser.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Edge 风格新标签页
 * 包含必应搜索栏、常用站点、新闻资讯
 */
public class NewTabPage {

    private final Context context;
    private final View rootView;
    private final EditText searchInput;
    private final RecyclerView quickLinksGrid;
    private final RecyclerView newsFeed;
    private final QuickLinkAdapter quickLinkAdapter;
    private final NewsAdapter newsAdapter;
    private NewTabCallback callback;

    public interface NewTabCallback {
        void onSearch(String query);
        void onQuickLinkClick(String title, String url);
        void onNewsClick(String title, String url);
        void onVoiceSearch();
    }

    public NewTabPage(Context context) {
        this.context = context;
        this.rootView = LayoutInflater.from(context).inflate(R.layout.new_tab_page, null);

        searchInput = rootView.findViewById(R.id.new_tab_search);
        quickLinksGrid = rootView.findViewById(R.id.quick_links_grid);
        newsFeed = rootView.findViewById(R.id.news_feed);

        // 语音搜索
        ImageView btnVoice = rootView.findViewById(R.id.btn_voice_search);
        btnVoice.setOnClickListener(v -> {
            if (callback != null) callback.onVoiceSearch();
        });

        // 搜索
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty() && callback != null) {
                callback.onSearch(query);
            }
            return true;
        });

        // 常用站点
        quickLinksGrid.setLayoutManager(new GridLayoutManager(context, 4));
        quickLinkAdapter = new QuickLinkAdapter(createQuickLinks());
        quickLinksGrid.setAdapter(quickLinkAdapter);

        // 新闻
        newsFeed.setLayoutManager(new LinearLayoutManager(context));
        newsAdapter = new NewsAdapter(createNewsFeed());
        newsFeed.setAdapter(newsAdapter);
    }

    public View getView() {
        return rootView;
    }

    public void setCallback(NewTabCallback callback) {
        this.callback = callback;
    }

    private List<QuickLink> createQuickLinks() {
        List<QuickLink> links = new ArrayList<>();
        links.add(new QuickLink("百度", "https://www.baidu.com", R.drawable.ic_edge_search));
        links.add(new QuickLink("淘宝", "https://www.taobao.com", R.drawable.ic_edge_search));
        links.add(new QuickLink("京东", "https://www.jd.com", R.drawable.ic_edge_search));
        links.add(new QuickLink("微博", "https://weibo.com", R.drawable.ic_edge_search));
        links.add(new QuickLink("知乎", "https://www.zhihu.com", R.drawable.ic_edge_search));
        links.add(new QuickLink("B站", "https://www.bilibili.com", R.drawable.ic_edge_search));
        links.add(new QuickLink("抖音", "https://www.douyin.com", R.drawable.ic_edge_search));
        links.add(new QuickLink("GitHub", "https://github.com", R.drawable.ic_edge_search));
        return links;
    }

    private List<NewsItem> createNewsFeed() {
        List<NewsItem> news = new ArrayList<>();
        news.add(new NewsItem("探索 Microsoft Edge 最新功能，AI 助手 Copilot 现已集成", "微软官方", "1 小时前"));
        news.add(new NewsItem("2026 年全球科技趋势：AI 大模型进入新阶段", "科技日报", "2 小时前"));
        news.add(new NewsItem("Google 发布 Android 16 开发者预览版", "IT之家", "3 小时前"));
        news.add(new NewsItem("5G-A 网络商用加速，6G 研发取得突破", "通信世界", "5 小时前"));
        news.add(new NewsItem("OpenAI 推出新一代多模态模型", "机器之心", "6 小时前"));
        news.add(new NewsItem("中国空间站新舱段完成对接，航天员顺利入驻", "新华社", "8 小时前"));
        return news;
    }

    // === 数据模型 ===

    static class QuickLink {
        String title;
        String url;
        int iconRes;

        QuickLink(String title, String url, int iconRes) {
            this.title = title;
            this.url = url;
            this.iconRes = iconRes;
        }
    }

    static class NewsItem {
        String title;
        String source;
        String time;

        NewsItem(String title, String source, String time) {
            this.title = title;
            this.source = source;
            this.time = time;
        }
    }

    // === 常用站点适配器 ===

    private class QuickLinkAdapter extends RecyclerView.Adapter<QuickLinkAdapter.ViewHolder> {

        private final List<QuickLink> items;

        QuickLinkAdapter(List<QuickLink> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quick_link, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            QuickLink item = items.get(position);
            holder.icon.setImageResource(item.iconRes);
            holder.title.setText(item.title);
            holder.itemView.setOnClickListener(v -> {
                if (callback != null) callback.onQuickLinkClick(item.title, item.url);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView title;

            ViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.quick_link_icon);
                title = itemView.findViewById(R.id.quick_link_title);
            }
        }
    }

    // === 新闻适配器 ===

    private class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

        private final List<NewsItem> items;

        NewsAdapter(List<NewsItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_news, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NewsItem item = items.get(position);
            holder.title.setText(item.title);
            holder.source.setText(item.source);
            holder.time.setText(item.time);
            holder.itemView.setOnClickListener(v -> {
                // 搜索新闻标题
                if (callback != null) {
                    callback.onNewsClick(item.title,
                            "https://www.bing.com/search?q=" + item.title);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, source, time;

            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.news_title);
                source = itemView.findViewById(R.id.news_source);
                time = itemView.findViewById(R.id.news_time);
            }
        }
    }
}