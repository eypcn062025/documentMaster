package com.documentmaster.app.document;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.documentmaster.app.R;
import com.documentmaster.app.activities.WordEditorActivity;
import com.documentmaster.app.utils.word.WordDocumentHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class CreateDocument {

    private static final String TAG = "CreateDocument";

    private final Context context;
    private final CreateDocumentCallback callback;

    public interface CreateDocumentCallback {
        void onDocumentCreated();

    }
    public CreateDocument(Context context, CreateDocumentCallback callback) {
        this.context = context;
        this.callback = callback;
    }
    public void showCreateNewDocumentDialog() {
        String[] options = {"Word Belgesi (.docx)", "Metin Belgesi (.txt)", "PDF Belgesi"};

        new MaterialAlertDialogBuilder(context)
                .setTitle("Yeni Belge Oluştur")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            createNewWordDocument();
                            break;
                        case 1:
                            createNewTextDocument();
                            break;
                        case 2:
                            createNewPdf();
                            break;
                    }
                })
                .show();
    }

    public void createNewWordDocument() {
        View dialogView = ((Activity) context).getLayoutInflater().inflate(R.layout.dialog_new_document, null);
        TextInputEditText editFileName = dialogView.findViewById(R.id.editFileName);
        editFileName.setHint("Word belgesi adı");
        editFileName.setText("Yeni Belge");
        new MaterialAlertDialogBuilder(context)
                .setTitle("Yeni Word Belgesi")
                .setView(dialogView)
                .setPositiveButton("Oluştur", (dialog, which) -> {
                    String fileName = editFileName.getText().toString().trim();
                    if (!fileName.isEmpty()) {
                        createWordDocumentFile(fileName);
                    } else {
                        Toast.makeText(context, "Dosya adı boş olamaz", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }
    public void createWordDocumentFile(String fileName) {
        try {
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (!fileName.endsWith(".docx")) {
                fileName += ".docx";
            }

            String filePath = new File(documentsDir, fileName).getAbsolutePath();

            boolean success = WordDocumentHelper.createWordDocument(filePath, "");

            if (success) {
                Toast.makeText(context, "DOCX belgesi oluşturuldu: " + fileName, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(context, WordEditorActivity.class);
                intent.putExtra(WordEditorActivity.EXTRA_FILE_PATH, filePath);
                intent.putExtra(WordEditorActivity.EXTRA_IS_NEW_DOCUMENT, true);
                context.startActivity(intent);

                if (callback != null) {
                    callback.onDocumentCreated();
                }
            } else {
                Toast.makeText(context, "DOCX belgesi oluşturulamadı", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "DOCX belge oluşturma hatası: " + e.getMessage());
            Toast.makeText(context, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();

        }
    }

    public void createNewTextDocument() {
        Toast.makeText(context, "Metin editörü geliştiriliyor...", Toast.LENGTH_SHORT).show();
    }

    public void createNewPdf() {
        Toast.makeText(context, "PDF oluşturma özelliği geliştiriliyor...", Toast.LENGTH_SHORT).show();
    }
}