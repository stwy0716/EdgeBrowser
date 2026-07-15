package com.edge.browser.translate;

import android.net.Uri;

public class TranslationManager {

    private static final String PREF_TRANSLATE_LANG = "translate_lang";
    private static final String DEFAULT_LANG = "zh-CN";

    private static TranslationManager instance;
    private String targetLang = DEFAULT_LANG;

    private TranslationManager() {}

    public static synchronized TranslationManager getInstance() {
        if (instance == null) instance = new TranslationManager();
        return instance;
    }

    public void setTargetLanguage(String lang) {
        this.targetLang = lang;
    }

    public String getTargetLanguage() {
        return targetLang;
    }

    public String getTranslateUrl(String originalUrl) {
        return "https://translate.google.com/translate?hl=" + targetLang
                + "&sl=auto&tl=" + targetLang + "&u=" + Uri.encode(originalUrl);
    }

    public String getTranslateUrl(String text, String sourceLang) {
        return "https://translate.google.com/?sl=" + sourceLang
                + "&tl=" + targetLang + "&text=" + Uri.encode(text);
    }
}