package com.documentmaster.app.web;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.documentmaster.app.R;

import java.io.InputStream;

public class WebViewManager {

    private final Context context;
    private final WebView webView;

    public WebViewManager(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
    }

    public void setup(Runnable onPageLoaded) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        webView.addJavascriptInterface(new WebAppInterface((WebAppCallback) context), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (onPageLoaded != null) {
                    onPageLoaded.run();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        loadLocalHtml();
    }
    private void loadLocalHtml() {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.richeditor);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            String htmlContent = new String(buffer);
            webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null);
        } catch (Exception e) {
            Log.e("WebViewManager", "HTML yükleme hatası: " + e.getMessage());
            Toast.makeText(context, "Editör yüklenemedi", Toast.LENGTH_SHORT).show();
        }
    }
}