package com.documentmaster.app.utils;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import android.util.Base64;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import java.io.ByteArrayInputStream;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
public class WordDocumentHelper {

    private static final String TAG = "WordDocumentHelper";

    public static class WordContent {
        private String content;
        private XWPFDocument document;
        private SpannableString spannableContent;
        private boolean isSuccess;
        private String error;

        public WordContent(String content, boolean isSuccess, String error) {
            this.content = content;
            this.isSuccess = isSuccess;
            this.error = error;
        }

        public WordContent(XWPFDocument document, boolean isSuccess, String error) {
            this.document = document;
            this.isSuccess = isSuccess;
            this.error = error;
        }

        public WordContent(SpannableString spannableContent, boolean isSuccess, String error) {
            this.spannableContent = spannableContent;
            this.content = spannableContent != null ? spannableContent.toString() : "";
            this.isSuccess = isSuccess;
            this.error = error;
        }

        public String getContent() { return content; }
        public XWPFDocument getDocument() { return document; }
        public SpannableString getSpannableContent() { return spannableContent; }
        public boolean isSuccess() { return isSuccess; }
        public String getError() { return error; }
    }

    private static String convertParagraphToHtmlWithImages(XWPFParagraph paragraph) {
        if (paragraph.getText().trim().isEmpty() && paragraph.getRuns().isEmpty()) {
            return "<p><br></p>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<p");

        ParagraphAlignment alignment = paragraph.getAlignment();
        if (alignment != null) {
            switch (alignment) {
                case CENTER:
                    html.append(" style=\"text-align: center;\"");
                    break;
                case RIGHT:
                    html.append(" style=\"text-align: right;\"");
                    break;
                case BOTH:
                    html.append(" style=\"text-align: justify;\"");
                    break;
                default:
                    html.append(" style=\"text-align: left;\"");
                    break;
            }
        }

        html.append(">");

        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) {
            html.append(escapeHtml(paragraph.getText()));
        } else {
            for (XWPFRun run : runs) {

                List<XWPFPicture> pictures = run.getEmbeddedPictures();
                if (!pictures.isEmpty()) {
                    for (XWPFPicture picture : pictures) {
                        html.append(convertPictureToHtml(picture));
                    }
                } else {

                    html.append(convertRunToHtml(run));
                }
            }
        }

        html.append("</p>");
        return html.toString();
    }


    private static String convertPictureToHtml(XWPFPicture picture) {
        try {
            XWPFPictureData pictureData = picture.getPictureData();
            if (pictureData != null) {
                byte[] imageBytes = pictureData.getData();
                String mimeType = pictureData.getPackagePart().getContentType();

                if (imageBytes != null && imageBytes.length > 0) {
                    String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                    StringBuilder imgHtml = new StringBuilder();
                    imgHtml.append("<img src=\"data:")
                            .append(mimeType)
                            .append(";base64,")
                            .append(base64Image)
                            .append("\" ");

                    imgHtml.append("style=\"max-width: 100%; height: auto; margin: 10px 0;\" ");
                    imgHtml.append("alt=\"Belge Resmi\" />");

                    Log.d(TAG, "Resim HTML'e çevrildi - Boyut: " + imageBytes.length + " bytes");
                    return imgHtml.toString();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Resim dönüştürme hatası: " + e.getMessage());
        }

        return "<p>[Resim yüklenemedi]</p>";
    }

    public static WordContent readWordDocument(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return new WordContent("", false, "Dosya bulunamadı");
            }
            if (filePath.toLowerCase().endsWith(".docx")) {
                return readDocxDocumentAsHtml(filePath);
            } else if (filePath.toLowerCase().endsWith(".html") || filePath.toLowerCase().endsWith(".txt")) {
                return readTextDocument(filePath);
            } else {
                return new WordContent("", false, "Desteklenmeyen dosya formatı");
            }

        } catch (Exception e) {
            Log.e(TAG, "Belge okuma hatası: " + e.getMessage());
            return new WordContent("", false, "Belge okuma hatası: " + e.getMessage());
        }
    }

    private static WordContent readDocxDocumentAsHtml(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<div>");

            List<IBodyElement> bodyElements = document.getBodyElements();

            for (IBodyElement element : bodyElements) {
                if (element instanceof XWPFParagraph) {
                    XWPFParagraph paragraph = (XWPFParagraph) element;
                    htmlContent.append(convertParagraphToHtml(paragraph));
                } else if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;
                    htmlContent.append(convertTableToHtml(table));
                }
            }

            htmlContent.append("</div>");
            String finalHtml = htmlContent.toString();

            Log.d(TAG, "DOCX → HTML dönüştürüldü: " + filePath);
            Log.d(TAG, "HTML içeriği: " + finalHtml.substring(0, Math.min(200, finalHtml.length())));

            return new WordContent(finalHtml, true, null);

        } catch (Exception e) {
            Log.e(TAG, "DOCX → HTML dönüştürme hatası: " + e.getMessage());
            return new WordContent("", false, "DOCX okuma hatası: " + e.getMessage());
        }
    }

    private static String convertParagraphToHtml(XWPFParagraph paragraph) {
        return convertParagraphToHtmlWithImages(paragraph);
    }

    private static String convertRunToHtml(XWPFRun run) {
        String text = run.getText(0);
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        boolean hasFormatting = false;


        if (run.isBold()) {
            html.append("<strong>");
            hasFormatting = true;
        }


        if (run.isItalic()) {
            html.append("<em>");
        }


        if (run.getUnderline() != UnderlinePatterns.NONE) {
            html.append("<u>");
        }

        String fontFamily = run.getFontFamily();
        int fontSize = run.getFontSize();
        String color = run.getColor();

        boolean hasStyle = fontFamily != null || fontSize != -1 || color != null;
        if (hasStyle) {
            html.append("<span style=\"");
            if (fontFamily != null) {
                html.append("font-family: ").append(fontFamily).append(";");
            }
            if (fontSize != -1) {
                html.append("font-size: ").append(fontSize).append("pt;");
            }
            if (color != null && !color.equals("auto")) {
                html.append("color: #").append(color).append(";");
            }
            html.append("\">");
        }
        html.append(escapeHtml(text));
        if (hasStyle) {
            html.append("</span>");
        }
        if (run.getUnderline() != UnderlinePatterns.NONE) {
            html.append("</u>");
        }
        if (run.isItalic()) {
            html.append("</em>");
        }
        if (run.isBold()) {
            html.append("</strong>");
        }

        return html.toString();
    }

    private static String convertTableToHtml(XWPFTable table) {
        StringBuilder html = new StringBuilder();
        html.append("<table border=\"1\" style=\"border-collapse: collapse; width: 100%; margin: 10px 0;\">");

        List<XWPFTableRow> rows = table.getRows();
        for (XWPFTableRow row : rows) {
            html.append("<tr>");
            List<XWPFTableCell> cells = row.getTableCells();
            for (XWPFTableCell cell : cells) {
                html.append("<td style=\"padding: 8px; border: 1px solid #333;\">");

                // Cell içindeki paragrafları işle
                List<XWPFParagraph> cellParagraphs = cell.getParagraphs();
                for (int i = 0; i < cellParagraphs.size(); i++) {
                    XWPFParagraph para = cellParagraphs.get(i);
                    String cellText = para.getText().trim();
                    if (!cellText.isEmpty()) {
                        html.append(escapeHtml(cellText));
                        if (i < cellParagraphs.size() - 1) {
                            html.append("<br>");
                        }
                    }
                }

                html.append("</td>");
            }
            html.append("</tr>");
        }

        html.append("</table>");
        return html.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static boolean saveHtmlToDocx(String filePath, String htmlContent) {
        try {
            Log.d(TAG, "💾 HTML→DOCX kaydetme başlıyor...");
            Log.d(TAG, "📝 HTML uzunluğu: " + (htmlContent != null ? htmlContent.length() : 0));
            Log.d(TAG, "📁 Hedef dosya: " + filePath);

            XWPFDocument document = new XWPFDocument();


            String cleanedHtml = normalizeHtmlForSaving(htmlContent);
            Log.d(TAG, "🧹 Temizlenmiş HTML uzunluğu: " + cleanedHtml.length());

            // HTML'i parse et ve DOCX'e çevir
            parseHtmlToDocxAdvanced(document, cleanedHtml);

            boolean success = saveDocument(document, filePath);

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

    private static String normalizeHtmlForSaving(String htmlContent) {
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

    private static void parseHtmlToDocxAdvanced(XWPFDocument document, String htmlContent) {
        try {
            Log.d(TAG, "🔄 Gelişmiş HTML parsing başlıyor...");


            String[] parts = splitHtmlContent(htmlContent);

            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty()) continue;

                if (part.contains("<table")) {
                    Log.d(TAG, "📋 Tablo bulundu, işleniyor...");
                    parseHtmlTable(document, part);
                } else if (part.contains("<img")) {
                    Log.d(TAG, "🖼️ Resim bulundu, işleniyor...");
                    parseImageToDocx(document, part);
                } else if (part.startsWith("<p")) {
                    Log.d(TAG, "📝 Paragraf bulundu, işleniyor...");
                    parseParagraphToDocx(document, part);
                } else {
                    Log.d(TAG, "📄 Düz metin olarak işleniyor...");
                    addPlainTextToDocument(document, stripAllHtmlTags(part));
                }
            }

            if (document.getParagraphs().isEmpty()) {
                Log.d(TAG, "📝 Boş belge, varsayılan paragraf ekleniyor...");
                XWPFParagraph emptyPara = document.createParagraph();
                XWPFRun emptyRun = emptyPara.createRun();
                emptyRun.setText(" ");
                emptyRun.setFontFamily("Calibri");
                emptyRun.setFontSize(11);
            }

            Log.d(TAG, "✅ HTML parsing tamamlandı - Toplam paragraf: " + document.getParagraphs().size());

        } catch (Exception e) {
            Log.e(TAG, "❌ HTML parse hatası: " + e.getMessage(), e);
            // Fallback: Tüm HTML'i düz metin olarak ekle
            addPlainTextToDocument(document, stripAllHtmlTags(htmlContent));
        }
    }

    private static void parseParagraphToDocx(XWPFDocument document, String paragraphHtml) {
        try {
            XWPFParagraph paragraph = document.createParagraph();

            // Alignment kontrolü
            if (paragraphHtml.contains("text-align: center")) {
                paragraph.setAlignment(ParagraphAlignment.CENTER);
            } else if (paragraphHtml.contains("text-align: right")) {
                paragraph.setAlignment(ParagraphAlignment.RIGHT);
            } else if (paragraphHtml.contains("text-align: justify")) {
                paragraph.setAlignment(ParagraphAlignment.BOTH);
            }

            // P tag içindeki içeriği al
            String content = extractContentFromPTag(paragraphHtml);

            // İçerikte resim var mı kontrol et
            if (content.contains("<img")) {
                parseInlineImageAndText(paragraph, content);
            } else {
                // Sadece metin, format parsing yap
                parseFormattedTextToRun(paragraph, content);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Paragraf parse hatası: " + e.getMessage());
            // Fallback: düz metin ekle
            addPlainTextToDocument(document, stripAllHtmlTags(paragraphHtml));
        }
    }

    private static void parseImageToDocx(XWPFDocument document, String imageHtml) {
        try {
            Log.d(TAG, "🖼️ Resim DOCX'e ekleniyor...");

            // Base64 data'yı çıkar
            Pattern pattern = Pattern.compile("src=\"data:([^;]+);base64,([^\"\\s]+)\"");
            Matcher matcher = pattern.matcher(imageHtml);

            if (matcher.find()) {
                String mimeType = matcher.group(1).trim();
                String base64Data = matcher.group(2).trim();

                Log.d(TAG, "📷 MIME Type: " + mimeType);
                Log.d(TAG, "📊 Base64 uzunluğu: " + base64Data.length());
                Log.d(TAG, "🔍 Base64 ilk 50 karakter: " + base64Data.substring(0, Math.min(50, base64Data.length())));

                // Base64 temizleme - ÖNEMLİ!
                String cleanBase64 = cleanBase64Data(base64Data);
                Log.d(TAG, "🧹 Temizlenmiş Base64 uzunluğu: " + cleanBase64.length());

                if (cleanBase64.isEmpty()) {
                    Log.e(TAG, "❌ Base64 verisi temizlendikten sonra boş kaldı");
                    addImagePlaceholder(document, "Base64 verisi boş");
                    return;
                }

                // Base64'ü decode et
                byte[] imageBytes;
                try {
                    imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                    Log.d(TAG, "✅ Base64 decode başarılı - Byte uzunluğu: " + imageBytes.length);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "❌ Base64 decode hatası: " + e.getMessage());
                    Log.d(TAG, "🔍 Hatalı Base64 örneği: " + cleanBase64.substring(0, Math.min(100, cleanBase64.length())));

                    // Alternatif decode yöntemleri dene
                    imageBytes = tryAlternativeBase64Decode(cleanBase64);
                    if (imageBytes == null) {
                        addImagePlaceholder(document, "Base64 decode hatası: " + e.getMessage());
                        return;
                    }
                }

                if (imageBytes.length == 0) {
                    Log.e(TAG, "❌ Decode edilen resim verisi boş");
                    addImagePlaceholder(document, "Resim verisi boş");
                    return;
                }

                // POI format türünü belirle
                int format = determinePOIImageFormat(mimeType, imageBytes);
                Log.d(TAG, "📎 POI format: " + format);

                XWPFParagraph imageParagraph = document.createParagraph();
                imageParagraph.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun imageRun = imageParagraph.createRun();

                try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                    int[] dimensions = calculateImageDimensions(imageBytes, 400, 300);

                    imageRun.addPicture(bis, format, "document_image",
                            Units.toEMU(dimensions[0]), Units.toEMU(dimensions[1]));

                    Log.d(TAG, "✅ Resim başarıyla DOCX'e eklendi - Boyut: " + imageBytes.length +
                            " bytes, Dimensions: " + dimensions[0] + "x" + dimensions[1]);
                }

            } else {
                Log.w(TAG, "⚠️ Resimde Base64 data pattern bulunamadı");
                Log.d(TAG, "🔍 Resim HTML: " + imageHtml.substring(0, Math.min(200, imageHtml.length())));
                addImagePlaceholder(document, "Base64 pattern bulunamadı");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Resim DOCX ekleme hatası: " + e.getMessage(), e);
            addImagePlaceholder(document, "Resim işleme hatası: " + e.getMessage());
        }
    }

    private static String cleanBase64Data(String base64Data) {
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

    private static byte[] tryAlternativeBase64Decode(String base64Data) {

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

    private static int determinePOIImageFormat(String mimeType, byte[] imageBytes) {

        if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
            return XWPFDocument.PICTURE_TYPE_JPEG;
        } else if (mimeType.contains("png")) {
            return XWPFDocument.PICTURE_TYPE_PNG;
        } else if (mimeType.contains("gif")) {
            return XWPFDocument.PICTURE_TYPE_GIF;
        } else if (mimeType.contains("bmp")) {
            return XWPFDocument.PICTURE_TYPE_BMP;
        }

        // Byte signature'a göre kontrol et
        if (imageBytes.length >= 4) {
            // PNG signature: 89 50 4E 47
            if (imageBytes[0] == (byte)0x89 && imageBytes[1] == 0x50 &&
                    imageBytes[2] == 0x4E && imageBytes[3] == 0x47) {
                return XWPFDocument.PICTURE_TYPE_PNG;
            }

            // JPEG signature: FF D8 FF
            if (imageBytes[0] == (byte)0xFF && imageBytes[1] == (byte)0xD8 &&
                    imageBytes[2] == (byte)0xFF) {
                return XWPFDocument.PICTURE_TYPE_JPEG;
            }

            // GIF signature: 47 49 46
            if (imageBytes[0] == 0x47 && imageBytes[1] == 0x49 && imageBytes[2] == 0x46) {
                return XWPFDocument.PICTURE_TYPE_GIF;
            }
        }

        // Varsayılan PNG
        Log.d(TAG, "⚠️ Resim formatı belirlenemedi, PNG varsayıldı");
        return XWPFDocument.PICTURE_TYPE_PNG;
    }

    private static void addImagePlaceholder(XWPFDocument document, String errorMessage) {
        XWPFParagraph placeholder = document.createParagraph();
        placeholder.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = placeholder.createRun();
        run.setText("[Resim yüklenemedi: " + errorMessage + "]");
        run.setItalic(true);
        run.setColor("999999");
        run.setFontFamily("Arial");
        run.setFontSize(10);
    }


    private static int[] calculateImageDimensions(byte[] imageBytes, int maxWidth, int maxHeight) {
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

    private static String addBase64Padding(String base64Data) {
        int paddingLength = 4 - (base64Data.length() % 4);
        if (paddingLength != 4) {
            return base64Data + "=".repeat(paddingLength);
        }
        return base64Data;
    }


    private static String extractContentFromPTag(String paragraphHtml) {
        Pattern pattern = Pattern.compile("<p[^>]*>(.*?)</p>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(paragraphHtml);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return paragraphHtml; // Fallback
    }

    private static void parseInlineImageAndText(XWPFParagraph paragraph, String content) {
        try {

            Pattern imgPattern = Pattern.compile("(<img[^>]*[/]?>)");
            String[] parts = imgPattern.split(content);
            Matcher imgMatcher = imgPattern.matcher(content);

            int partIndex = 0;
            while (partIndex < parts.length || imgMatcher.find()) {

                if (partIndex < parts.length) {
                    String textPart = parts[partIndex].trim();
                    if (!textPart.isEmpty()) {
                        parseFormattedTextToRun(paragraph, textPart);
                    }
                    partIndex++;
                }


                if (imgMatcher.find()) {
                    String imgHtml = imgMatcher.group();
                    // Resmi aynı paragrafa ekle (satır içi)
                    addInlineImageToRun(paragraph, imgHtml);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Inline resim parse hatası: " + e.getMessage());
            // Fallback: sadece metni ekle
            parseFormattedTextToRun(paragraph, stripAllHtmlTags(content));
        }
    }

    private static void parseFormattedTextToRun(XWPFParagraph paragraph, String htmlContent) {
        try {
            Log.d(TAG, "📝 Format parsing başlıyor: " + htmlContent.substring(0, Math.min(100, htmlContent.length())));


            List<FormattedTextSegment> segments = parseFormattedSegments(htmlContent);

            for (FormattedTextSegment segment : segments) {
                if (segment.text.trim().isEmpty()) continue;

                XWPFRun run = paragraph.createRun();
                run.setText(segment.text);


                run.setFontFamily(segment.fontFamily != null ? segment.fontFamily : "Calibri");
                run.setFontSize(segment.fontSize > 0 ? segment.fontSize : 11);


                if (segment.isBold) run.setBold(true);
                if (segment.isItalic) run.setItalic(true);
                if (segment.isUnderline) run.setUnderline(UnderlinePatterns.SINGLE);

                // Renk
                if (segment.color != null && !segment.color.isEmpty() && !segment.color.equals("#000000")) {
                    String colorHex = segment.color.replace("#", "");
                    if (colorHex.length() == 6) {
                        run.setColor(colorHex);
                    }
                }

                Log.d(TAG, "✅ Run oluşturuldu: '" + segment.text + "' - Bold:" + segment.isBold +
                        " Italic:" + segment.isItalic + " Color:" + segment.color + " Font:" + segment.fontFamily);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Format parsing hatası: " + e.getMessage(), e);


            XWPFRun fallbackRun = paragraph.createRun();
            fallbackRun.setText(stripAllHtmlTags(htmlContent));
            fallbackRun.setFontFamily("Calibri");
            fallbackRun.setFontSize(11);
        }
    }


    private static class FormattedTextSegment {
        String text;
        String fontFamily;
        int fontSize;
        String color;
        boolean isBold;
        boolean isItalic;
        boolean isUnderline;

        FormattedTextSegment(String text) {
            this.text = text;
            this.fontFamily = "Calibri";
            this.fontSize = 11;
            this.color = "#000000";
            this.isBold = false;
            this.isItalic = false;
            this.isUnderline = false;
        }
    }

    private static List<FormattedTextSegment> parseFormattedSegments(String htmlContent) {
        List<FormattedTextSegment> segments = new ArrayList<>();

        try {

            String content = htmlContent;


            Pattern boldPattern = Pattern.compile("(<(?:strong|b)[^>]*>)(.*?)</(?:strong|b)>", Pattern.DOTALL);
            content = processFormatTags(content, boldPattern, segments, "bold");


            Pattern italicPattern = Pattern.compile("(<(?:em|i)[^>]*>)(.*?)</(?:em|i)>", Pattern.DOTALL);
            content = processFormatTags(content, italicPattern, segments, "italic");


            Pattern underlinePattern = Pattern.compile("(<u[^>]*>)(.*?)</u>", Pattern.DOTALL);
            content = processFormatTags(content, underlinePattern, segments, "underline");


            Pattern spanPattern = Pattern.compile("(<span[^>]*style=\"([^\"]*)\">)(.*?)</span>", Pattern.DOTALL);
            content = processSpanTags(content, spanPattern, segments);


            String remainingText = stripAllHtmlTags(content).trim();
            if (!remainingText.isEmpty()) {
                segments.add(new FormattedTextSegment(remainingText));
            }

            Log.d(TAG, "📋 " + segments.size() + " format segmenti oluşturuldu");

        } catch (Exception e) {
            Log.e(TAG, "❌ Segment parsing hatası: " + e.getMessage());
            // Fallback: tek segment
            segments.add(new FormattedTextSegment(stripAllHtmlTags(htmlContent)));
        }

        return segments;
    }


    private static String processFormatTags(String content, Pattern pattern, List<FormattedTextSegment> segments, String formatType) {
        Matcher matcher = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String innerText = matcher.group(2);

            FormattedTextSegment segment = new FormattedTextSegment(stripAllHtmlTags(innerText));

            switch (formatType) {
                case "bold":
                    segment.isBold = true;
                    break;
                case "italic":
                    segment.isItalic = true;
                    break;
                case "underline":
                    segment.isUnderline = true;
                    break;
            }

            segments.add(segment);
            matcher.appendReplacement(sb, "");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }


    private static String processSpanTags(String content, Pattern pattern, List<FormattedTextSegment> segments) {
        Matcher matcher = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String styleAttr = matcher.group(2);
            String innerText = matcher.group(3);

            FormattedTextSegment segment = new FormattedTextSegment(stripAllHtmlTags(innerText));

            parseStyleAttributes(styleAttr, segment);

            segments.add(segment);
            matcher.appendReplacement(sb, "");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static void parseStyleAttributes(String styleAttr, FormattedTextSegment segment) {
        if (styleAttr == null || styleAttr.isEmpty()) return;

        try {
            String[] styles = styleAttr.split(";");

            for (String style : styles) {
                String[] parts = style.split(":");
                if (parts.length != 2) continue;

                String property = parts[0].trim().toLowerCase();
                String value = parts[1].trim();

                switch (property) {
                    case "color":
                        if (value.startsWith("#") && value.length() == 7) {
                            segment.color = value;
                            Log.d(TAG, "🎨 Renk bulundu: " + value);
                        }
                        break;

                    case "font-family":
                        segment.fontFamily = value.replaceAll("[\"']", "").split(",")[0].trim();
                        Log.d(TAG, "🔤 Font ailesi: " + segment.fontFamily);
                        break;

                    case "font-size":
                        try {
                            if (value.endsWith("pt")) {
                                segment.fontSize = Integer.parseInt(value.replace("pt", "").trim());
                            } else if (value.endsWith("px")) {
                                // Pixel'i point'e çevir (yaklaşık)
                                int px = Integer.parseInt(value.replace("px", "").trim());
                                segment.fontSize = (int)(px * 0.75); // 1px ≈ 0.75pt
                            }
                            Log.d(TAG, "📏 Font boyutu: " + segment.fontSize + "pt");
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "⚠️ Font boyutu parse edilemedi: " + value);
                        }
                        break;

                    case "font-weight":
                        if (value.equals("bold") || value.equals("700") || value.equals("bolder")) {
                            segment.isBold = true;
                            Log.d(TAG, "💪 Bold bulundu");
                        }
                        break;

                    case "font-style":
                        if (value.equals("italic")) {
                            segment.isItalic = true;
                            Log.d(TAG, "📐 Italic bulundu");
                        }
                        break;

                    case "text-decoration":
                        if (value.contains("underline")) {
                            segment.isUnderline = true;
                            Log.d(TAG, "📝 Underline bulundu");
                        }
                        break;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Style parsing hatası: " + e.getMessage());
        }
    }

    private static String extractColorFromStyle(String html) {
        try {

            Pattern colorPattern = Pattern.compile("color:[\\s]*([#a-fA-F0-9]{6,7})", Pattern.CASE_INSENSITIVE);
            Matcher colorMatcher = colorPattern.matcher(html);
            if (colorMatcher.find()) {
                String color = colorMatcher.group(1);
                Log.d(TAG, "🎨 CSS renk bulundu: " + color);
                return color;
            }


            Pattern stylePattern = Pattern.compile("style=\"[^\"]*color:[\\s]*([#a-fA-F0-9]{6,7})[^\"]*\"", Pattern.CASE_INSENSITIVE);
            Matcher styleMatcher = stylePattern.matcher(html);
            if (styleMatcher.find()) {
                String color = styleMatcher.group(1);
                Log.d(TAG, "🎨 Inline style renk bulundu: " + color);
                return color;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Renk çıkarma hatası: " + e.getMessage());
        }
        return null;
    }

    private static void addInlineImageToRun(XWPFParagraph paragraph, String imageHtml) {
        try {
            Pattern pattern = Pattern.compile("src=\"data:([^;]+);base64,([^\"]+)\"");
            Matcher matcher = pattern.matcher(imageHtml);

            if (matcher.find()) {
                String mimeType = matcher.group(1);
                String base64Data = matcher.group(2);

                byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
                int format = mimeType.contains("jpeg") ? XWPFDocument.PICTURE_TYPE_JPEG : XWPFDocument.PICTURE_TYPE_PNG;

                XWPFRun imageRun = paragraph.createRun();
                try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                    imageRun.addPicture(bis, format, "inline_image",
                            Units.toEMU(200), Units.toEMU(150)); // Küçük boyut
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Satır içi resim hatası: " + e.getMessage());
        }
    }

    private static String[] splitHtmlContent(String html) {

        java.util.List<String> parts = new java.util.ArrayList<>();

        Pattern pattern = Pattern.compile("(<p[^>]*>.*?</p>|<table[^>]*>.*?</table>|<img[^>]*[/]?>)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);

        int lastEnd = 0;
        while (matcher.find()) {

            if (matcher.start() > lastEnd) {
                String between = html.substring(lastEnd, matcher.start()).trim();
                if (!between.isEmpty()) {
                    parts.add(between);
                }
            }


            parts.add(matcher.group());
            lastEnd = matcher.end();
        }


        if (lastEnd < html.length()) {
            String remaining = html.substring(lastEnd).trim();
            if (!remaining.isEmpty()) {
                parts.add(remaining);
            }
        }

        Log.d(TAG, "📄 HTML " + parts.size() + " parçaya ayrıldı");
        return parts.toArray(new String[0]);
    }

    private static void parseHtmlToDocx(XWPFDocument document, String htmlContent) {
        try {

            String content = htmlContent;


            if (content.contains("<table")) {
                parseHtmlWithTables(document, content);
            } else {
                parseHtmlWithoutTables(document, content);
            }

        } catch (Exception e) {
            Log.e(TAG, "HTML parse hatası: " + e.getMessage());

            addPlainTextToDocument(document, stripAllHtmlTags(htmlContent));
        }
    }

    private static void parseHtmlWithTables(XWPFDocument document, String htmlContent) {

        Pattern tablePattern = Pattern.compile("<table[^>]*>.*?</table>", Pattern.DOTALL);
        Matcher tableMatcher = tablePattern.matcher(htmlContent);

        int lastEnd = 0;

        while (tableMatcher.find()) {

            String beforeTable = htmlContent.substring(lastEnd, tableMatcher.start());
            if (!beforeTable.trim().isEmpty()) {
                parseHtmlWithoutTables(document, beforeTable);
            }

            String tableHtml = tableMatcher.group();
            parseHtmlTable(document, tableHtml);

            lastEnd = tableMatcher.end();
        }

        if (lastEnd < htmlContent.length()) {
            String afterTables = htmlContent.substring(lastEnd);
            if (!afterTables.trim().isEmpty()) {
                parseHtmlWithoutTables(document, afterTables);
            }
        }
    }

    private static void parseHtmlWithoutTables(XWPFDocument document, String htmlContent) {

        Pattern pPattern = Pattern.compile("<p[^>]*>(.*?)</p>", Pattern.DOTALL);
        Matcher pMatcher = pPattern.matcher(htmlContent);

        boolean foundParagraphs = false;

        while (pMatcher.find()) {
            foundParagraphs = true;
            String paragraphContent = pMatcher.group(1);
            addFormattedParagraph(document, paragraphContent);
        }


        if (!foundParagraphs) {
            String cleanContent = stripAllHtmlTags(htmlContent);
            String[] lines = cleanContent.split("\\n|<br[^>]*>");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    addPlainTextToDocument(document, line.trim());
                }
            }
        }
    }

    private static void addFormattedParagraph(XWPFDocument document, String htmlContent) {
        XWPFParagraph paragraph = document.createParagraph();

        if (htmlContent.contains("text-align: center")) {
            paragraph.setAlignment(ParagraphAlignment.CENTER);
        } else if (htmlContent.contains("text-align: right")) {
            paragraph.setAlignment(ParagraphAlignment.RIGHT);
        } else if (htmlContent.contains("text-align: justify")) {
            paragraph.setAlignment(ParagraphAlignment.BOTH);  // JUSTIFY yerine BOTH
        }
        parseFormattedText(paragraph, htmlContent);
    }

    private static void parseFormattedText(XWPFParagraph paragraph, String htmlContent) {
        XWPFRun run = paragraph.createRun();

        boolean isBold = htmlContent.contains("<strong>") || htmlContent.contains("<b>");
        boolean isItalic = htmlContent.contains("<em>") || htmlContent.contains("<i>");
        boolean isUnderline = htmlContent.contains("<u>");


        String cleanText = stripAllHtmlTags(htmlContent).trim();
        if (cleanText.isEmpty()) {
            cleanText = " "; // Boş paragraf için
        }

        run.setText(cleanText);
        run.setFontFamily("Calibri");
        run.setFontSize(11);

        if (isBold) run.setBold(true);
        if (isItalic) run.setItalic(true);
        if (isUnderline) run.setUnderline(UnderlinePatterns.SINGLE);


        String color = extractColorFromStyle(htmlContent);
        if (color != null && !color.isEmpty()) {
            run.setColor(color.replace("#", ""));
        }
    }

    private static void parseHtmlTable(XWPFDocument document, String tableHtml) {
        try {

            Pattern rowPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL);
            Matcher rowMatcher = rowPattern.matcher(tableHtml);

            List<String[]> tableData = new java.util.ArrayList<>();
            int maxCols = 0;


            while (rowMatcher.find()) {
                String rowHtml = rowMatcher.group(1);


                Pattern cellPattern = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL);
                Matcher cellMatcher = cellPattern.matcher(rowHtml);

                java.util.List<String> rowCells = new java.util.ArrayList<>();
                while (cellMatcher.find()) {
                    String cellContent = stripAllHtmlTags(cellMatcher.group(1)).trim();
                    rowCells.add(cellContent.isEmpty() ? " " : cellContent);
                }

                if (!rowCells.isEmpty()) {
                    tableData.add(rowCells.toArray(new String[0]));
                    maxCols = Math.max(maxCols, rowCells.size());
                }
            }


            if (!tableData.isEmpty() && maxCols > 0) {
                XWPFTable table = document.createTable(tableData.size(), maxCols);
                table.setWidth("100%");

                for (int i = 0; i < tableData.size(); i++) {
                    XWPFTableRow row = table.getRow(i);
                    String[] rowData = tableData.get(i);

                    for (int j = 0; j < maxCols; j++) {
                        XWPFTableCell cell = row.getCell(j);
                        String cellText = j < rowData.length ? rowData[j] : " ";
                        cell.setText(cellText);

                        // Cell styling
                        cell.setColor("FFFFFF");
                        XWPFParagraph cellPara = cell.getParagraphs().get(0);
                        cellPara.setAlignment(ParagraphAlignment.LEFT);
                    }
                }

                Log.d(TAG, "Tablo oluşturuldu: " + tableData.size() + "x" + maxCols);
            }

        } catch (Exception e) {
            Log.e(TAG, "Tablo parse hatası: " + e.getMessage());

            addPlainTextToDocument(document, stripAllHtmlTags(tableHtml));
        }
    }


    private static void addPlainTextToDocument(XWPFDocument document, String text) {
        if (text == null || text.trim().isEmpty()) return;

        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text.trim());
        run.setFontFamily("Calibri");
        run.setFontSize(11);
    }

    private static String stripAllHtmlTags(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").trim();
    }

    private static WordContent readTextDocument(String filePath) {
        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            String finalContent = cleanHtmlContent(content.toString());
            return new WordContent(finalContent, true, null);
        } catch (IOException e) {
            Log.e(TAG, "Text dosya okuma hatası: " + e.getMessage());
            return new WordContent("", false, "Dosya okuma hatası: " + e.getMessage());
        }
    }

    private static String cleanHtmlContent(String htmlContent) {
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

    private static boolean saveDocument(XWPFDocument document, String filePath) {
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
            properties.append("📏 Dosya Boyutu: ").append(formatFileSize(file.length())).append("\n");

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
                properties.append("🏷️ Dosya Türü: ").append(getFileTypeDescription(getFileExtension(file.getName()))).append("\n");
            }


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

    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private static String getFileTypeDescription(String extension) {
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


    public static boolean updateWordDocument(String filePath, String newContent) {
        try {
            File file = new File(filePath);

            XWPFDocument document;
            if (file.exists() && filePath.toLowerCase().endsWith(".docx")) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    document = new XWPFDocument(fis);
                }

                clearDocumentContent(document);
            } else {
                document = new XWPFDocument();
            }


            if (newContent != null && !newContent.trim().isEmpty()) {
                String[] lines = newContent.split("\n");
                for (String line : lines) {
                    XWPFParagraph paragraph = document.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(line);
                    run.setFontFamily("Calibri");
                    run.setFontSize(11);
                }
            }

            return saveDocument(document, filePath);

        } catch (Exception e) {
            Log.e(TAG, "Belge güncelleme hatası: " + e.getMessage());
            return false;
        }
    }

    private static void clearDocumentContent(XWPFDocument document) {

        List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (int i = paragraphs.size() - 1; i >= 0; i--) {
            document.removeBodyElement(document.getPosOfParagraph(paragraphs.get(i)));
        }


        List<XWPFTable> tables = document.getTables();
        for (int i = tables.size() - 1; i >= 0; i--) {
            document.removeBodyElement(document.getPosOfTable(tables.get(i)));
        }
    }

    public static boolean addTable(String filePath, int rows, int cols) {
        try {
            File file = new File(filePath);
            XWPFDocument document;

            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    document = new XWPFDocument(fis);
                }
            } else {
                document = new XWPFDocument();
            }

            XWPFTable table = document.createTable(rows, cols);

            // Tablo stilini ayarla
            table.setWidth("100%");

            for (int i = 0; i < rows; i++) {
                XWPFTableRow row = table.getRow(i);
                for (int j = 0; j < cols; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    cell.setText("Hücre " + (i + 1) + "-" + (j + 1));
                }
            }

            return saveDocument(document, filePath);

        } catch (Exception e) {
            Log.e(TAG, "Tablo ekleme hatası: " + e.getMessage());
            return false;
        }
    }

    public static boolean saveSpannableToDocx(String filePath, Spannable spannable) {
        try {
            XWPFDocument document = new XWPFDocument();

            String text = spannable.toString();
            String[] lines = text.split("\n");

            for (String line : lines) {
                if (line.trim().isEmpty()) {

                    document.createParagraph();
                    continue;
                }

                XWPFParagraph paragraph = document.createParagraph();


                if (isTableLine(line)) {
                    createTableParagraph(paragraph, line);
                } else {

                    createFormattedRun(paragraph, line, spannable, getLineStartIndex(text, line));
                }
            }

            return saveDocument(document, filePath);

        } catch (Exception e) {
            Log.e(TAG, "Spannable kaydetme hatası: " + e.getMessage());
            return false;
        }
    }


    private static boolean isTableLine(String line) {
        return line.contains("│") || line.contains("┌") || line.contains("├") ||
                line.contains("└") || line.contains("┐") || line.contains("┤") ||
                line.contains("┘") || line.contains("─");
    }


    private static void createTableParagraph(XWPFParagraph paragraph, String line) {
        XWPFRun run = paragraph.createRun();
        run.setText(line);
        run.setFontFamily("Courier New");
        run.setFontSize(10);
        run.setColor("404040");
    }

    private static int getLineStartIndex(String fullText, String line) {
        int index = fullText.indexOf(line);
        return index != -1 ? index : 0;
    }


    private static void createFormattedRun(XWPFParagraph paragraph, String text, Spannable spannable, int startIndex) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontFamily("Calibri");
        run.setFontSize(11);

        int endIndex = startIndex + text.length();

        Object[] spans = spannable.getSpans(startIndex, endIndex, Object.class);

        for (Object span : spans) {
            if (span instanceof StyleSpan) {
                StyleSpan styleSpan = (StyleSpan) span;
                if (styleSpan.getStyle() == android.graphics.Typeface.BOLD) {
                    run.setBold(true);
                } else if (styleSpan.getStyle() == android.graphics.Typeface.ITALIC) {
                    run.setItalic(true);
                }
            } else if (span instanceof UnderlineSpan) {
                run.setUnderline(UnderlinePatterns.SINGLE);
            } else if (span instanceof ForegroundColorSpan) {
                ForegroundColorSpan colorSpan = (ForegroundColorSpan) span;
                int color = colorSpan.getForegroundColor();
                String hexColor = String.format("%06X", (0xFFFFFF & color));
                run.setColor(hexColor);
            } else if (span instanceof AbsoluteSizeSpan) {
                AbsoluteSizeSpan sizeSpan = (AbsoluteSizeSpan) span;
                int sizeInPx = sizeSpan.getSize();

                int sizeInPt = (int) (sizeInPx * 72 / 96);
                run.setFontSize(Math.max(8, Math.min(72, sizeInPt)));
            } else if (span instanceof TypefaceSpan) {
                TypefaceSpan typefaceSpan = (TypefaceSpan) span;
                String fontFamily = typefaceSpan.getFamily();
                if (fontFamily != null) {
                    switch (fontFamily) {
                        case "serif":
                            run.setFontFamily("Times New Roman");
                            break;
                        case "sans-serif":
                            run.setFontFamily("Arial");
                            break;
                        case "monospace":
                            run.setFontFamily("Courier New");
                            break;
                        case "cursive":
                            run.setFontFamily("Comic Sans MS");
                            break;
                        default:
                            run.setFontFamily("Calibri");
                            break;
                    }
                }
            }
        }
    }
}