package com.edge.browser.security;

import android.content.Context;

import com.edge.browser.BrowserLogger;
import com.edge.browser.data.DatabaseHelper;

public class AntiFingerprintManager {

    private static final String TAG = "AntiFingerprintManager";
    private static final String KEY_ANTI_FINGERPRINT_ENABLED = "anti_fingerprint_enabled";

    private static AntiFingerprintManager instance;
    private final DatabaseHelper db;
    private boolean enabled;

    private AntiFingerprintManager(Context context) {
        this.db = DatabaseHelper.getInstance(context);
        loadState();
    }

    public static synchronized AntiFingerprintManager getInstance(Context context) {
        if (instance == null) {
            instance = new AntiFingerprintManager(context.getApplicationContext());
        }
        return instance;
    }

    public void loadState() {
        loadState(db);
    }

    public void loadState(DatabaseHelper db) {
        String value = db.getSetting(KEY_ANTI_FINGERPRINT_ENABLED, "false");
        enabled = Boolean.parseBoolean(value);
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "Anti-Fingerprint loaded: " + enabled);
    }

    public void saveState() {
        saveState(db);
    }

    public void saveState(DatabaseHelper db) {
        db.setSetting(KEY_ANTI_FINGERPRINT_ENABLED, String.valueOf(enabled));
        BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.PRIVACY,
                "Anti-Fingerprint saved: " + enabled);
    }

    public String getAntiFingerprintJS() {
        if (!enabled) {
            return "";
        }

        return "(function(){\n"
                + "  // Override navigator properties\n"
                + "  Object.defineProperty(navigator, 'platform', {get: function(){return 'Win32';}});\n"
                + "  Object.defineProperty(navigator, 'vendor', {get: function(){return 'Google Inc.';}});\n"
                + "  Object.defineProperty(navigator, 'vendorSub', {get: function(){return '';}});\n"
                + "  Object.defineProperty(navigator, 'productSub', {get: function(){return '20030107';}});\n"
                + "  Object.defineProperty(navigator, 'hardwareConcurrency', {get: function(){return 4;}});\n"
                + "  Object.defineProperty(navigator, 'deviceMemory', {get: function(){return 4;}});\n"
                + "  if (navigator.plugins) {\n"
                + "    Object.defineProperty(navigator, 'plugins', {get: function(){\n"
                + "      var arr = []; arr.item = function(){return null;}; arr.namedItem = function(){return null;};\n"
                + "      arr.refresh = function(){}; return arr;\n"
                + "    }});\n"
                + "  }\n"
                + "  if (navigator.mimeTypes) {\n"
                + "    Object.defineProperty(navigator, 'mimeTypes', {get: function(){\n"
                + "      var arr = []; arr.item = function(){return null;}; arr.namedItem = function(){return null;};\n"
                + "      return arr;\n"
                + "    }});\n"
                + "  }\n"
                + "  // Override canvas fingerprinting\n"
                + "  var origToDataURL = HTMLCanvasElement.prototype.toDataURL;\n"
                + "  HTMLCanvasElement.prototype.toDataURL = function(type) {\n"
                + "    var ctx = this.getContext('2d');\n"
                + "    if (ctx) {\n"
                + "      var imageData = ctx.getImageData(0, 0, this.width, this.height);\n"
                + "      var data = imageData.data;\n"
                + "      for (var i = 0; i < data.length; i += 4) {\n"
                + "        data[i] = data[i] ^ (data[i] % 2);\n"
                + "      }\n"
                + "      ctx.putImageData(imageData, 0, 0);\n"
                + "    }\n"
                + "    return origToDataURL.apply(this, arguments);\n"
                + "  };\n"
                + "  var origToBlob = HTMLCanvasElement.prototype.toBlob;\n"
                + "  HTMLCanvasElement.prototype.toBlob = function(callback, type, quality) {\n"
                + "    var ctx = this.getContext('2d');\n"
                + "    if (ctx) {\n"
                + "      var imageData = ctx.getImageData(0, 0, this.width, this.height);\n"
                + "      var data = imageData.data;\n"
                + "      for (var i = 0; i < data.length; i += 4) {\n"
                + "        data[i] = data[i] ^ (data[i] % 2);\n"
                + "      }\n"
                + "      ctx.putImageData(imageData, 0, 0);\n"
                + "    }\n"
                + "    return origToBlob.apply(this, arguments);\n"
                + "  };\n"
                + "  // Override WebGL fingerprinting\n"
                + "  var origGetParameter = WebGLRenderingContext.prototype.getParameter;\n"
                + "  WebGLRenderingContext.prototype.getParameter = function(param) {\n"
                + "    if (param === 37445) { return 'Intel Inc.'; }\n"
                + "    if (param === 37446) { return 'Intel Iris OpenGL Engine'; }\n"
                + "    return origGetParameter.call(this, param);\n"
                + "  };\n"
                + "  var origGetSupportedExtensions = WebGLRenderingContext.prototype.getSupportedExtensions;\n"
                + "  WebGLRenderingContext.prototype.getSupportedExtensions = function() {\n"
                + "    var exts = origGetSupportedExtensions.call(this);\n"
                + "    if (exts && exts.indexOf('WEBGL_debug_renderer_info') !== -1) {\n"
                + "      exts = exts.filter(function(e){return e !== 'WEBGL_debug_renderer_info';});\n"
                + "    }\n"
                + "    return exts;\n"
                + "  };\n"
                + "  // Override audio fingerprinting\n"
                + "  var origCreateAnalyser = AudioContext.prototype.createAnalyser;\n"
                + "  AudioContext.prototype.createAnalyser = function() {\n"
                + "    var analyser = origCreateAnalyser.call(this);\n"
                + "    var origGetFloatFrequencyData = analyser.getFloatFrequencyData;\n"
                + "    analyser.getFloatFrequencyData = function(array) {\n"
                + "      origGetFloatFrequencyData.call(this, array);\n"
                + "      for (var i = 0; i < array.length; i++) {\n"
                + "        array[i] = array[i] + (Math.random() * 0.0001);\n"
                + "      }\n"
                + "    };\n"
                + "    return analyser;\n"
                + "  };\n"
                + "})();";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveState();
    }
}