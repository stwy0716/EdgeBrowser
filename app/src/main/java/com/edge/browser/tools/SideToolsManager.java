package com.edge.browser.tools;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

public class SideToolsManager {

    private static SideToolsManager instance;
    private final Map<String, ToolAction> toolActions;

    public interface ToolAction {
        void execute(Context context, String... params);
    }

    private SideToolsManager() {
        toolActions = new HashMap<>();
        initTools();
    }

    public static synchronized SideToolsManager getInstance() {
        if (instance == null) {
            instance = new SideToolsManager();
        }
        return instance;
    }

    private void initTools() {
        // Calculator
        toolActions.put("calculator", (context, params) -> {
            // Show built-in calculator panel
        });

        // Unit converter
        toolActions.put("converter", (context, params) -> {
            // Show unit converter panel
        });

        // Currency converter
        toolActions.put("currency", (context, params) -> {
            // Show currency converter with live rates
        });

        // Translator
        toolActions.put("translator", (context, params) -> {
            String text = params.length > 0 ? params[0] : "";
            String url = "https://translate.google.com/?sl=auto&tl=zh-CN&text=" + Uri.encode(text);
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        // Todo list
        toolActions.put("todo", (context, params) -> {
            // Show built-in todo list panel
        });

        // Word/Excel preview
        toolActions.put("office_preview", (context, params) -> {
            if (params.length > 0) {
                String url = "https://view.officeapps.live.com/op/view.aspx?src=" + Uri.encode(params[0]);
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        // Screenshot
        toolActions.put("screenshot", (context, params) -> {
            // Trigger screenshot
        });

        // Web search
        toolActions.put("search", (context, params) -> {
            String query = params.length > 0 ? params[0] : "";
            String url = "https://www.bing.com/search?q=" + Uri.encode(query);
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        // Shopping comparison
        toolActions.put("shopping", (context, params) -> {
            String query = params.length > 0 ? params[0] : "";
            String url = "https://www.bing.com/shop?q=" + Uri.encode(query);
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        // Email
        toolActions.put("email", (context, params) -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            context.startActivity(intent);
        });

        // QR code
        toolActions.put("qrcode", (context, params) -> {
            String url = params.length > 0 ? params[0] : "";
            // Generate and show QR code
        });
    }

    public void executeTool(String toolId, Context context, String... params) {
        ToolAction action = toolActions.get(toolId);
        if (action != null) {
            action.execute(context, params);
        }
    }

    public void registerTool(String id, ToolAction action) {
        toolActions.put(id, action);
    }

    public boolean hasTool(String toolId) {
        return toolActions.containsKey(toolId);
    }
}