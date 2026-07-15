package com.edge.browser.video;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VideoDownloader {

    private static VideoDownloader instance;
    private final List<String> videoUrls = new ArrayList<>();

    private VideoDownloader() {}

    public static synchronized VideoDownloader getInstance() {
        if (instance == null) {
            instance = new VideoDownloader();
        }
        return instance;
    }

    public void detectVideos(WebView webView) {
        if (webView == null) return;
        clearDetected();
        webView.addJavascriptInterface(new VideoJsInterface(), "EdgeVideoDetector");
        webView.evaluateJavascript(getDetectionJS(), null);
    }

    public String getDetectionJS() {
        return "javascript:(function() {"
                + "var urls = [];"
                + "var videos = document.querySelectorAll('video');"
                + "for (var i = 0; i < videos.length; i++) {"
                + "  var v = videos[i];"
                + "  if (v.src && urls.indexOf(v.src) === -1) {"
                + "    urls.push(v.src);"
                + "    EdgeVideoDetector.onVideoDetected(v.src);"
                + "  }"
                + "  var sources = v.querySelectorAll('source');"
                + "  for (var j = 0; j < sources.length; j++) {"
                + "    var src = sources[j].src;"
                + "    if (src && urls.indexOf(src) === -1) {"
                + "      urls.push(src);"
                + "      EdgeVideoDetector.onVideoDetected(src);"
                + "    }"
                + "  }"
                + "}"
                + "var sourceTags = document.querySelectorAll('source[type^=\"video\"]');"
                + "for (var k = 0; k < sourceTags.length; k++) {"
                + "  var src = sourceTags[k].src;"
                + "  if (src && urls.indexOf(src) === -1) {"
                + "    urls.push(src);"
                + "    EdgeVideoDetector.onVideoDetected(src);"
                + "  }"
                + "}"
                + "return urls.length;"
                + "})()";
    }

    public List<String> getVideoUrls() {
        return Collections.unmodifiableList(new ArrayList<>(videoUrls));
    }

    public void downloadVideo(Context context, String url, String filename) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(filename);
        request.setDescription("Downloading video...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null) {
            dm.enqueue(request);
        }
    }

    public void clearDetected() {
        videoUrls.clear();
    }

    public class VideoJsInterface {
        @JavascriptInterface
        public void onVideoDetected(String url) {
            if (url != null && !url.isEmpty() && !videoUrls.contains(url)) {
                videoUrls.add(url);
            }
        }
    }
}