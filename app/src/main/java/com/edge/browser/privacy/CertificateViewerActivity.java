package com.edge.browser.privacy;
import com.edge.browser.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.SSLPeerUnverifiedException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CertificateViewerActivity extends AppCompatActivity {

    private static final String EXTRA_URL = "cert_url";

    public static void showCertificate(Activity activity, String url) {
        Intent intent = new Intent(activity, CertificateViewerActivity.class);
        intent.putExtra(EXTRA_URL, url);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("证书信息");
        }

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url != null) {
            loadCertificate(url);
        } else {
            Toast.makeText(this, "URL 无效", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadCertificate(String url) {
        new Thread(() -> {
            try {
                String host = extractHost(url);
                OkHttpClient client = new OkHttpClient.Builder()
                        .hostnameVerifier((hostname, session) -> true)
                        .build();

                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                java.security.cert.Certificate[] certs = null;
                try {
                    certs = response.handshake().peerCertificates().toArray(new java.security.cert.Certificate[0]);
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "无法获取证书信息", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    response.close();
                    return;
                }
                response.close();

                if (certs == null || certs.length == 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "未找到证书", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                X509Certificate cert = (X509Certificate) certs[0];
                StringBuilder sb = new StringBuilder();

                sb.append("域名: ").append(host).append("\n\n");

                sb.append("Subject: ").append(cert.getSubjectDN().getName()).append("\n\n");
                sb.append("Issuer: ").append(cert.getIssuerDN().getName()).append("\n\n");

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                sb.append("Valid From: ").append(sdf.format(cert.getNotBefore())).append("\n");
                sb.append("Valid To: ").append(sdf.format(cert.getNotAfter())).append("\n\n");

                sb.append("Serial Number: ").append(formatSerial(cert.getSerialNumber())).append("\n\n");
                sb.append("Sig Algorithm: ").append(cert.getSigAlgName()).append("\n\n");
                sb.append("Public Key Algorithm: ").append(cert.getPublicKey().getAlgorithm()).append("\n\n");

                String fingerprint = getSHA256Fingerprint(cert);
                sb.append("SHA-256 Fingerprint:\n").append(fingerprint).append("\n\n");

                if (certs.length > 1) {
                    sb.append("--- 证书链 (共 ").append(certs.length).append(" 个) ---\n");
                    for (int i = 1; i < certs.length; i++) {
                        X509Certificate chainCert = (X509Certificate) certs[i];
                        sb.append("\n[").append(i + 1).append("] ").append(chainCert.getSubjectDN().getName());
                    }
                }

                final String certInfo = sb.toString();
                runOnUiThread(() -> displayCertificateInfo(certInfo));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "获取证书失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void displayCertificateInfo(String certInfo) {
        LinearLayout container = findViewById(R.id.cert_info_container);
        if (container == null) return;

        String[] sections = certInfo.split("\n\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            String[] lines = section.split("\n");
            if (lines.length == 0) continue;

            for (String line : lines) {
                TextView tv = new TextView(this);
                tv.setText(line);
                tv.setTextSize(14);
                tv.setTextColor(getResources().getColor(android.R.color.tab_indicator_text, null));
                tv.setPadding(0, 4, 0, 4);

                if (line.startsWith("域名:") || line.startsWith("Subject:") || line.startsWith("Issuer:")
                        || line.startsWith("Sig Algorithm:") || line.startsWith("Public Key Algorithm:")
                        || line.startsWith("SHA-256") || line.startsWith("Serial Number:")
                        || line.startsWith("Valid From:") || line.startsWith("Valid To:")) {
                    tv.setTypeface(null, Typeface.BOLD);
                }

                if (line.length() > 60 && !line.contains(":") && !line.startsWith("---")) {
                    tv.setTextSize(12);
                    tv.setTypeface(null, Typeface.NORMAL);
                }

                container.addView(tv);
            }

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, null));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) divider.getLayoutParams();
            params.setMargins(0, 8, 0, 8);
            divider.setLayoutParams(params);
            container.addView(divider);
        }
    }

    private String extractHost(String url) {
        try {
            if (url.startsWith("https://") || url.startsWith("http://")) {
                int start = url.indexOf("://") + 3;
                int end = url.indexOf('/', start);
                return end > 0 ? url.substring(start, end) : url.substring(start);
            }
            return url;
        } catch (Exception e) {
            return url;
        }
    }

    private String formatSerial(java.math.BigInteger serial) {
        String hex = serial.toString(16);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            if (i > 0) sb.append(":");
            sb.append(hex.substring(i, Math.min(i + 2, hex.length())));
        }
        return sb.toString().toUpperCase();
    }

    private String getSHA256Fingerprint(X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(cert.getEncoded());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                if (i > 0 && i % 16 == 0) sb.append("\n");
                sb.append(String.format("%02X", digest[i]));
                if (i < digest.length - 1 && (i + 1) % 16 != 0) sb.append(":");
            }
            return sb.toString();
        } catch (Exception e) {
            return "N/A";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}