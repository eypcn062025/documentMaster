package com.documentmaster.app.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.documentmaster.app.utils.word.WordDocumentHelper;
import com.documentmaster.app.web.WebViewBridge;

import java.io.File;
import java.util.concurrent.ExecutorService;

public class DocumentLoadManager {

    private static final String TAG = "DocumentLoadManager";

    private final Context context;
    private final WebViewBridge webViewBridge;
    private final ExecutorService executorService;
    private final LoadCallback callback;

    public interface LoadCallback {
        void onLoadCompleted(boolean success, String content, String fileName, String errorMessage);
    }

    public DocumentLoadManager(Context context, WebViewBridge webViewBridge,
                               ExecutorService executorService, LoadCallback callback) {
        this.context = context;
        this.webViewBridge = webViewBridge;
        this.executorService = executorService;
        this.callback = callback;
    }

    public void loadDocument(String currentFilePath) {
        if (executorService == null || TextUtils.isEmpty(currentFilePath)) {
            Toast.makeText(context, "Dosya yolu hatası", Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onLoadCompleted(false, null, null, "Dosya yolu hatası");
            }
            return;
        }

        executorService.execute(() -> {
            try {
                WordDocumentHelper.WordContent result = WordDocumentHelper.readWordDocument(currentFilePath);
                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (result.isSuccess()) {
                        String htmlContent = result.getContent();
                        Log.d("deneme",htmlContent);
                        File file = new File(currentFilePath);
                        String fileName = file.getName();
                        Toast.makeText(context, "Belge yüklendi", Toast.LENGTH_SHORT).show();
                        if (callback != null) {
                            callback.onLoadCompleted(true, htmlContent, fileName, null);
                        }
                    } else {
                        String errorMsg = "Belge yüklenemedi: " + result.getError();
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();

                        if (callback != null) {
                            callback.onLoadCompleted(false, null, null, errorMsg);
                        }
                    }
                });
            } catch (Exception e) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    String errorMsg = "Yükleme hatası: " + e.getMessage();
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();

                    if (callback != null) {
                        callback.onLoadCompleted(false, null, null, errorMsg);
                    }
                });
            }
        });
    }

    public void setEditorContent(String content, boolean isWebViewLoaded) {
        if (!isWebViewLoaded || content == null) {
            Log.d(TAG, "WebView hazır değil veya içerik null");
            return;
        }
        try {
            String jsCommand = "setHtml('" + content + "')";
            webViewBridge.executeJS(jsCommand);
        } catch (Exception e) {
            Log.e(TAG, "Editöre içerik yükleme hatası: " + e.getMessage());
            webViewBridge.executeJS("setHtml('<p>İçerik yüklenemedi</p>')");
        }
    }
}