package com.documentmaster.app.document;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.documentmaster.app.Document;
import com.documentmaster.app.R;
import com.documentmaster.app.activities.WordEditorActivity;
import com.documentmaster.app.utils.WordDocumentHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class DocumentMenuOperations {

    private static final String TAG = "DocumentOperations";

    private final Context context;
    private final DocumentOperationCallback callback;

    public interface DocumentOperationCallback {
        void onDocumentUpdated();
        void onDocumentDeleted(Document document);
        void onDocumentRenamed(Document document);

    }

    public DocumentMenuOperations(Context context, DocumentOperationCallback callback) {
        this.context = context;
        this.callback = callback;
    }


    public void openDocument(Document document) {
        if (document == null || document.getPath() == null) {
            showToast("Geçersiz belge");
            return;
        }

        String fileName = document.getName().toLowerCase();
        String filePath = document.getPath();

        Log.d(TAG, "Açılmaya çalışılan dosya: " + filePath);
        Log.d(TAG, "Dosya türü: " + fileName);

        try {
            if (fileName.endsWith(".docx") || fileName.endsWith(".doc") ||
                    fileName.endsWith(".html") || fileName.endsWith(".htm") || fileName.endsWith(".txt")) {

                if (WordDocumentHelper.isValidWordDocument(filePath)) {
                    Log.d(TAG, "Geçerli Word belgesi, editör açılıyor...");
                    Intent intent = new Intent(context, WordEditorActivity.class);
                    intent.putExtra(WordEditorActivity.EXTRA_FILE_PATH, filePath);
                    intent.putExtra(WordEditorActivity.EXTRA_IS_NEW_DOCUMENT, false);
                    context.startActivity(intent);
                } else {
                    Log.e(TAG, "Geçersiz Word belgesi: " + filePath);
                    showToast("Belge açılamadı - dosya bozulmuş olabilir");
                }
            } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                showToast("Excel editörü geliştiriliyor...");
            } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
                showToast("PowerPoint editörü geliştiriliyor...");
            } else if (fileName.endsWith(".pdf")) {
                showToast("PDF görüntüleyici geliştiriliyor...");
            } else {
                // Sistem editörü ile açmayı dene
                tryOpenWithSystemEditor(document);
            }
        } catch (Exception e) {
            Log.e(TAG, "Belge açma genel hatası: " + e.getMessage());
            showToast("Belge açılırken hata oluştu: " + e.getMessage());
        }
    }

    public void editDocument(Document document) {
        if (document == null || document.getPath() == null) {
            showToast("Geçersiz belge");
            return;
        }

        String fileName = document.getName().toLowerCase();

        try {
            if (fileName.endsWith(".docx") || fileName.endsWith(".doc") ||
                    fileName.endsWith(".html") || fileName.endsWith(".htm") || fileName.endsWith(".txt")) {
                Intent intent = new Intent(context, WordEditorActivity.class);
                intent.putExtra(WordEditorActivity.EXTRA_FILE_PATH, document.getPath());
                intent.putExtra(WordEditorActivity.EXTRA_IS_NEW_DOCUMENT, false);
                context.startActivity(intent);
            } else {
                showToast("Bu dosya türü henüz düzenlenemez");
            }
        } catch (Exception e) {
            Log.e(TAG, "Düzenleme hatası: " + e.getMessage());
            showToast("Düzenleme başlatılamadı: " + e.getMessage());
        }
    }

    /**
     * Belgeyi paylaşır
     */
    public void shareDocument(Document document) {
        if (document == null || document.getPath() == null) {
            showToast("Geçersiz belge");
            return;
        }

        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(document.getPath())));
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, document.getName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "DocumentMaster ile paylaşılan belge: " + document.getName());
            context.startActivity(Intent.createChooser(shareIntent, "Belgeyi Paylaş"));
        } catch (Exception e) {
            Log.e(TAG, "Paylaşma hatası: " + e.getMessage());
            showToast("Paylaşma hatası: " + e.getMessage());
        }
    }

    /**
     * Belgeyi siler (onaylama dialogu ile)
     */
    public void deleteDocument(Document document) {
        if (document == null || document.getPath() == null) {
            showToast("Geçersiz belge");
            return;
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle("Belgeyi Sil")
                .setMessage("Bu belgeyi silmek istediğinize emin misiniz?\n\n📄 " + document.getName() +
                        "\n📏 " + document.getFormattedSize() +
                        "\n📅 " + document.getFormattedDate())
                .setPositiveButton("Sil", (dialog, which) -> {
                    performDelete(document);
                })
                .setNegativeButton("İptal", null)
                .setIcon(android.R.drawable.ic_menu_delete)
                .show();
    }

    /**
     * Belgeyi yeniden adlandırır
     */
    public void renameDocument(Document document) {
        if (document == null || document.getPath() == null) {
            showToast("Geçersiz belge");
            return;
        }

        View dialogView = ((Activity) context).getLayoutInflater().inflate(R.layout.dialog_rename, null);
        TextInputEditText editNewName = dialogView.findViewById(R.id.editNewName);

        String currentName = document.getName();
        String nameWithoutExtension = currentName.contains(".") ?
                currentName.substring(0, currentName.lastIndexOf(".")) : currentName;
        editNewName.setText(nameWithoutExtension);
        editNewName.selectAll();

        new MaterialAlertDialogBuilder(context)
                .setTitle("Yeniden Adlandır")
                .setMessage("Mevcut ad: " + document.getName())
                .setView(dialogView)
                .setPositiveButton("Değiştir", (dialog, which) -> {
                    String newName = editNewName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        performRename(document, newName);
                    } else {
                        showToast("Dosya adı boş olamaz");
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    /**
     * Belge özelliklerini gösterir
     */
    public void showDocumentProperties(Document document) {
        if (document == null || document.getPath() == null) {
            showToast("Geçersiz belge");
            return;
        }

        try {
            String properties;

            if (document.getName().toLowerCase().endsWith(".docx") ||
                    document.getName().toLowerCase().endsWith(".doc") ||
                    document.getName().toLowerCase().endsWith(".html") ||
                    document.getName().toLowerCase().endsWith(".htm") ||
                    document.getName().toLowerCase().endsWith(".txt")) {
                properties = WordDocumentHelper.getDocumentProperties(document.getPath());
            } else {
                // Basit dosya özellikleri
                File file = new File(document.getPath());
                properties = "📄 Ad: " + document.getName() + "\n" +
                        "🏷️ Tür: " + document.getType() + "\n" +
                        "📏 Boyut: " + document.getFormattedSize() + "\n" +
                        "📅 Tarih: " + document.getFormattedDate() + "\n" +
                        "📁 Konum: " + document.getPath() + "\n" +
                        "🔐 Okunabilir: " + (file.canRead() ? "Evet" : "Hayır") + "\n" +
                        "✏️ Yazılabilir: " + (file.canWrite() ? "Evet" : "Hayır");
            }

            new MaterialAlertDialogBuilder(context)
                    .setTitle("📋 Belge Özellikleri")
                    .setMessage(properties)
                    .setPositiveButton("Tamam", null)
                    .setNeutralButton("Paylaş", (dialog, which) -> shareDocument(document))
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Özellikler gösterme hatası: " + e.getMessage());
            showToast("Özellikler gösterilemiyor: " + e.getMessage());
        }
    }

    /**
     * Belge menüsünü gösterir
     */
    public void showDocumentOptionsMenu(Document document) {
        if (document == null) {
            showToast("Geçersiz belge");
            return;
        }

        String[] options = {"Aç", "Düzenle", "Paylaş", "Sil", "Yeniden Adlandır", "Özellikler"};

        new MaterialAlertDialogBuilder(context)
                .setTitle(document.getName())
                .setItems(options, (dialog, which) -> {
                    try {
                        switch (which) {
                            case 0:
                                openDocument(document);
                                break;
                            case 1:
                                editDocument(document);
                                break;
                            case 2:
                                shareDocument(document);
                                break;
                            case 3:
                                deleteDocument(document);
                                break;
                            case 4:
                                renameDocument(document);
                                break;
                            case 5:
                                showDocumentProperties(document);
                                break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Belge işlemi hatası: " + e.getMessage());
                        showToast("İşlem başarısız: " + e.getMessage());
                    }
                })
                .show();
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void performDelete(Document document) {
        try {
            File file = new File(document.getPath());
            if (file.exists() && file.delete()) {
                showToast("✅ Belge silindi: " + document.getName());
                if (callback != null) {
                    callback.onDocumentDeleted(document);
                }
            } else {
                showToast("❌ Belge silinemedi");
            }
        } catch (Exception e) {
            Log.e(TAG, "Silme hatası: " + e.getMessage());
            showToast("Silme hatası: " + e.getMessage());
        }
    }

    private void performRename(Document document, String newName) {
        try {
            File oldFile = new File(document.getPath());
            String extension = "";
            String oldName = oldFile.getName();

            if (oldName.contains(".")) {
                extension = oldName.substring(oldName.lastIndexOf("."));
            }

            File newFile = new File(oldFile.getParent(), newName + extension);

            if (newFile.exists()) {
                showToast("Bu isimde bir dosya zaten var");
                return;
            }

            if (oldFile.renameTo(newFile)) {
                document.setName(newName + extension);
                document.setPath(newFile.getAbsolutePath());
                showToast("✅ Dosya adı değiştirildi: " + newName + extension);

                if (callback != null) {
                    callback.onDocumentRenamed(document);
                }
            } else {
                showToast("❌ Dosya adı değiştirilemedi");
            }
        } catch (Exception e) {
            Log.e(TAG, "Yeniden adlandırma hatası: " + e.getMessage());
            showToast("Yeniden adlandırma hatası: " + e.getMessage());
        }
    }

    private void tryOpenWithSystemEditor(Document document) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(document.getPath())), "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Sistem editörü açma hatası: " + e.getMessage());
            showToast("Dosya açılamadı");
        }
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}