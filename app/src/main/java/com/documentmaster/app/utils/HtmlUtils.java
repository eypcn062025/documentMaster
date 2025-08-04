package com.documentmaster.app.utils;

import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class HtmlUtils {
    private static final String TAG = "HtmlUtils";
    public static String cleanHtmlResult(String result) {
        if (result == null || result.trim().isEmpty()) {
            Log.w("WordEditor", "⚠️ JavaScript sonucu boş");
            return "";
        }

        String cleaned = result.trim();
        Log.d("WordEditor", "🔄 Ham sonuç uzunluğu: " + cleaned.length());


        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }


        cleaned = cleaned
                .replace("\\u003C", "<")
                .replace("\\u003E", ">")
                .replace("\\u0026", "&")
                .replace("\\u0022", "\"")
                .replace("\\u0027", "'");


        cleaned = cleaned
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "")
                .replace("\\/", "/")
                .replace("\\\\", "\\");


        cleaned = cleaned
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");

        Log.d("WordEditor", "✅ Temizlenmiş sonuç uzunluğu: " + cleaned.length());
        Log.d("WordEditor", "📝 Temizlenmiş içerik örneği: " +
                cleaned.substring(0, Math.min(150, cleaned.length())));

        return cleaned;
    }

    public static String cleanHtmlResultAdvanced(String result) {
        if (result == null || result.trim().isEmpty()) {
            Log.w(TAG, "⚠️ JavaScript sonucu boş");
            return "";
        }

        String cleaned = result.trim();
        Log.d(TAG, "🔄 Ham sonuç uzunluğu: " + cleaned.length());

        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }


        cleaned = cleaned
                .replace("\\u003C", "<")
                .replace("\\u003E", ">")
                .replace("\\u0026", "&")
                .replace("\\u0022", "\"")
                .replace("\\u0027", "'")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "")
                .replace("\\/", "/")
                .replace("\\\\", "\\");


        cleaned = cleaned
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");


        cleaned = cleanHtmlPreservingImages(cleaned);

        Log.d(TAG, "✅ Gelişmiş temizleme tamamlandı - Son uzunluk: " + cleaned.length());
        return cleaned;
    }

    private static String cleanHtmlPreservingImages(String html) {
        // Resim verilerini geçici olarak sakla
        java.util.Map<String, String> imageMap = new java.util.HashMap<>();
        Pattern imagePattern = Pattern.compile("data:image/[^;]+;base64,[A-Za-z0-9+/=]+");
        Matcher imageMatcher = imagePattern.matcher(html);

        int imageIndex = 0;
        StringBuffer sb = new StringBuffer();

        while (imageMatcher.find()) {
            String imageData = imageMatcher.group();
            String placeholder = "IMAGE_PLACEHOLDER_" + imageIndex;
            imageMap.put(placeholder, imageData);
            imageMatcher.appendReplacement(sb, placeholder);
            imageIndex++;
        }
        imageMatcher.appendTail(sb);

        // HTML'i temizle
        String cleanedHtml = sb.toString()
                .replaceAll("\\s+", " ")  // Çoklu boşlukları tek boşluğa çevir
                .trim();

        // Resimleri geri koy
        for (java.util.Map.Entry<String, String> entry : imageMap.entrySet()) {
            cleanedHtml = cleanedHtml.replace(entry.getKey(), entry.getValue());
        }

        Log.d(TAG, "🖼️ " + imageMap.size() + " resim korunarak HTML temizlendi");
        return cleanedHtml;
    }

    public static String convertToHtml(String content) {
        if (content == null) return "<p><br></p>";

        // Zaten HTML formatında mı kontrol et
        if (content.trim().startsWith("<") &&
                (content.contains("<p>") || content.contains("<div>") || content.contains("<table"))) {
            Log.d(TAG, "İçerik zaten HTML formatında");
            return content;
        }

        StringBuilder html = new StringBuilder();
        html.append("<div>");

        String[] lines = content.split("\n");
        boolean inTable = false;
        StringBuilder tableBuilder = null;

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) {
                if (!inTable) {
                    html.append("<p><br></p>");
                }
                continue;
            }

            // Tablo satırı mı kontrol et
            if (isTableLine(line)) {
                if (!inTable) {
                    // Yeni tablo başlat
                    inTable = true;
                    tableBuilder = new StringBuilder();
                    tableBuilder.append("<table border=\"1\" style=\"border-collapse: collapse; width: 100%; margin: 10px 0;\">");
                }

                if (line.contains("│")) {
                    // Tablo satırını HTML'e çevir
                    tableBuilder.append(convertTableLineToHtml(line));
                }
            } else {
                // Tablo bitmiş, ekle
                if (inTable && tableBuilder != null) {
                    tableBuilder.append("</table>");
                    html.append(tableBuilder.toString());
                    inTable = false;
                    tableBuilder = null;
                }

                // Normal paragraf ekle
                html.append("<p>").append(escapeHtml(line)).append("</p>");
            }
        }

        // Açık tablo varsa kapat
        if (inTable && tableBuilder != null) {
            tableBuilder.append("</table>");
            html.append(tableBuilder.toString());
        }

        html.append("</div>");

        String result = html.toString();
        Log.d(TAG, "Text → HTML dönüştürüldü");
        return result;
    }

    public static String normalizeHtmlForEditor(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "<p><br></p>";
        }

        String normalized = html
                // Escaped karakterleri düzelt
                .replaceAll("\\\\\"", "\"")
                .replaceAll("\\\\n", "\n")
                .replaceAll("\\\\r", "")
                .replaceAll("\\\\/", "/")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .trim();

        // Eğer HTML tag'i yoksa paragraf olarak wrap et
        if (!normalized.contains("<")) {
            String[] lines = normalized.split("\n");
            StringBuilder htmlBuilder = new StringBuilder();
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    htmlBuilder.append("<p><br></p>");
                } else {
                    htmlBuilder.append("<p>").append(escapeHtml(line.trim())).append("</p>");
                }
            }
            normalized = htmlBuilder.toString();
        }

        Log.d(TAG, "HTML normalize edildi");
        return normalized;
    }

    public static int countImagesInHtml(String html) {
        if (html == null) return 0;

        Pattern pattern = Pattern.compile("<img[^>]*src=\"data:image/[^\"]+\"[^>]*>");
        Matcher matcher = pattern.matcher(html);

        int count = 0;
        while (matcher.find()) {
            count++;
        }

        return count;
    }

    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static boolean isTableLine(String line) {
        return line.contains("│") || line.contains("┌") || line.contains("├") ||
                line.contains("└") || line.contains("─");
    }


    private static String convertTableLineToHtml(String line) {
        if (!line.contains("│")) {
            return "";
        }

        // Satırı hücrelere böl
        String[] cells = line.split("│");
        StringBuilder tableRow = new StringBuilder("<tr>");

        for (String cell : cells) {
            String cleanCell = cell.trim();
            if (!cleanCell.isEmpty() &&
                    !cleanCell.equals("┌") && !cleanCell.equals("├") &&
                    !cleanCell.equals("└") && !cleanCell.equals("┐") &&
                    !cleanCell.equals("┤") && !cleanCell.equals("┘") &&
                    !cleanCell.matches("[-─]+")) {

                tableRow.append("<td style=\"padding: 8px; border: 1px solid #333;\">")
                        .append(escapeHtml(cleanCell))
                        .append("</td>");
            }
        }

        tableRow.append("</tr>");
        return tableRow.toString();
    }

}
