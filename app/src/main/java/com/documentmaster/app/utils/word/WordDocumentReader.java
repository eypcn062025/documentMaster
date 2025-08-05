package com.documentmaster.app.utils.word;

import android.util.Log;

import org.apache.poi.xwpf.usermodel.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WordDocumentReader {

    private static final String TAG = "WordDocumentReader";

    public static WordDocumentHelper.WordContent readWordDocument(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return new WordDocumentHelper.WordContent("", false, "Dosya bulunamadı");
            }

            if (filePath.toLowerCase().endsWith(".docx")) {
                return readDocxDocumentAsHtml(filePath);
            } else if (filePath.toLowerCase().endsWith(".html") || filePath.toLowerCase().endsWith(".txt")) {
                return readTextDocument(filePath);
            } else {
                return new WordDocumentHelper.WordContent("", false, "Desteklenmeyen dosya formatı");
            }
        } catch (Exception e) {
            Log.e(TAG, "Belge okuma hatası: " + e.getMessage());
            return new WordDocumentHelper.WordContent("", false, "Belge okuma hatası: " + e.getMessage());
        }
    }

    public static WordDocumentHelper.WordContent readDocxDocumentAsHtml(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<div>");

            List<IBodyElement> bodyElements = document.getBodyElements();

            for (IBodyElement element : bodyElements) {
                if (element instanceof XWPFParagraph) {
                    XWPFParagraph paragraph = (XWPFParagraph) element;
                    htmlContent.append(DocumentConverter.convertParagraphToHtmlWithImages(paragraph));
                } else if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;
                    htmlContent.append(DocumentConverter.convertTableToHtml(table));
                }
            }

            htmlContent.append("</div>");
            String finalHtml = htmlContent.toString();

            Log.d(TAG, "DOCX → HTML dönüştürüldü: " + filePath);
            Log.d(TAG, "HTML içeriği: " + finalHtml.substring(0, Math.min(200, finalHtml.length())));

            return new WordDocumentHelper.WordContent(finalHtml, true, null);

        } catch (Exception e) {
            Log.e(TAG, "DOCX → HTML dönüştürme hatası: " + e.getMessage());
            return new WordDocumentHelper.WordContent("", false, "DOCX okuma hatası: " + e.getMessage());
        }
    }

    public static WordDocumentHelper.WordContent readTextDocument(String filePath) {
        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            String finalContent = DocumentUtils.cleanHtmlContent(content.toString());
            return new WordDocumentHelper.WordContent(finalContent, true, null);
        } catch (IOException e) {
            Log.e(TAG, "Text dosya okuma hatası: " + e.getMessage());
            return new WordDocumentHelper.WordContent("", false, "Dosya okuma hatası: " + e.getMessage());
        }
    }

    public static String getDocumentProperties(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return "Dosya bulunamadı: " + filePath;
            }

            StringBuilder properties = new StringBuilder();

            // Temel dosya bilgileri
            properties.append("📄 Dosya Adı: ").append(file.getName()).append("\n");
            properties.append("📁 Konum: ").append(file.getParent()).append("\n");
            properties.append("📏 Dosya Boyutu: ").append(DocumentUtils.formatFileSize(file.length())).append("\n");

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            properties.append("📅 Son Değişiklik: ").append(dateFormat.format(new Date(file.lastModified()))).append("\n");

            if (filePath.toLowerCase().endsWith(".docx")) {
                try (FileInputStream fis = new FileInputStream(file);
                     XWPFDocument document = new XWPFDocument(fis)) {

                    properties.append("🏷️ Dosya Türü: Microsoft Word Belgesi (.docx)\n");

                    // İçerik analizi
                    int paragraphCount = document.getParagraphs().size();
                    int tableCount = document.getTables().size();

                    StringBuilder text = new StringBuilder();
                    for (XWPFParagraph paragraph : document.getParagraphs()) {
                        text.append(paragraph.getText()).append(" ");
                    }

                    String[] words = text.toString().trim().split("\\s+");
                    int wordCount = text.toString().trim().isEmpty() ? 0 : words.length;
                    int charCount = text.toString().length();
                    int charCountNoSpaces = text.toString().replaceAll("\\s", "").length();

                    properties.append("\n📊 İçerik Analizi:\n");
                    properties.append("📝 Paragraf Sayısı: ").append(paragraphCount).append("\n");
                    properties.append("📋 Tablo Sayısı: ").append(tableCount).append("\n");
                    properties.append("📖 Kelime Sayısı: ").append(wordCount).append("\n");
                    properties.append("🔤 Karakter Sayısı: ").append(charCount).append("\n");
                    properties.append("🔠 Boşluksuz Karakter: ").append(charCountNoSpaces).append("\n");

                    // Ortalama kelime uzunluğu
                    if (wordCount > 0) {
                        double avgWordLength = (double) charCountNoSpaces / wordCount;
                        properties.append("📐 Ortalama Kelime Uzunluğu: ").append(String.format("%.1f", avgWordLength)).append(" karakter\n");
                    }

                    // Okuma süresi tahmini
                    if (wordCount > 0) {
                        double readingTimeMinutes = (double) wordCount / 200;
                        if (readingTimeMinutes < 1) {
                            properties.append("⏱️ Tahmini Okuma Süresi: 1 dakikadan az\n");
                        } else {
                            properties.append("⏱️ Tahmini Okuma Süresi: ").append(String.format("%.1f", readingTimeMinutes)).append(" dakika\n");
                        }
                    }

                    document.close();
                }
            } else {
                properties.append("🏷️ Dosya Türü: ").append(DocumentUtils.getFileTypeDescription(DocumentUtils.getFileExtension(file.getName()))).append("\n");
            }

            // Dosya izinleri
            properties.append("\n🔐 Dosya İzinleri:\n");
            properties.append("👁️ Okunabilir: ").append(file.canRead() ? "✅ Evet" : "❌ Hayır").append("\n");
            properties.append("✏️ Yazılabilir: ").append(file.canWrite() ? "✅ Evet" : "❌ Hayır").append("\n");

            return properties.toString();

        } catch (Exception e) {
            Log.e(TAG, "Özellikler alma hatası: " + e.getMessage());
            return "Özellikler alınamadı: " + e.getMessage();
        }
    }


    public static boolean isValidWordDocument(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                return false;
            }

            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".docx")) {
                try (FileInputStream fis = new FileInputStream(file);
                     XWPFDocument document = new XWPFDocument(fis)) {
                    document.close();
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "DOCX geçerlilik hatası: " + e.getMessage());
                    return false;
                }
            } else {
                return fileName.endsWith(".html") || fileName.endsWith(".htm") || fileName.endsWith(".txt");
            }

        } catch (Exception e) {
            Log.e(TAG, "Geçerlilik kontrolü hatası: " + e.getMessage());
            return false;
        }
    }
}