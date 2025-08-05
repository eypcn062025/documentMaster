package com.documentmaster.app.utils;

import android.content.Context;
import android.widget.Toast;

import com.documentmaster.app.Document;
import com.documentmaster.app.document.CreateDocument;
import com.documentmaster.app.document.LoadDocument;
import com.documentmaster.app.document.OpenDocument;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class ActionDialog {

    private final Context context;
    private final CreateDocument createDocument;
    private final OpenDocument openDocument;
    private final LoadDocument loadDocument;



    public ActionDialog(Context context,
                         CreateDocument createDocument, OpenDocument openDocument, LoadDocument loadDocument) {
        this.context = context;
        this.createDocument = createDocument;
        this.openDocument = openDocument;
        this.loadDocument = loadDocument;
    }

    public void showQuickActionDialog() {
        String[] options = {"Yeni Word Belgesi", "Dosya Aç", "Tarama Yap", "Son Belgeleri Görüntüle"};

        new MaterialAlertDialogBuilder(context)
                .setTitle("Hızlı İşlemler")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            createDocument.createNewWordDocument();
                            break;
                        case 1:
                            openDocument.openFilePicker();
                            break;
                        case 2:
                            Toast.makeText(context, "Tarama özelliği geliştiriliyor...", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            showRecentDocuments();
                            break;
                    }
                })
                .show();
    }
    public void showRecentDocuments() {
        loadDocument.loadRecentDocuments(5);
    }
}
