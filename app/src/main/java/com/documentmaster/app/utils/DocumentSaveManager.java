package com.documentmaster.app.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.documentmaster.app.R;
import com.documentmaster.app.utils.word.WordDocumentHelper;
import com.documentmaster.app.web.WebViewBridge;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.concurrent.ExecutorService;

public class DocumentSaveManager {

    private static final String TAG = "DocumentSaveManager";

    private final Context context;
    private final WebViewBridge webViewBridge;
    private final ExecutorService executorService;
    private final SaveCallback callback;

    public interface SaveCallback {
        void onSaveStarted();
        void onSaveCompleted(boolean success, String message);
        void onContentChanged(boolean isChanged);
        void onFilePathChanged(String newFilePath, String fileName);
    }
    public DocumentSaveManager(Context context, WebViewBridge webViewBridge,
                               ExecutorService executorService, SaveCallback callback) {
        this.context = context;
        this.webViewBridge = webViewBridge;
        this.executorService = executorService;
        this.callback = callback;
    }

    public void saveDocument(String currentFilePath, boolean isNewDocument) {
        if (isNewDocument || currentFilePath == null || currentFilePath.isEmpty()) {
            saveDocumentAs(currentFilePath);
            return;
        }
        notifyStarted();
        webViewBridge.getHtml(html -> {
            Log.d("deneme",html);
            saveHtmlToFile(html, currentFilePath);
        });
    }
    private void saveHtmlToFile(String htmlResult, String filePath) {

        String cleanHtml = HtmlUtils.cleanHtmlResult(htmlResult);
        Log.d("deneme",cleanHtml);
        if (cleanHtml == null || cleanHtml.trim().isEmpty()) {
            notifyResult(false, "❌ İçerik boş - kaydetme iptal edildi");
            return;
        }
        executorService.execute(() -> {
            try {
                boolean success = WordDocumentHelper.saveHtmlToDocx(filePath, cleanHtml);

                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (success) {
                        notifyResult(true, "✅ Belge kaydedildi!");
                        markContentAsUnchanged();
                    } else {
                        notifyResult(false, "❌ Belge kaydedilemedi");
                    }
                });
            } catch (Exception e) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    notifyResult(false, "❌ Kaydetme hatası: " + e.getMessage());
                });
            }
        });
    }
    //-----------------saveDocumentAs------------------------//
    public void saveDocumentAs(String currentFilePath) {
        showSaveAsDialog(currentFilePath);
    }

    private void showSaveAsDialog(String currentFilePath) {
        View dialogView = ((android.app.Activity) context).getLayoutInflater().inflate(R.layout.dialog_save_as, null);
        EditText editFileName = dialogView.findViewById(R.id.editFileName);
        if (currentFilePath != null) {
            File currentFile = new File(currentFilePath);
            String nameWithoutExtension = currentFile.getName().replaceFirst("[.][^.]+$", "");
            editFileName.setText(nameWithoutExtension);
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle("Farklı Kaydet")
                .setView(dialogView)
                .setPositiveButton("Kaydet", (dialog, which) -> {
                    String fileName = editFileName.getText().toString().trim();
                    if (!TextUtils.isEmpty(fileName)) {
                        saveAsNewFile(fileName);
                    } else {
                        showMessage("Dosya adı boş olamaz");
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }
    private void saveAsNewFile(String fileName) {
        if (!fileName.endsWith(".docx")) {
            fileName += ".docx";
        }
        File documentsDir = new File(context.getFilesDir(), "Documents");
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }
        String newFilePath = new File(documentsDir, fileName).getAbsolutePath();
        final String finalFileName = fileName;
        notifyStarted();
        webViewBridge.getHtml(htmlResult -> {
            saveHtmlToFileAs(htmlResult, newFilePath, finalFileName);
        });
    }

    private void saveHtmlToFileAs(String htmlResult, String newFilePath, String fileName) {
        String cleanHtml = HtmlUtils.cleanHtmlResult(htmlResult);
        if (cleanHtml == null || cleanHtml.trim().isEmpty()) {
            notifyResult(false, "❌ İçerik boş - kaydetme iptal edildi");
            return;
        }
        executorService.execute(() -> {
            try {
                boolean success = WordDocumentHelper.saveHtmlToDocx(newFilePath, cleanHtml);

                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (success) {
                        notifyResult(true, "✅ Belge kaydedildi: " + fileName);
                        notifyFilePathChanged(newFilePath, fileName);
                        markContentAsUnchanged();
                    } else {
                        notifyResult(false, "❌ Belge kaydedilemedi");
                    }
                });

            } catch (Exception e) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    notifyResult(false, "❌ Kaydetme hatası: " + e.getMessage());
                });
            }
        });
    }

    private void notifyFilePathChanged(String newFilePath, String fileName) {
        if (callback != null) {
            callback.onFilePathChanged(newFilePath, fileName);
        }
    }

    private void notifyResult(boolean success, String message) {
        if (callback != null) {
            callback.onSaveCompleted(success, message);
        }
    }
    private void markContentAsUnchanged() {
        if (callback != null) {
            callback.onContentChanged(false);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    private void notifyStarted() {
        if (callback != null) {
            callback.onSaveStarted();
        }
    }
}