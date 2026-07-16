package com.edge.browser.ui;
import com.edge.browser.R;

import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class LinkPreviewManager {

    public interface LinkPreviewCallback {
        void onOpen();
        void onOpenInNewTab();
        void onCopyLink();
        void onShare();
    }

    public LinkPreviewManager() {}

    public void showLinkPreview(Context context, View anchor, String url, LinkPreviewCallback callback) {
        if (context == null || anchor == null || url == null) return;

        View popupView = LayoutInflater.from(context).inflate(R.layout.link_preview_card, null);

        TextView titleText = popupView.findViewById(R.id.link_preview_title);
        TextView urlText = popupView.findViewById(R.id.link_preview_url);
        ImageView faviconImage = popupView.findViewById(R.id.link_preview_favicon);
        LinearLayout btnOpen = popupView.findViewById(R.id.link_preview_btn_open);
        LinearLayout btnNewTab = popupView.findViewById(R.id.link_preview_btn_new_tab);
        LinearLayout btnCopy = popupView.findViewById(R.id.link_preview_btn_copy);
        LinearLayout btnShare = popupView.findViewById(R.id.link_preview_btn_share);

        titleText.setText(extractTitle(url));
        urlText.setText(url);

        PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        btnOpen.setOnClickListener(v -> {
            if (callback != null) callback.onOpen();
            popupWindow.dismiss();
        });

        btnNewTab.setOnClickListener(v -> {
            if (callback != null) callback.onOpenInNewTab();
            popupWindow.dismiss();
        });

        btnCopy.setOnClickListener(v -> {
            if (callback != null) callback.onCopyLink();
            popupWindow.dismiss();
        });

        btnShare.setOnClickListener(v -> {
            if (callback != null) callback.onShare();
            popupWindow.dismiss();
        });

        popupWindow.showAtLocation(anchor, Gravity.CENTER, 0, 0);
    }

    private String extractTitle(String url) {
        if (url == null || url.isEmpty()) return "未知页面";
        try {
            String host = url;
            if (url.startsWith("http://") || url.startsWith("https://")) {
                int start = url.indexOf("://") + 3;
                int end = url.indexOf('/', start);
                host = end > 0 ? url.substring(start, end) : url.substring(start);
            }
            int colon = host.indexOf(':');
            if (colon > 0) host = host.substring(0, colon);
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return "未知页面";
        }
    }
}