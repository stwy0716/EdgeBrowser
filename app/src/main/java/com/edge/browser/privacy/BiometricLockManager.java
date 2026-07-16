package com.edge.browser.privacy;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class BiometricLockManager {

    private static BiometricLockManager instance;
    private final SharedPreferences prefs;
    private static final String PREFS_NAME = "edge_biometric_lock";
    private static final String KEY_LOCK_ENABLED = "lock_enabled";

    public interface BiometricCallback {
        void onSuccess();
        void onFailed();
        void onError(String error);
    }

    private BiometricLockManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized BiometricLockManager getInstance(Context context) {
        if (instance == null) {
            instance = new BiometricLockManager(context.getApplicationContext());
        }
        return instance;
    }

    public void authenticate(Activity activity, BiometricCallback callback) {
        if (!(activity instanceof FragmentActivity)) {
            callback.onError("Activity must be FragmentActivity");
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("应用锁")
                .setSubtitle("验证身份以解锁浏览器")
                .setNegativeButtonText("取消")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt((FragmentActivity) activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        callback.onFailed();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        callback.onError(errString.toString());
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    public boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public boolean isLockEnabled() {
        return prefs.getBoolean(KEY_LOCK_ENABLED, false);
    }

    public void setLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply();
    }
}