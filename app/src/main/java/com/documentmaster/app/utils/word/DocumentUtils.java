package com.documentmaster.app.utils.word;

import android.util.Base64;
import android.util.Log;
import java.util.Locale;

public class DocumentUtils {

    private static final String TAG = "DocumentUtils";

    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static String stripAllHtmlTags(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").trim();
    }


    public static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    public static String cleanBase64Data(String base64Data) {
        if (base64Data == null) return "";

        return base64Data
                // Whitespace'leri kaldır
                .replaceAll("\\s+", "")
                // JavaScript escape'lerini temizle
                .replace("\\n", "")
                .replace("\\r", "")
                .replace("\\t", "")
                // Geçersiz karakterleri kaldır
                .replaceAll("[^A-Za-z0-9+/=]", "")
                .trim();
    }

    public static byte[] tryAlternativeBase64Decode(String base64Data) {

        try {
            Log.d(TAG, "🔄 Base64 decode deneme - NO_WRAP flag");
            return Base64.decode(base64Data, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.d(TAG, "❌ NO_WRAP decode başarısız: " + e.getMessage());
        }

        try {
            Log.d(TAG, "🔄 Base64 decode deneme - Padding ekleme");
            String paddedData = addBase64Padding(base64Data);
            return Base64.decode(paddedData, Base64.DEFAULT);
        } catch (Exception e) {
            Log.d(TAG, "❌ Padding decode başarısız: " + e.getMessage());
        }

        try {
            Log.d(TAG, "🔄 Base64 decode deneme - URL_SAFE flag");
            return Base64.decode(base64Data, Base64.URL_SAFE);
        } catch (Exception e) {
            Log.d(TAG, "❌ URL_SAFE decode başarısız: " + e.getMessage());
        }

        Log.e(TAG, "❌ Tüm Base64 decode yöntemleri başarısız");
        return null;
    }

    public static String addBase64Padding(String base64Data) {
        int paddingLength = 4 - (base64Data.length() % 4);
        if (paddingLength != 4) {
            return base64Data + "=".repeat(paddingLength);
        }
        return base64Data;
    }
    public static int[] calculateImageDimensions(byte[] imageBytes, int maxWidth, int maxHeight) {
        // Basit boyut hesaplama
        int width = maxWidth;
        int height = maxHeight;

        // Resim boyutu çok büyükse küçült
        if (imageBytes.length > 1000000) { // 1MB'dan büyük
            width = maxWidth / 2;
            height = maxHeight / 2;
        } else if (imageBytes.length > 500000) { // 500KB'dan büyük
            width = (int)(maxWidth * 0.75);
            height = (int)(maxHeight * 0.75);
        }

        return new int[]{width, height};
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    public static String getFileTypeDescription(String extension) {
        switch (extension.toLowerCase()) {
            case "docx":
                return "Microsoft Word Belgesi";
            case "html":
            case "htm":
                return "HTML Belgesi";
            case "txt":
                return "Metin Belgesi";
            case "doc":
                return "Microsoft Word Belgesi (Eski Format)";
            case "rtf":
                return "Rich Text Format";
            default:
                return "Bilinmeyen Format";
        }
    }

    public static String cleanHtmlContent(String htmlContent) {
        if (htmlContent == null) return "";

        return htmlContent
                .replaceAll("<[^>]*>", "") // HTML etiketlerini kaldır
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String normalizeHtmlForSaving(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return "<p>Boş belge</p>";
        }

        String normalized = htmlContent
                // Unicode escape karakterlerini decode et
                .replaceAll("\\\\u003C", "<")
                .replaceAll("\\\\u003E", ">")
                .replaceAll("\\\\u0026", "&")
                .replaceAll("\\\\u0022", "\"")
                .replaceAll("\\\\u0027", "'")
                // JavaScript escape karakterlerini temizle
                .replaceAll("\\\\\"", "\"")
                .replaceAll("\\\\n", " ")
                .replaceAll("\\\\r", "")
                .replaceAll("\\\\/", "/")
                .replaceAll("\\\\\\\\", "\\\\")
                // HTML entity'leri decode et
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                // Gereksiz div'leri temizle
                .replaceAll("<div[^>]*>", "")
                .replaceAll("</div>", "")
                // Boş paragrafları düzelt
                .replaceAll("<p[^>]*>\\s*<br[^>]*>\\s*</p>", "<p> </p>")
                .replaceAll("<p[^>]*>\\s*</p>", "<p> </p>")
                .trim();

        Log.d(TAG, "📋 HTML normalizing tamamlandı");
        return normalized;
    }
}