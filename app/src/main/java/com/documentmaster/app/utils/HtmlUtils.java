package com.documentmaster.app.utils;

import android.util.Log;

public class HtmlUtils {
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
        return cleaned;
    }
}
