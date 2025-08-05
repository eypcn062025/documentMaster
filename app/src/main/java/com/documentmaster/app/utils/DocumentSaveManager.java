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
        if (isNewDocument) {
            saveDocumentAs(currentFilePath);
            return;
        }
        if (executorService == null || currentFilePath == null || currentFilePath.isEmpty()) {
            showMessage("❌ Kaydetme hatası - dosya yolu bulunamadı");
            return;
        }
        if (callback != null) {
            callback.onSaveStarted();
        }

        webViewBridge.getBestEffortHtml(html -> processSaveResult(html, currentFilePath));
    }

    public void saveDocumentAs(String currentFilePath) {
        View dialogView = ((android.app.Activity) context).getLayoutInflater().inflate(R.layout.dialog_save_as, null);
        EditText editFileName = dialogView.findViewById(R.id.editFileName);

        // Mevcut dosya adını doldur
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
                        performSaveAs(fileName);
                    } else {
                        showMessage("Dosya adı boş olamaz");
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    // ========== PRIVATE METHODS ==========

    private void processSaveResult(String result, String currentFilePath) {
        String htmlContent = HtmlUtils.cleanHtmlResultAdvanced(result);
        Log.d(TAG, "🧹 Temizlenmiş HTML uzunluğu: " +
                (htmlContent != null ? htmlContent.length() : 0));

        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            if (callback != null) {
                callback.onSaveCompleted(false, "❌ İçerik boş - kaydetme iptal edildi");
            }
            return;
        }

        int imageCount = HtmlUtils.countImagesInHtml(htmlContent);
        Log.d(TAG, "🖼️ HTML'de " + imageCount + " resim bulundu");

        executorService.execute(() -> {
            try {
                Log.d(TAG, "💾 DOCX kaydetme işlemi başlıyor...");

                File originalFile = new File(currentFilePath);
                File backupFile = new File(currentFilePath + ".backup");

                // Yedek oluştur
                if (originalFile.exists()) {
                    try {
                        FileUtils.copyFile(originalFile, backupFile);
                        Log.d(TAG, "🔒 Yedek oluşturuldu");
                    } catch (Exception e) {
                        Log.w(TAG, "⚠️ Yedekleme hatası: " + e.getMessage());
                    }
                }

                long startTime = System.currentTimeMillis();
                boolean success = WordDocumentHelper.saveHtmlToDocx(currentFilePath, htmlContent);
                long endTime = System.currentTimeMillis();

                Log.d(TAG, "⏱️ Kaydetme süresi: " + (endTime - startTime) + "ms");

                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (success) {
                        handleSaveSuccess(backupFile, htmlContent, imageCount, currentFilePath);
                    } else {
                        handleSaveFailure(backupFile);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Kaydetme exception: " + e.getMessage(), e);
                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (callback != null) {
                        callback.onSaveCompleted(false, "❌ Kaydetme hatası: " + e.getMessage());
                    }
                });
            }
        });
    }

    private void performSaveAs(String fileName) {
        if (executorService == null) {
            showMessage("Sistem hatası");
            return;
        }

        String finalFileName = fileName;
        if (!finalFileName.endsWith(".docx")) {
            finalFileName += ".docx";
        }

        File documentsDir = new File(context.getFilesDir(), "Documents");
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }

        String newFilePath = new File(documentsDir, finalFileName).getAbsolutePath();
        final String fileNameToSave = finalFileName;

        if (callback != null) {
            callback.onSaveStarted();
        }

        webViewBridge.getHtml(result -> {
            String htmlContent = HtmlUtils.cleanHtmlResult(result);

            executorService.execute(() -> {
                try {
                    boolean success = WordDocumentHelper.saveHtmlToDocx(newFilePath, htmlContent);

                    ((android.app.Activity) context).runOnUiThread(() -> {
                        if (success) {
                            // Başarılı save as
                            if (callback != null) {
                                callback.onSaveCompleted(true, "Belge kaydedildi: " + fileNameToSave);
                                callback.onFilePathChanged(newFilePath, fileNameToSave);
                                callback.onContentChanged(false); // İçerik değişmedi artık
                            }
                        } else {
                            if (callback != null) {
                                callback.onSaveCompleted(false, "Belge kaydedilemedi");
                            }
                        }
                    });
                } catch (Exception e) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        if (callback != null) {
                            callback.onSaveCompleted(false, "Kaydetme hatası: " + e.getMessage());
                        }
                    });
                }
            });
        });
    }

    private void handleSaveSuccess(File backupFile, String htmlContent, int imageCount, String currentFilePath) {
        File savedFile = new File(currentFilePath);

        if (savedFile.exists() && savedFile.length() > 0) {
            String message = "✅ Belge kaydedildi!";
            if (imageCount > 0) {
                message += " (" + imageCount + " resim dahil)";
            }

            Log.d(TAG, "✅ Kaydetme başarılı - Dosya: " + savedFile.length() + " bytes");

            // Yedek dosyayı sil
            if (backupFile.exists()) {
                backupFile.delete();
            }

            if (callback != null) {
                callback.onSaveCompleted(true, message);
                callback.onContentChanged(false); // İçerik değişmedi artık
            }

        } else {
            handleSaveFailure(backupFile);
        }
    }

    private void handleSaveFailure(File backupFile) {
        Log.e(TAG, "❌ Kaydetme başarısız - dosya oluşmadı");

        // Yedekten geri yükle
        if (backupFile.exists()) {
            try {
                File originalFile = new File(backupFile.getParent(),
                        backupFile.getName().replace(".backup", ""));
                FileUtils.copyFile(backupFile, originalFile);
                backupFile.delete();
                Log.d(TAG, "🔄 Yedekten geri yüklendi");
            } catch (Exception e) {
                Log.e(TAG, "❌ Geri yükleme hatası: " + e.getMessage());
            }
        }

        if (callback != null) {
            callback.onSaveCompleted(false, "❌ Belge kaydedilemedi - lütfen tekrar deneyin");
        }
    }
    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}