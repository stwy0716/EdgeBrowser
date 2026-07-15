package com.edge.browser;

import android.app.Application;
import androidx.multidex.MultiDexApplication;

public class EdgeApplication extends MultiDexApplication {

    private static EdgeApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static EdgeApplication getInstance() {
        return instance;
    }
}