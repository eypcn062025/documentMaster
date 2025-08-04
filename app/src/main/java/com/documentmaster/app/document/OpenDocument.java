package com.documentmaster.app.document;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.documentmaster.app.Document;
import com.documentmaster.app.activities.WordEditorActivity;
import com.documentmaster.app.utils.FileManagerHelper;
import com.documentmaster.app.utils.WordDocumentHelper;

import java.io.File;

public class OpenDocument {

    private static final String TAG = "OpenDocument";
    private static final int PICK_FILE_REQUEST = 1002;

    private final Context context;
    private final OpenDocumentCallback callback;

    public interface OpenDocumentCallback {
        void onDocumentOpened(String filePath);
        void showProgressDialog(String message);
        void hideProgressDialog();
    }

    public OpenDocument(Context context, OpenDocumentCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            String[] mimeTypes = {
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/msword",
                    "application/pdf",
                    "text/plain",
                    "text/html",
                    "*/*"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

            ((Activity) context).startActivityForResult(Intent.createChooser(intent, "Word Belgesi Seç"), PICK_FILE_REQUEST);

        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Dosya seçici bulunamadı: " + e.getMessage());
            try {
                Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
                fallbackIntent.setType("*/*");
                fallbackIntent.addCategory(Intent.CATEGORY_OPENABLE);
                ((Activity) context).startActivityForResult(fallbackIntent, PICK_FILE_REQUEST);
            } catch (Exception ex) {
                Toast.makeText(context, "Dosya seçici açılamadı. Lütfen bir dosya yöneticisi uygulaması yükleyin.", Toast.LENGTH_LONG).show();
            }
        }
    }
    public void processSelectedFile(Uri fileUri) {
        try {
            String fileName = FileManagerHelper.getFileNameFromUri(context, fileUri);
            Log.d(TAG, "Dosya adı: " + fileName);

            long fileSize = FileManagerHelper.getFileSizeFromUri(context, fileUri);
            Log.d(TAG, "Dosya boyutu: " + fileSize + " bytes");

            if (fileSize > 50 * 1024 * 1024) {
                Toast.makeText(context, "Dosya çok büyük. Maksimum 50MB desteklenir.", Toast.LENGTH_LONG).show();
                return;
            }

            if (fileName != null && !isSupportedFile(fileName)) {
                Toast.makeText(context, "Desteklenmeyen dosya formatı: " + fileName, Toast.LENGTH_LONG).show();
                return;
            }

            if (callback != null) {
                callback.showProgressDialog("Dosya açılıyor...");
            }

            new Thread(() -> {
                try {
                    String filePath = null;

                    filePath = FileManagerHelper.getRealPathFromURI(context, fileUri);

                    if (filePath == null || !new File(filePath).exists()) {
                        Log.d(TAG, "Gerçek yol alınamadı, temp'e kopyalanıyor...");
                        filePath = FileManagerHelper.copyFileFromUri(context, fileUri);
                    }

                    if (filePath != null && new File(filePath).exists()) {
                        final String finalPath = filePath;

                        ((Activity) context).runOnUiThread(() -> {
                            if (callback != null) {
                                callback.hideProgressDialog();
                            }
                            openDocumentFromPath(finalPath, fileName);
                        });
                    } else {
                        ((Activity) context).runOnUiThread(() -> {
                            if (callback != null) {
                                callback.hideProgressDialog();
                            }
                            Toast.makeText(context, "Dosya erişimi başarısız", Toast.LENGTH_LONG).show();
                        });
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Dosya işleme hatası: " + e.getMessage());
                    ((Activity) context).runOnUiThread(() -> {
                        if (callback != null) {
                            callback.hideProgressDialog();
                        }
                        Toast.makeText(context, "Dosya açma hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Dosya seçim işleme hatası: " + e.getMessage());
            Toast.makeText(context, "Dosya işleme hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void openDocumentFromPath(String filePath, String displayName) {
        try {
            Log.d(TAG, "🚀 Belge açma başlıyor...");

            String fileName = displayName != null ? displayName : new File(filePath).getName();
            String lowerFileName = fileName.toLowerCase();

            if (lowerFileName.endsWith(".docx") || lowerFileName.endsWith(".doc") ||
                    lowerFileName.endsWith(".html") || lowerFileName.endsWith(".htm") ||
                    lowerFileName.endsWith(".txt")) {

                if (WordDocumentHelper.isValidWordDocument(filePath)) {
                    Log.d(TAG, "✅ Geçerli belge, editör açılıyor...");

                    Intent intent = new Intent(context, WordEditorActivity.class);
                    intent.putExtra(WordEditorActivity.EXTRA_FILE_PATH, filePath);
                    intent.putExtra(WordEditorActivity.EXTRA_IS_NEW_DOCUMENT, false);
                    intent.putExtra("DISPLAY_NAME", displayName);
                    context.startActivity(intent);

                    if (callback != null) {
                        callback.onDocumentOpened(filePath);
                    }

                } else {
                    Log.w(TAG, "❌ Geçersiz belge: " + filePath);
                    Toast.makeText(context, "❌ Belge açılamadı - dosya bozulmuş veya desteklenmiyor", Toast.LENGTH_LONG).show();

                }
            } else {
                Log.w(TAG, "⚠️ Desteklenmeyen format: " + fileName);
                Toast.makeText(context, "⚠️ Bu dosya türü için editör geliştiriliyor: " + fileName, Toast.LENGTH_SHORT).show();

            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Belge açma hatası: " + e.getMessage(), e);
            Toast.makeText(context, "❌ Belge açma hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void openSelectedFile(Uri fileUri) {
        try {
            String filePath = fileUri.getPath();
            if (filePath != null) {
                Document document = new Document(filePath);
                openDocumentFromPath(filePath, document.getName());
            } else {
                Toast.makeText(context, "Dosya yolu alınamadı", Toast.LENGTH_SHORT).show();

            }
        } catch (Exception e) {
            Log.e(TAG, "Dosya açma hatası: " + e.getMessage());
            Toast.makeText(context, "Dosya açılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    private boolean isSupportedFile(String fileName) {
        if (fileName == null) return false;

        String[] supportedExtensions = {
                ".docx", ".doc",
                ".html", ".htm", ".txt",
                ".pdf", ".xls", ".xlsx", ".ppt", ".pptx", ".csv"
        };

        String lowerCaseFileName = fileName.toLowerCase();
        for (String extension : supportedExtensions) {
            if (lowerCaseFileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }



    // ========== PUBLIC UTILITY METHODS ==========

    public static int getPickFileRequestCode() {
        return PICK_FILE_REQUEST;
    }


    public static String[] getSupportedExtensions() {
        return new String[]{
                ".docx", ".doc", ".html", ".htm", ".txt",
                ".pdf", ".xls", ".xlsx", ".ppt", ".pptx", ".csv"
        };
    }
}