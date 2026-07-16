package com.edge.browser.content;

import com.edge.browser.translate.TranslationManager;
import com.edge.browser.webview.EdgeWebView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLEncoder;

public class InlineTranslationManager extends TranslationManager {

    private static InlineTranslationManager instance;

    private InlineTranslationManager() {}

    public static synchronized InlineTranslationManager getInstance() {
        if (instance == null) {
            instance = new InlineTranslationManager();
        }
        return instance;
    }

    public void translatePage(EdgeWebView webView, String targetLanguage) {
        if (webView == null) return;
        String js = buildTranslationJS(targetLanguage);
        webView.injectJavaScript(js);
    }

    public String translateText(String text, String sourceLang, String targetLang) throws IOException {
        if (text == null || text.isEmpty()) return "";
        String url = "https://translate.google.com/m?sl=" + sourceLang
                + "&tl=" + targetLang + "&q=" + URLEncoder.encode(text, "UTF-8");
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .get();
        Element resultDiv = doc.selectFirst("div.result-container");
        if (resultDiv != null) {
            return resultDiv.text();
        }
        return text;
    }

    private String buildTranslationJS(String targetLang) {
        return "(function(){" +
            "var lang='" + targetLang + "';" +
            "var texts=document.querySelectorAll('p,h1,h2,h3,h4,h5,h6,span,li,td,th,div:not(:has(div))');" +
            "var count=0;" +
            "var maxChars=0;" +
            "texts.forEach(function(el){" +
                "maxChars+=el.textContent.length;" +
            "});" +
            "if(maxChars>50000){" +
                "alert('页面内容过多，建议使用Google翻译完整页面');" +
                "return;" +
            "}" +
            "var queue=[];" +
            "texts.forEach(function(el){" +
                "var text=el.textContent.trim();" +
                "if(text.length>5&&text.length<500){" +
                    "queue.push({el:el,text:text});" +
                "}" +
            "});" +
            "var idx=0;" +
            "function processNext(){" +
                "if(idx>=queue.length){" +
                    "var badge=document.createElement('div');" +
                    "badge.style.cssText='position:fixed;top:10px;right:10px;background:#0078D4;color:#fff;padding:8px 16px;border-radius:20px;font-size:14px;z-index:999999;font-family:sans-serif;';" +
                    "badge.textContent='已翻译为" + getLanguageName(targetLang) + "';" +
                    "document.body.appendChild(badge);" +
                    "setTimeout(function(){badge.remove();},3000);" +
                    "return;" +
                "}" +
                "var item=queue[idx];" +
                "var url='https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl='+lang+'&dt=t&q='+encodeURIComponent(item.text);" +
                "fetch(url).then(function(r){return r.json();}).then(function(data){" +
                    "if(data&&data[0]&&data[0][0]&&data[0][0][0]){" +
                        "item.el.textContent=data[0][0][0];" +
                    "}" +
                    "idx++;" +
                    "setTimeout(processNext,200);" +
                "}).catch(function(){" +
                    "idx++;" +
                    "setTimeout(processNext,200);" +
                "});" +
            "}" +
            "processNext();" +
            "})()";
    }

    private String getLanguageName(String code) {
        switch (code) {
            case "zh-CN": return "中文";
            case "en": return "English";
            case "ja": return "日本語";
            case "ko": return "한국어";
            case "fr": return "Français";
            case "de": return "Deutsch";
            case "es": return "Español";
            case "ru": return "Русский";
            case "pt": return "Português";
            case "ar": return "العربية";
            default: return code;
        }
    }
}