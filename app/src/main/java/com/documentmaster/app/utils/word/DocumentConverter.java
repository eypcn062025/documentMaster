package com.documentmaster.app.utils.word;

import android.util.Base64;
import android.util.Log;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentConverter {

    private static final String TAG = "DocumentConverter";

    public static String convertParagraphToHtmlWithImages(XWPFParagraph paragraph) {
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
            html.append(DocumentUtils.escapeHtml(paragraph.getText()));
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

    public static String convertPictureToHtml(XWPFPicture picture) {
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

    public static String convertRunToHtml(XWPFRun run) {
        String text = run.getText(0);
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();

        if (run.isBold()) {
            html.append("<strong>");
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

        html.append(DocumentUtils.escapeHtml(text));

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

    public static String convertTableToHtml(XWPFTable table) {
        StringBuilder html = new StringBuilder();
        html.append("<table border=\"1\" style=\"border-collapse: collapse; width: 100%; margin: 10px 0;\">");

        List<XWPFTableRow> rows = table.getRows();
        for (XWPFTableRow row : rows) {
            html.append("<tr>");
            List<XWPFTableCell> cells = row.getTableCells();
            for (XWPFTableCell cell : cells) {
                html.append("<td style=\"padding: 8px; border: 1px solid #333;\">");

                List<XWPFParagraph> cellParagraphs = cell.getParagraphs();
                for (int i = 0; i < cellParagraphs.size(); i++) {
                    XWPFParagraph para = cellParagraphs.get(i);
                    String cellText = para.getText().trim();
                    if (!cellText.isEmpty()) {
                        html.append(DocumentUtils.escapeHtml(cellText));
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

    public static void parseHtmlToDocxAdvanced(XWPFDocument document, String htmlContent) {
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
                    addPlainTextToDocument(document, DocumentUtils.stripAllHtmlTags(part));
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
            addPlainTextToDocument(document, DocumentUtils.stripAllHtmlTags(htmlContent));
        }
    }

    public static String[] splitHtmlContent(String html) {
        List<String> parts = new ArrayList<>();

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

    public static void parseParagraphToDocx(XWPFDocument document, String paragraphHtml) {
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

            String content = extractContentFromPTag(paragraphHtml);

            if (content.contains("<img")) {
                parseInlineImageAndText(paragraph, content);
            } else {
                parseFormattedTextToRun(paragraph, content);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Paragraf parse hatası: " + e.getMessage());
            addPlainTextToDocument(document, DocumentUtils.stripAllHtmlTags(paragraphHtml));
        }
    }

    public static void parseImageToDocx(XWPFDocument document, String imageHtml) {
        try {
            Log.d(TAG, "🖼️ Resim DOCX'e ekleniyor...");

            Pattern pattern = Pattern.compile("src=\"data:([^;]+);base64,([^\"\\s]+)\"");
            Matcher matcher = pattern.matcher(imageHtml);

            if (matcher.find()) {
                String mimeType = matcher.group(1).trim();
                String base64Data = matcher.group(2).trim();

                Log.d(TAG, "📷 MIME Type: " + mimeType);
                Log.d(TAG, "📊 Base64 uzunluğu: " + base64Data.length());

                String cleanBase64 = DocumentUtils.cleanBase64Data(base64Data);
                Log.d(TAG, "🧹 Temizlenmiş Base64 uzunluğu: " + cleanBase64.length());

                if (cleanBase64.isEmpty()) {
                    Log.e(TAG, "❌ Base64 verisi temizlendikten sonra boş kaldı");
                    addImagePlaceholder(document, "Base64 verisi boş");
                    return;
                }

                byte[] imageBytes;
                try {
                    imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                    Log.d(TAG, "✅ Base64 decode başarılı - Byte uzunluğu: " + imageBytes.length);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "❌ Base64 decode hatası: " + e.getMessage());
                    imageBytes = DocumentUtils.tryAlternativeBase64Decode(cleanBase64);
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

                int format = determinePOIImageFormat(mimeType, imageBytes);
                Log.d(TAG, "📎 POI format: " + format);

                XWPFParagraph imageParagraph = document.createParagraph();
                imageParagraph.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun imageRun = imageParagraph.createRun();

                try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                    int[] dimensions = DocumentUtils.calculateImageDimensions(imageBytes, 400, 300);

                    imageRun.addPicture(bis, format, "document_image",
                            Units.toEMU(dimensions[0]), Units.toEMU(dimensions[1]));

                    Log.d(TAG, "✅ Resim başarıyla DOCX'e eklendi - Boyut: " + imageBytes.length +
                            " bytes, Dimensions: " + dimensions[0] + "x" + dimensions[1]);
                }

            } else {
                Log.w(TAG, "⚠️ Resimde Base64 data pattern bulunamadı");
                addImagePlaceholder(document, "Base64 pattern bulunamadı");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Resim DOCX ekleme hatası: " + e.getMessage(), e);
            addImagePlaceholder(document, "Resim işleme hatası: " + e.getMessage());
        }
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

        // Byte signature kontrolü
        if (imageBytes.length >= 4) {
            if (imageBytes[0] == (byte)0x89 && imageBytes[1] == 0x50 &&
                    imageBytes[2] == 0x4E && imageBytes[3] == 0x47) {
                return XWPFDocument.PICTURE_TYPE_PNG;
            }
            if (imageBytes[0] == (byte)0xFF && imageBytes[1] == (byte)0xD8 &&
                    imageBytes[2] == (byte)0xFF) {
                return XWPFDocument.PICTURE_TYPE_JPEG;
            }
            if (imageBytes[0] == 0x47 && imageBytes[1] == 0x49 && imageBytes[2] == 0x46) {
                return XWPFDocument.PICTURE_TYPE_GIF;
            }
        }

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

    private static String extractContentFromPTag(String paragraphHtml) {
        Pattern pattern = Pattern.compile("<p[^>]*>(.*?)</p>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(paragraphHtml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return paragraphHtml;
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
                    addInlineImageToRun(paragraph, imgHtml);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Inline resim parse hatası: " + e.getMessage());
            parseFormattedTextToRun(paragraph, DocumentUtils.stripAllHtmlTags(content));
        }
    }

    private static void parseFormattedTextToRun(XWPFParagraph paragraph, String htmlContent) {
        try {
            XWPFRun run = paragraph.createRun();
            String cleanText = DocumentUtils.stripAllHtmlTags(htmlContent).trim();
            if (cleanText.isEmpty()) {
                cleanText = " ";
            }

            run.setText(cleanText);

            // Varsayılan font ayarları
            String fontFamily = "Calibri";
            int fontSize = 11;
            String color = null;

            // Bold, italic, underline kontrolü
            boolean isBold = htmlContent.contains("<strong>") || htmlContent.contains("<b>");
            boolean isItalic = htmlContent.contains("<em>") || htmlContent.contains("<i>");
            boolean isUnderline = htmlContent.contains("<u>");

            // Style attribute'dan font bilgilerini çıkar
            if (htmlContent.contains("style=")) {
                // font-family kontrolü
                if (htmlContent.contains("font-family:")) {
                    int start = htmlContent.indexOf("font-family:") + 12;
                    int end = htmlContent.indexOf(";", start);
                    if (end == -1) end = htmlContent.indexOf("\"", start);
                    if (end == -1) end = htmlContent.indexOf("'", start);
                    if (end > start) {
                        fontFamily = htmlContent.substring(start, end).trim().replaceAll("[\"';]", "");
                    }
                }

                // font-size kontrolü
                if (htmlContent.contains("font-size:")) {
                    int start = htmlContent.indexOf("font-size:") + 10;
                    int end = htmlContent.indexOf("pt", start);
                    if (end == -1) end = htmlContent.indexOf(";", start);
                    if (end == -1) end = htmlContent.indexOf("\"", start);
                    if (end > start) {
                        try {
                            String sizeStr = htmlContent.substring(start, end).trim().replaceAll("[^0-9]", "");
                            if (!sizeStr.isEmpty()) {
                                fontSize = Integer.parseInt(sizeStr);
                            }
                        } catch (Exception e) {
                            fontSize = 11; // varsayılan
                        }
                    }
                }

                // color kontrolü
                if (htmlContent.contains("color:")) {
                    int start = htmlContent.indexOf("color:") + 6;
                    int end = htmlContent.indexOf(";", start);
                    if (end == -1) end = htmlContent.indexOf("\"", start);
                    if (end == -1) end = htmlContent.indexOf("'", start);
                    if (end > start) {
                        color = htmlContent.substring(start, end).trim();
                        if (color.startsWith("#")) {
                            color = color.substring(1); // # işaretini kaldır
                        }
                        if (color.equals("auto") || color.isEmpty()) {
                            color = null;
                        }
                    }
                }
            }

            // Formatları uygula
            run.setFontFamily(fontFamily);
            run.setFontSize(fontSize);
            if (color != null) {
                run.setColor(color);
            }

            if (isBold) run.setBold(true);
            if (isItalic) run.setItalic(true);
            if (isUnderline) run.setUnderline(UnderlinePatterns.SINGLE);

        } catch (Exception e) {
            Log.e(TAG, "❌ Format parsing hatası: " + e.getMessage(), e);
            XWPFRun fallbackRun = paragraph.createRun();
            fallbackRun.setText(DocumentUtils.stripAllHtmlTags(htmlContent));
            fallbackRun.setFontFamily("Calibri");
            fallbackRun.setFontSize(11);
        }
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
                            Units.toEMU(200), Units.toEMU(150));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Satır içi resim hatası: " + e.getMessage());
        }
    }

    public static void parseHtmlTable(XWPFDocument document, String tableHtml) {
        try {
            Pattern rowPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL);
            Matcher rowMatcher = rowPattern.matcher(tableHtml);

            List<String[]> tableData = new ArrayList<>();
            int maxCols = 0;

            while (rowMatcher.find()) {
                String rowHtml = rowMatcher.group(1);
                Pattern cellPattern = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL);
                Matcher cellMatcher = cellPattern.matcher(rowHtml);

                List<String> rowCells = new ArrayList<>();
                while (cellMatcher.find()) {
                    String cellContent = DocumentUtils.stripAllHtmlTags(cellMatcher.group(1)).trim();
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
                        cell.setColor("FFFFFF");
                        XWPFParagraph cellPara = cell.getParagraphs().get(0);
                        cellPara.setAlignment(ParagraphAlignment.LEFT);
                    }
                }
                Log.d(TAG, "Tablo oluşturuldu: " + tableData.size() + "x" + maxCols);
            }
        } catch (Exception e) {
            Log.e(TAG, "Tablo parse hatası: " + e.getMessage());
            addPlainTextToDocument(document, DocumentUtils.stripAllHtmlTags(tableHtml));
        }
    }

    public static void addPlainTextToDocument(XWPFDocument document, String text) {
        if (text == null || text.trim().isEmpty()) return;

        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text.trim());
        run.setFontFamily("Calibri");
        run.setFontSize(11);
    }
}