package com.documentmaster.app.document;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.documentmaster.app.Document;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class LoadDocument {

    private static final String TAG = "LoadDocument";

    private final Context context;
    private final LoadDocumentCallback callback;

    public interface LoadDocumentCallback {
        void onDocumentsLoaded(List<Document> documents);
        void onDocumentLoadFailed(String error);
    }

    public LoadDocument(Context context, LoadDocumentCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void loadDocuments() {
        List<Document> documentList = new ArrayList<>();

        try {
            loadInternalDocuments(documentList);
            loadDownloadsDocuments(documentList);
            loadDocumentsFromFolder(documentList);

            if (callback != null) {
                callback.onDocumentsLoaded(documentList);
            }

            Log.d(TAG, "Toplam " + documentList.size() + " belge yüklendi");

        } catch (Exception e) {
            Log.e(TAG, "Belge yükleme hatası: " + e.getMessage());
            if (callback != null) {
                callback.onDocumentLoadFailed("Belge yükleme hatası: " + e.getMessage());
            }
        }
    }

    public void loadInternalDocuments() {
        List<Document> documentList = new ArrayList<>();
        loadInternalDocuments(documentList);

        if (callback != null) {
            callback.onDocumentsLoaded(documentList);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void loadInternalDocuments(List<Document> documentList) {
        File internalDir = new File(context.getFilesDir(), "Documents");
        if (internalDir.exists()) {
            loadDocumentsFromDirectory(internalDir, documentList);
            Log.d(TAG, "Internal documents yüklendi: " + internalDir.getAbsolutePath());
        }
    }

    private void loadDownloadsDocuments(List<Document> documentList) {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloadsDir != null && downloadsDir.exists()) {
                loadDocumentsFromDirectory(downloadsDir, documentList);
                Log.d(TAG, "Downloads klasörü tarandı: " + downloadsDir.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Downloads klasörü yüklenemedi: " + e.getMessage());
        }
    }

    private void loadDocumentsFromFolder(List<Document> documentList) {
        try {
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (documentsDir != null && documentsDir.exists()) {
                loadDocumentsFromDirectory(documentsDir, documentList);
                Log.d(TAG, "Documents klasörü tarandı: " + documentsDir.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Documents klasörü yüklenemedi: " + e.getMessage());
        }
    }

    private void loadDocumentsFromDirectory(File directory, List<Document> documentList) {
        try {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && isSupportedFile(file.getName())) {
                        Document document = new Document(file.getAbsolutePath());
                        documentList.add(document);
                        Log.d(TAG, "Belge eklendi: " + file.getName());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Dizin tarama hatası: " + e.getMessage() + " - Dizin: " + directory.getAbsolutePath());
        }
    }

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

    public void loadRecentDocuments(int count) {
        List<Document> allDocuments = new ArrayList<>();
        loadInternalDocuments(allDocuments);
        loadDownloadsDocuments(allDocuments);
        loadDocumentsFromFolder(allDocuments);

        List<Document> recentDocuments = new ArrayList<>();
        for (int i = 0; i < Math.min(count, allDocuments.size()); i++) {
            recentDocuments.add(allDocuments.get(i));
        }

        if (callback != null) {
            callback.onDocumentsLoaded(recentDocuments);
        }
    }

}