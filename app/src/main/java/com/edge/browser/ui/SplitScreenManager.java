package com.edge.browser.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.edge.browser.BrowserLogger;
import com.edge.browser.tab.TabItem;

public class SplitScreenManager {

    private static final String TAG = "SplitScreenManager";
    private static SplitScreenManager instance;

    private SplitScreenManager() {}

    public static synchronized SplitScreenManager getInstance() {
        if (instance == null) {
            instance = new SplitScreenManager();
        }
        return instance;
    }

    public boolean isSplitScreenSupported(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getPackageManager().hasSystemFeature(
                    android.content.pm.PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT) ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && true);
        }
        return false;
    }

    public void enterSplitScreen(Activity activity, TabItem tab1, TabItem tab2) {
        if (activity == null || tab1 == null || tab2 == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                // Open the second tab's URL in a new activity to trigger multi-window
                Intent intent = new Intent(activity, activity.getClass());
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra("tab_url", tab2.getUrl());
                intent.putExtra("tab_title", tab2.getTitle());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

                if (activity.isInMultiWindowMode()) {
                    activity.startActivity(intent);
                } else {
                    activity.startActivity(intent);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        activity.enterPictureInPictureMode(
                                new android.app.PictureInPictureParams.Builder().build());
                    }
                }
                BrowserLogger.getInstance().i(TAG, BrowserLogger.LogCategory.SYSTEM,
                        "Split screen entered for tabs: " + tab1.getTitle() + " and " + tab2.getTitle());
            } catch (Exception e) {
                BrowserLogger.getInstance().e(TAG, BrowserLogger.LogCategory.SYSTEM,
                        "Failed to enter split screen", e);
            }
        }
    }
}