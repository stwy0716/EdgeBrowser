package com.edge.browser.devtools;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edge.browser.BrowserLogger;
import com.edge.browser.R;
import com.edge.browser.webview.EdgeWebView;
import com.edge.browser.webview.GeckoWebView;

import java.util.List;

public class DevToolsActivity extends AppCompatActivity {

    private TextView tvPageSource;
    private TextView tvConsoleOutput;
    private TextView tvCurrentUA;
    private TextView tvResourceInfo;
    private ScrollView sourceScroll;
    private ScrollView consoleScroll;
    private DevToolsManager devToolsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devtools);

        devToolsManager = DevToolsManager.getInstance();

        tvPageSource = findViewById(R.id.tv_page_source);
        tvConsoleOutput = findViewById(R.id.tv_console_output);
        tvCurrentUA = findViewById(R.id.tv_current_ua);
        tvResourceInfo = findViewById(R.id.tv_resource_info);
        sourceScroll = findViewById(R.id.source_scroll);
        consoleScroll = findViewById(R.id.console_scroll);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Update current UA display
        tvCurrentUA.setText("当前UA: " + devToolsManager.getUserAgent());

        // View page source
        findViewById(R.id.btn_view_source).setOnClickListener(v -> {
            EdgeWebView webView = getCurrentWebView();
            if (webView == null) {
                Toast.makeText(this, "没有活动的WebView", Toast.LENGTH_SHORT).show();
                return;
            }
            String source = devToolsManager.getPageSource(webView);
            tvPageSource.setText(source);
            sourceScroll.post(() -> sourceScroll.fullScroll(View.FOCUS_UP));
            Toast.makeText(this, "源代码已获取 (" + source.length() + " 字符)", Toast.LENGTH_SHORT).show();
        });

        // Inject console capture
        findViewById(R.id.btn_capture_console).setOnClickListener(v -> {
            EdgeWebView webView = getCurrentWebView();
            if (webView == null) {
                Toast.makeText(this, "没有活动的WebView", Toast.LENGTH_SHORT).show();
                return;
            }
            devToolsManager.injectConsoleCapture(webView);
            Toast.makeText(this, "控制台捕获已注入", Toast.LENGTH_SHORT).show();
        });

        // Refresh console output
        findViewById(R.id.btn_refresh_console).setOnClickListener(v -> {
            EdgeWebView webView = getCurrentWebView();
            if (webView == null) {
                Toast.makeText(this, "没有活动的WebView", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> messages = devToolsManager.getConsoleMessages(webView);
            if (messages.isEmpty()) {
                tvConsoleOutput.setText("(无控制台输出)");
            } else {
                StringBuilder sb = new StringBuilder();
                for (String msg : messages) {
                    sb.append(msg).append("\n");
                }
                tvConsoleOutput.setText(sb.toString());
            }
            consoleScroll.post(() -> consoleScroll.fullScroll(View.FOCUS_DOWN));
        });

        // Clear console
        findViewById(R.id.btn_clear_console).setOnClickListener(v -> {
            tvConsoleOutput.setText("控制台输出将显示在这里");
        });

        // UA switcher buttons
        findViewById(R.id.btn_ua_android).setOnClickListener(v -> {
            EdgeWebView webView = getCurrentWebView();
            if (webView != null) {
                webView.getSettings().setUserAgentString(null);
                tvCurrentUA.setText("当前UA: Android (默认)");
                webView.reload();
                Toast.makeText(this, "已切换到 Android UA", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_ua_desktop).setOnClickListener(v -> {
            setUA(DevToolsManager.UA_VALUES[1], "Desktop (Windows)");
        });

        findViewById(R.id.btn_ua_macos).setOnClickListener(v -> {
            setUA(DevToolsManager.UA_VALUES[2], "Desktop (macOS)");
        });

        findViewById(R.id.btn_ua_iphone).setOnClickListener(v -> {
            setUA(DevToolsManager.UA_VALUES[3], "iPhone");
        });

        findViewById(R.id.btn_ua_ipad).setOnClickListener(v -> {
            setUA(DevToolsManager.UA_VALUES[4], "iPad");
        });

        findViewById(R.id.btn_ua_custom).setOnClickListener(v -> {
            showCustomUADialog();
        });

        // Resource info
        findViewById(R.id.btn_resource_info).setOnClickListener(v -> {
            EdgeWebView webView = getCurrentWebView();
            if (webView == null) {
                Toast.makeText(this, "没有活动的WebView", Toast.LENGTH_SHORT).show();
                return;
            }
            String info = devToolsManager.getResourceInfo(webView);
            tvResourceInfo.setText(info);
        });
    }

    private void setUA(String ua, String label) {
        EdgeWebView webView = getCurrentWebView();
        if (webView != null) {
            devToolsManager.setUserAgent(webView, ua);
            tvCurrentUA.setText("当前UA: " + label);
            webView.reload();
            Toast.makeText(this, "已切换到 " + label, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "没有活动的WebView", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCustomUADialog() {
        EditText input = new EditText(this);
        input.setHint("输入自定义 User Agent");
        input.setPadding(32, 24, 32, 24);
        input.setTextSize(14);

        new AlertDialog.Builder(this)
                .setTitle("自定义 User Agent")
                .setView(input)
                .setPositiveButton("应用", (d, which) -> {
                    String ua = input.getText().toString().trim();
                    if (!ua.isEmpty()) {
                        EdgeWebView webView = getCurrentWebView();
                        if (webView != null) {
                            devToolsManager.setUserAgent(webView, ua);
                            tvCurrentUA.setText("当前UA: 自定义");
                            webView.reload();
                            Toast.makeText(this, "已应用自定义UA", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private EdgeWebView getCurrentWebView() {
        try {
            com.edge.browser.MainActivity activity = com.edge.browser.EdgeApplication.getMainActivity();
            if (activity != null) {
                com.edge.browser.webview.IBrowserView wv = activity.getCurrentWebView();
                if (wv != null && wv.isWebViewBased()) {
                    return (EdgeWebView) wv.getWebView();
                }
            }
        } catch (Exception e) {
            BrowserLogger.getInstance().e("DevToolsActivity", BrowserLogger.LogCategory.SYSTEM,
                    "Failed to get WebView", e);
        }
        return null;
    }
}