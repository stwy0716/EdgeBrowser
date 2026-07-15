package com.edge.browser.media;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.os.Build;
import android.util.Rational;
import android.view.View;
import android.webkit.WebView;

public class PictureInPictureManager {

    private static PictureInPictureManager instance;
    private boolean isInPipMode = false;
    private WebView pipWebView;

    private PictureInPictureManager() {}

    public static synchronized PictureInPictureManager getInstance() {
        if (instance == null) {
            instance = new PictureInPictureManager();
        }
        return instance;
    }

    public void enterPictureInPicture(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build();
            activity.enterPictureInPictureMode(params);
            isInPipMode = true;
        }
    }

    public void enterPictureInPicture(Activity activity, WebView webView) {
        this.pipWebView = webView;
        enterPictureInPicture(activity);
    }

    public boolean isInPipMode() {
        return isInPipMode;
    }

    public void setInPipMode(boolean inPipMode) {
        isInPipMode = inPipMode;
    }

    public WebView getPipWebView() {
        return pipWebView;
    }
}