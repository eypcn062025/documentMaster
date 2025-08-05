package com.documentmaster.app.utils.word;

import android.text.Spannable;
import android.text.SpannableString;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class WordDocumentHelper {

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

    public static WordContent readWordDocument(String filePath) {
        return WordDocumentReader.readWordDocument(filePath);
    }

    public static String getDocumentProperties(String filePath) {
        return WordDocumentReader.getDocumentProperties(filePath);
    }

    public static boolean isValidWordDocument(String filePath) {
        return WordDocumentReader.isValidWordDocument(filePath);
    }


    public static boolean saveHtmlToDocx(String filePath, String htmlContent) {
        return WordDocumentWriter.saveHtmlToDocx(filePath, htmlContent);
    }

    public static boolean createWordDocument(String filePath, String content) {
        return WordDocumentWriter.createWordDocument(filePath, content);
    }

}