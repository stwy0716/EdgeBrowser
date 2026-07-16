package com.edge.browser.ui;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.os.Bundle;

import com.edge.browser.BrowserLogger;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceSearchManager {

    private static final String TAG = "VoiceSearchManager";
    private static VoiceSearchManager instance;
    private SpeechRecognizer speechRecognizer;

    public interface VoiceCallback {
        void onResult(String query);
        void onError(String error);
    }

    private VoiceSearchManager() {}

    public static synchronized VoiceSearchManager getInstance() {
        if (instance == null) {
            instance = new VoiceSearchManager();
        }
        return instance;
    }

    public void startVoiceSearch(Activity activity, VoiceCallback callback) {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            if (callback != null) {
                callback.onError("语音识别不可用");
            }
            return;
        }

        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {}

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {}

                @Override
                public void onError(int error) {
                    String errorMsg;
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            errorMsg = "音频错误";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            errorMsg = "客户端错误";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            errorMsg = "权限不足";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            errorMsg = "网络错误";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            errorMsg = "网络超时";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            errorMsg = "未识别到语音";
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            errorMsg = "识别器忙";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                            errorMsg = "服务器错误";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            errorMsg = "语音输入超时";
                            break;
                        default:
                            errorMsg = "未知错误: " + error;
                            break;
                    }
                    BrowserLogger.getInstance().e(TAG, BrowserLogger.LogCategory.SYSTEM, "Speech recognition error: " + errorMsg, null);
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String query = matches.get(0);
                        BrowserLogger.getInstance().i(TAG, BrowserLogger.LogCategory.SYSTEM, "Voice search result: " + query);
                        if (callback != null) {
                            callback.onResult(query);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("未识别到语音");
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "说出搜索内容...");
            speechRecognizer.startListening(intent);

            BrowserLogger.getInstance().d(TAG, BrowserLogger.LogCategory.SYSTEM, "Voice search started");
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, BrowserLogger.LogCategory.SYSTEM, "Failed to start voice search", e);
            if (callback != null) {
                callback.onError("启动语音识别失败: " + e.getMessage());
            }
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }
}