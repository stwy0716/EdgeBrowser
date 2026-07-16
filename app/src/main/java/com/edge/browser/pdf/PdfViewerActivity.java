package com.edge.browser.pdf;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.edge.browser.BrowserLogger;
import com.edge.browser.BrowserLogger.LogCategory;
import com.edge.browser.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "PdfViewerActivity";

    private ImageView pdfPageView;
    private TextView pageIndicator;
    private ImageButton btnPrevPage;
    private ImageButton btnNextPage;

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private int currentPageIndex;
    private int pageCount;
    private ParcelFileDescriptor fileDescriptor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        pdfPageView = new ImageView(this);
        pdfPageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pdfPageView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        android.widget.FrameLayout container = findViewById(R.id.pdf_container);
        if (container != null) {
            container.addView(pdfPageView);
        }

        pageIndicator = findViewById(R.id.page_indicator);
        btnPrevPage = findViewById(R.id.btn_prev_page);
        btnNextPage = findViewById(R.id.btn_next_page);

        btnPrevPage.setOnClickListener(v -> showPage(currentPageIndex - 1));
        btnNextPage.setOnClickListener(v -> showPage(currentPageIndex + 1));

        String filePath = getIntent().getStringExtra("pdf_file_path");
        String downloadUrl = getIntent().getStringExtra("pdf_url");

        if (filePath != null) {
            openPdf(new File(filePath));
        } else if (downloadUrl != null) {
            downloadAndOpenPdf(downloadUrl);
        } else {
            BrowserLogger.getInstance().e(TAG, LogCategory.NAVIGATION, "No PDF path or URL provided");
            Toast.makeText(this, "No PDF file specified", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void openPdf(File file) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);
            pageCount = pdfRenderer.getPageCount();
            currentPageIndex = 0;
            updatePageIndicator();
            updateButtonStates();
            showPage(0);
            BrowserLogger.getInstance().i(TAG, LogCategory.NAVIGATION, "PDF opened: " + file.getName() + " (" + pageCount + " pages)");
        } catch (Exception e) {
            BrowserLogger.getInstance().e(TAG, LogCategory.NAVIGATION, "Failed to open PDF", e);
            Toast.makeText(this, "Failed to open PDF file", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void downloadAndOpenPdf(final String urlString) {
        new Thread(() -> {
            FileOutputStream fos = null;
            InputStream inputStream = null;
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(PdfViewerActivity.this, "Failed to download PDF", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                File tempFile = new File(getCacheDir(), "temp_pdf_" + System.currentTimeMillis() + ".pdf");
                inputStream = connection.getInputStream();
                fos = new FileOutputStream(tempFile);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.flush();

                runOnUiThread(() -> openPdf(tempFile));
            } catch (Exception e) {
                BrowserLogger.getInstance().e(TAG, LogCategory.NAVIGATION, "Failed to download PDF", e);
                runOnUiThread(() -> {
                    Toast.makeText(PdfViewerActivity.this, "Failed to download PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            } finally {
                try { if (fos != null) fos.close(); } catch (Exception ignored) {}
                try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            }
        }).start();
    }

    private void showPage(int index) {
        if (pdfRenderer == null) return;
        if (index < 0 || index >= pageCount) return;

        if (currentPage != null) {
            currentPage.close();
        }

        currentPageIndex = index;
        currentPage = pdfRenderer.openPage(index);

        Bitmap bitmap = Bitmap.createBitmap(
                currentPage.getWidth(),
                currentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        pdfPageView.setImageBitmap(bitmap);
        updatePageIndicator();
        updateButtonStates();
    }

    private void updatePageIndicator() {
        if (pageIndicator != null) {
            pageIndicator.setText((currentPageIndex + 1) + " / " + pageCount);
        }
    }

    private void updateButtonStates() {
        if (btnPrevPage != null) {
            btnPrevPage.setEnabled(currentPageIndex > 0);
        }
        if (btnNextPage != null) {
            btnNextPage.setEnabled(currentPageIndex < pageCount - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentPage != null) {
            try { currentPage.close(); } catch (Exception ignored) {}
            currentPage = null;
        }
        if (pdfRenderer != null) {
            try { pdfRenderer.close(); } catch (Exception ignored) {}
            pdfRenderer = null;
        }
        if (fileDescriptor != null) {
            try { fileDescriptor.close(); } catch (Exception ignored) {}
            fileDescriptor = null;
        }
    }
}