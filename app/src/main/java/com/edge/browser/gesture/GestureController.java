package com.edge.browser.gesture;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class GestureController {

    public interface GestureCallback {
        void onBackGesture();
        void onForwardGesture();
        void onShowTabsGesture();
        void onRefreshGesture();
        void onScrollToTopGesture();
    }

    private View bottomBar;
    private GestureCallback callback;
    private GestureDetector gestureDetector;
    private boolean attached = false;

    // 触摸起始位置
    private float downX;
    private float downY;
    private long downTime;
    private static final int SWIPE_THRESHOLD = 50;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private static final int DOUBLE_TAP_TIMEOUT = 300;
    private static final float EDGE_WIDTH_RATIO = 0.25f; // 边缘区域宽度占比

    private long lastTapTime = 0;
    private float lastTapX = -1;
    private float lastTapY = -1;

    public void attach(View bottomBar, GestureCallback callback) {
        if (attached) {
            detach();
        }
        this.bottomBar = bottomBar;
        this.callback = callback;

        gestureDetector = new GestureDetector(bottomBar.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                long now = System.currentTimeMillis();
                float x = e.getX();
                float y = e.getY();

                if (lastTapTime > 0 && (now - lastTapTime) < DOUBLE_TAP_TIMEOUT
                        && Math.abs(x - lastTapX) < 50 && Math.abs(y - lastTapY) < 50) {
                    // 双击检测
                    if (callback != null) {
                        callback.onScrollToTopGesture();
                    }
                    lastTapTime = 0;
                    return true;
                }

                lastTapTime = now;
                lastTapX = x;
                lastTapY = y;
                return true;
            }
        });

        bottomBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector != null) {
                    gestureDetector.onTouchEvent(event);
                }

                float barWidth = v.getWidth();
                float barHeight = v.getHeight();
                float edgeWidth = barWidth * EDGE_WIDTH_RATIO;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        downTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_UP:
                        float upX = event.getX();
                        float upY = event.getY();
                        float deltaX = upX - downX;
                        float deltaY = upY - downY;
                        long deltaTime = System.currentTimeMillis() - downTime;

                        if (deltaTime > 1000) break;

                        // 从底部左边缘向右滑动 → 后退
                        if (downX < edgeWidth && deltaX > SWIPE_THRESHOLD
                                && Math.abs(deltaX) > Math.abs(deltaY)) {
                            if (callback != null) {
                                callback.onBackGesture();
                            }
                            return true;
                        }

                        // 从底部右边缘向左滑动 → 前进
                        if (downX > barWidth - edgeWidth && deltaX < -SWIPE_THRESHOLD
                                && Math.abs(deltaX) > Math.abs(deltaY)) {
                            if (callback != null) {
                                callback.onForwardGesture();
                            }
                            return true;
                        }

                        // 从底部中间向上滑动 → 显示标签页
                        if (downX >= edgeWidth && downX <= barWidth - edgeWidth
                                && deltaY < -SWIPE_THRESHOLD
                                && Math.abs(deltaY) > Math.abs(deltaX)) {
                            if (callback != null) {
                                callback.onShowTabsGesture();
                            }
                            return true;
                        }

                        // 从底部中间向下滑动 → 刷新
                        if (downX >= edgeWidth && downX <= barWidth - edgeWidth
                                && deltaY > SWIPE_THRESHOLD
                                && Math.abs(deltaY) > Math.abs(deltaX)) {
                            if (callback != null) {
                                callback.onRefreshGesture();
                            }
                            return true;
                        }
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        break;
                }
                return false;
            }
        });

        attached = true;
    }

    public void detach() {
        if (bottomBar != null) {
            bottomBar.setOnTouchListener(null);
            bottomBar = null;
        }
        gestureDetector = null;
        callback = null;
        attached = false;
    }
}