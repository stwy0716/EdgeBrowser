package com.edge.browser.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.edge.browser.R;
import com.edge.browser.tab.TabItem;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class TabListAdapter extends RecyclerView.Adapter<TabListAdapter.ViewHolder> {

    private Context context;
    private List<TabItem> tabs;
    private TabListCallback callback;

    public interface TabListCallback {
        void onTabClicked(int index);
        void onTabClosed(int index);
        void onTabPinned(int index);
    }

    public TabListAdapter(Context context, List<TabItem> tabs, TabListCallback callback) {
        this.context = context;
        this.tabs = tabs;
        this.callback = callback;
    }

    public void updateTabs(List<TabItem> newTabs) {
        this.tabs = newTabs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tab_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TabItem tab = tabs.get(position);

        holder.titleText.setText(tab.getTitle());
        holder.urlText.setText(tab.getUrl());

        // Set group color indicator
        if (tab.getGroupColor() != -1) {
            holder.groupIndicator.setBackgroundColor(tab.getGroupColor());
            holder.groupIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.groupIndicator.setVisibility(View.GONE);
        }

        // Pinned tab indicator
        if (tab.isPinned()) {
            holder.pinIcon.setVisibility(View.VISIBLE);
        } else {
            holder.pinIcon.setVisibility(View.GONE);
        }

        // Sleeping tab indicator
        if (tab.isSleeping()) {
            holder.card.setAlpha(0.6f);
        } else {
            holder.card.setAlpha(1.0f);
        }

        // Muted tab
        if (tab.isMuted()) {
            holder.muteIcon.setVisibility(View.VISIBLE);
        } else {
            holder.muteIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> callback.onTabClicked(position));
        holder.closeIcon.setOnClickListener(v -> callback.onTabClosed(position));
        holder.pinIcon.setOnClickListener(v -> {
            callback.onTabPinned(position);
        });
    }

    @Override
    public int getItemCount() {
        return tabs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView titleText;
        TextView urlText;
        ImageView closeIcon;
        ImageView pinIcon;
        ImageView muteIcon;
        View groupIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.tab_card);
            titleText = itemView.findViewById(R.id.tab_title);
            urlText = itemView.findViewById(R.id.tab_url);
            closeIcon = itemView.findViewById(R.id.btn_tab_close);
            pinIcon = itemView.findViewById(R.id.btn_tab_pin);
            muteIcon = itemView.findViewById(R.id.btn_tab_mute);
            groupIndicator = itemView.findViewById(R.id.group_indicator);
        }
    }
}