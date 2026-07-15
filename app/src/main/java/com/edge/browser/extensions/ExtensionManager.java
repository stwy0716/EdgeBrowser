package com.edge.browser.extensions;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.edge.browser.data.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class ExtensionManager {

    private static ExtensionManager instance;
    private final DatabaseHelper db;

    private ExtensionManager(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static synchronized ExtensionManager getInstance(Context context) {
        if (instance == null) {
            instance = new ExtensionManager(context.getApplicationContext());
        }
        return instance;
    }

    public void installExtension(String id, String name, String desc, String version,
                                  String js, String css) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_EXT_ID, id);
        cv.put(DatabaseHelper.COL_EXT_NAME, name);
        cv.put(DatabaseHelper.COL_EXT_DESC, desc);
        cv.put(DatabaseHelper.COL_EXT_VERSION, version);
        cv.put(DatabaseHelper.COL_EXT_JS, js);
        cv.put(DatabaseHelper.COL_EXT_CSS, css);
        cv.put(DatabaseHelper.COL_EXT_ENABLED, 1);
        cv.put(DatabaseHelper.COL_EXT_INSTALLED_AT, System.currentTimeMillis());
        db.getWritableDatabase().insertWithOnConflict(DatabaseHelper.TABLE_EXTENSIONS, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void uninstallExtension(String id) {
        db.getWritableDatabase().delete(DatabaseHelper.TABLE_EXTENSIONS,
                DatabaseHelper.COL_EXT_ID + "=?", new String[]{id});
    }

    public void setEnabled(String id, boolean enabled) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_EXT_ENABLED, enabled ? 1 : 0);
        db.getWritableDatabase().update(DatabaseHelper.TABLE_EXTENSIONS, cv,
                DatabaseHelper.COL_EXT_ID + "=?", new String[]{id});
    }

    public List<ExtensionInfo> getExtensions() {
        List<ExtensionInfo> list = new ArrayList<>();
        Cursor c = db.getReadableDatabase().query(DatabaseHelper.TABLE_EXTENSIONS,
                null, null, null, null, null, DatabaseHelper.COL_EXT_INSTALLED_AT + " DESC");
        try {
            while (c.moveToNext()) {
                list.add(cursorToExtension(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public List<ExtensionInfo> getEnabledExtensions() {
        List<ExtensionInfo> list = new ArrayList<>();
        Cursor c = db.getReadableDatabase().query(DatabaseHelper.TABLE_EXTENSIONS,
                null, DatabaseHelper.COL_EXT_ENABLED + "=1",
                null, null, null, DatabaseHelper.COL_EXT_INSTALLED_AT + " DESC");
        try {
            while (c.moveToNext()) {
                list.add(cursorToExtension(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public String getExtensionJS(String url) {
        StringBuilder sb = new StringBuilder();
        for (ExtensionInfo ext : getEnabledExtensions()) {
            if (ext.jsContent != null && !ext.jsContent.isEmpty()) {
                sb.append("/* Extension: ").append(ext.name).append(" */\n");
                sb.append(ext.jsContent).append("\n");
            }
        }
        return sb.toString();
    }

    public String getExtensionCSS(String url) {
        StringBuilder sb = new StringBuilder();
        for (ExtensionInfo ext : getEnabledExtensions()) {
            if (ext.cssContent != null && !ext.cssContent.isEmpty()) {
                sb.append("/* Extension: ").append(ext.name).append(" */\n");
                sb.append(ext.cssContent).append("\n");
            }
        }
        return sb.toString();
    }

    public void preloadBuiltinExtensions() {
        // 1. Dark Reader - 暗色模式扩展
        installExtension("dark-reader", "Dark Reader", "暗色模式，保护眼睛",
                "1.0",
                "/* Dark Reader JS */\n" +
                "(function(){if(window.__darkReaderInstalled)return;window.__darkReaderInstalled=true;" +
                "function applyDark(){var s=document.createElement('style');s.id='dark-reader-style';" +
                "s.textContent='html{filter:invert(0.85) hue-rotate(180deg) brightness(1.05) contrast(0.95)}" +
                "img,video,canvas,iframe,[style*=\"background-image\"]{filter:invert(1) hue-rotate(180deg)}';" +
                "document.head.appendChild(s);}" +
                "if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',applyDark);" +
                "else applyDark();})();",
                "html { filter: invert(0.85) hue-rotate(180deg) brightness(1.05) contrast(0.95); }\n" +
                "img, video, canvas, iframe, [style*=\"background-image\"] { filter: invert(1) hue-rotate(180deg); }");

        // 2. AdGuard Base - 增强广告拦截
        installExtension("adguard-base", "AdGuard Base", "增强广告拦截，移除常见广告元素",
                "1.0",
                "/* AdGuard Base JS */\n" +
                "(function(){if(window.__adguardBaseInstalled)return;window.__adguardBaseInstalled=true;" +
                "var selectors=['.ad','.ads','.advertisement','[id*=\"ad-\"]','[class*=\"ad-\"]'," +
                "'[id*=\"banner\"]','[class*=\"banner\"]','.sponsored','.promo','.popup-ad'," +
                "'[aria-label*=\"广告\"]','[aria-label*=\"advertisement\"]'];" +
                "function removeAds(){selectors.forEach(function(s){try{var els=document.querySelectorAll(s);" +
                "for(var i=0;i<els.length;i++){els[i].style.display='none';}}catch(e){}});}" +
                "if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',removeAds);" +
                "else removeAds();" +
                "var observer=new MutationObserver(function(){removeAds();});" +
                "if(document.body)observer.observe(document.body,{childList:true,subtree:true});" +
                "else document.addEventListener('DOMContentLoaded',function(){observer.observe(document.body,{childList:true,subtree:true});});" +
                "})();",
                ".ad, .ads, .advertisement, [id*=\"ad-\"], [class*=\"ad-\"], [id*=\"banner\"], [class*=\"banner\"], " +
                ".sponsored, .promo, .popup-ad { display: none !important; }");

        // 3. Translate Helper - 翻译辅助
        installExtension("translate-helper", "Translate Helper", "翻译辅助，添加翻译按钮",
                "1.0",
                "/* Translate Helper JS */\n" +
                "(function(){if(window.__translateHelperInstalled)return;window.__translateHelperInstalled=true;" +
                "function addTranslateBtn(){" +
                "var btn=document.createElement('div');" +
                "btn.id='translate-helper-btn';" +
                "btn.textContent='翻译';" +
                "btn.style.cssText='position:fixed;bottom:80px;right:20px;z-index:99999;" +
                "background:#0078D4;color:#fff;padding:10px 16px;border-radius:8px;cursor:pointer;" +
                "font-size:14px;font-family:sans-serif;box-shadow:0 2px 8px rgba(0,0,0,0.3);';" +
                "btn.onclick=function(){" +
                "var text=window.getSelection().toString();" +
                "if(!text)text=document.body.innerText.substring(0,500);" +
                "var url='https://translate.google.com/?sl=auto&tl=zh-CN&text='+encodeURIComponent(text);" +
                "window.open(url,'_blank');};" +
                "document.body.appendChild(btn);}" +
                "if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',addTranslateBtn);" +
                "else addTranslateBtn();})();",
                "#translate-helper-btn { transition: opacity 0.3s; }\n" +
                "#translate-helper-btn:hover { opacity: 0.9; background: #106EBE; }");
    }

    private ExtensionInfo cursorToExtension(Cursor c) {
        ExtensionInfo ext = new ExtensionInfo();
        ext.id = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EXT_ID));
        ext.name = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EXT_NAME));
        ext.description = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EXT_DESC));
        ext.version = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EXT_VERSION));
        ext.jsContent = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EXT_JS));
        ext.cssContent = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EXT_CSS));
        ext.enabled = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_EXT_ENABLED)) == 1;
        ext.installedAt = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_EXT_INSTALLED_AT));
        return ext;
    }
}