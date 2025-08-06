package com.documentmaster.app.utils.word;

import android.text.Spannable;
import android.text.style.*;
import android.util.Log;

import org.apache.poi.xwpf.usermodel.*;
import java.io.*;
import java.util.List;

public class WordDocumentWriter {

    private static final String TAG = "WordDocumentWriter";

    public static boolean saveHtmlToDocx(String filePath, String htmlContent) {
        try {
            XWPFDocument document = new XWPFDocument();
            Log.d("deneme",htmlContent);
            DocumentConverter.parseHtmlToDocxAdvanced(document, htmlContent);
            boolean success = saveDocument(document, filePath);
            Log.d("deneme",document.getDocument().toString());
            if (success) {
                Log.d(TAG, "✅ HTML→DOCX kaydetme başarılı");
            } else {
                Log.e(TAG, "❌ HTML→DOCX kaydetme başarısız");
            }

            return success;

        } catch (Exception e) {
            Log.e(TAG, "❌ HTML→DOCX kaydetme hatası: " + e.getMessage(), e);
            return false;
        }
    }

    public static boolean createWordDocument(String filePath, String content) {
        try {
            XWPFDocument document = new XWPFDocument();

            if (content != null && !content.trim().isEmpty()) {
                String[] lines = content.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        XWPFParagraph paragraph = document.createParagraph();
                        XWPFRun run = paragraph.createRun();
                        run.setText(line);
                        run.setFontFamily("Calibri");
                        run.setFontSize(11);
                    }
                }
            } else {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(""); // Boş text
                run.setFontFamily("Calibri");
                run.setFontSize(11);
            }

            return saveDocument(document, filePath);

        } catch (Exception e) {
            Log.e(TAG, "Belge oluşturma hatası: " + e.getMessage());
            return false;
        }
    }

    public static boolean saveDocument(XWPFDocument document, String filePath) {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                document.write(fos);
                document.close();
            }

            Log.d(TAG, "Belge başarıyla kaydedildi: " + filePath);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Belge kaydetme hatası: " + e.getMessage());
            return false;
        }
    }
}