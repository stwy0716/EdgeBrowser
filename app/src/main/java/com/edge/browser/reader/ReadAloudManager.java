package com.edge.browser.reader;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.WebView;
import com.edge.browser.webview.EdgeWebView;

import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class ReadAloudManager {

    private static ReadAloudManager instance;
    private Context context;
    private TextToSpeech textToSpeech;
    private boolean isReading = false;
    private boolean isPaused = false;
    private float speechRate = 1.0f;
    private float pitch = 1.0f;
    private Locale selectedLocale = Locale.CHINESE;
    private ReadAloudListener listener;

    public interface ReadAloudListener {
        void onReadingStarted();
        void onReadingProgress(String utteranceId);
        void onReadingPaused();
        void onReadingResumed();
        void onReadingCompleted();
        void onReadingError(String error);
    }

    private ReadAloudManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized ReadAloudManager getInstance(Context context) {
        if (instance == null) {
            instance = new ReadAloudManager(context);
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context;
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(selectedLocale);
                textToSpeech.setSpeechRate(speechRate);
                textToSpeech.setPitch(pitch);
            }
        });

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (listener != null) listener.onReadingStarted();
            }

            @Override
            public void onDone(String utteranceId) {
                if (listener != null) listener.onReadingCompleted();
            }

            @Override
            public void onError(String utteranceId) {
                if (listener != null) listener.onReadingError("Read aloud error");
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                if (listener != null) listener.onReadingError("Error code: " + errorCode);
            }
        });
    }

    public void setListener(ReadAloudListener listener) {
        this.listener = listener;
    }

    public void readAloud(EdgeWebView webView) {
        if (textToSpeech == null) {
            init(context);
        }

        if (isReading && !isPaused) {
            pause();
            return;
        }

        if (isPaused) {
            resume();
            return;
        }

        // Extract text from page
        webView.evaluateJavascript(
                "(function() {" +
                        "  var article = document.querySelector('article') || document.querySelector('main') || " +
                        "    document.querySelector('[role=main]') || document.body;" +
                        "  return article.innerText;" +
                        "})()",
                value -> {
                    String text = cleanText(value);
                    if (text != null && !text.isEmpty()) {
                        startReading(text);
                    }
                });
    }

    public void readAloud(String text) {
        if (textToSpeech == null) {
            init(context);
        }
        startReading(text);
    }

    public void readSelectedText(String text) {
        if (textToSpeech == null) {
            init(context);
        }
        if (isReading) {
            stop();
        }
        startReading(text);
    }

    private void startReading(String text) {
        isReading = true;
        isPaused = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String utteranceId = UUID.randomUUID().toString();
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UUID.randomUUID().toString());
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        }

        if (listener != null) listener.onReadingStarted();
    }

    public void pause() {
        if (textToSpeech != null && isReading) {
            textToSpeech.stop();
            isPaused = true;
            if (listener != null) listener.onReadingPaused();
        }
    }

    public void resume() {
        if (textToSpeech != null && isPaused) {
            isPaused = false;
            if (listener != null) listener.onReadingResumed();
        }
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            isReading = false;
            isPaused = false;
        }
    }

    public void setSpeechRate(float rate) {
        this.speechRate = rate;
        if (textToSpeech != null) textToSpeech.setSpeechRate(rate);
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        if (textToSpeech != null) textToSpeech.setPitch(pitch);
    }

    public void setLanguage(Locale locale) {
        this.selectedLocale = locale;
        if (textToSpeech != null) textToSpeech.setLanguage(locale);
    }

    public float getSpeechRate() { return speechRate; }
    public float getPitch() { return pitch; }
    public boolean isReading() { return isReading; }
    public boolean isPaused() { return isPaused; }

    private String cleanText(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("^\"|\"$", "")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", " ")
                .replace("\\r", "")
                .trim();
    }

    public void destroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }
}