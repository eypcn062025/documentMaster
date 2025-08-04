package com.documentmaster.app.web;

import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WebViewBridge {
    private boolean editorLoaded = false;

    public void setEditorLoaded(boolean loaded) {
        this.editorLoaded = loaded;
    }
    private final WebView webView;

    public WebViewBridge(WebView webView) {
        this.webView = webView;
    }

    public void execute(String jsCode) {
        if (webView != null) {
            webView.evaluateJavascript(jsCode, null);
        }
    }

    public void getHtml(HtmlCallback callback) {
        webView.evaluateJavascript("getHtml()", result -> {
            if (result != null) {
                callback.onHtmlReady(result);
            } else {
                callback.onHtmlReady("");
            }
        });
    }
    public void getPlainText(HtmlCallback callback) {
        webView.evaluateJavascript("getText()", result -> {
            if (result != null) {
                callback.onHtmlReady(result);
            } else {
                callback.onHtmlReady("");
            }
        });
    }
    public void setHtml(String html) {
        String js = "setHtml(`" + escapeHtml(html) + "`)";
        execute(js);
    }

    public void insertImage(String base64Data, String mimeType, String imageName, Runnable onSuccess, Runnable onError) {
        String jsCommand = String.format(
                "insertImageFromAndroid('%s', '%s', '%s')",
                base64Data, mimeType, imageName
        );

        webView.evaluateJavascript(jsCommand, result -> {
            Log.d("WebViewBridge", "🖼️ insertImage result: " + result);
            if ("true".equals(result)) {
                if (onSuccess != null) onSuccess.run();
            } else {
                if (onError != null) onError.run();
            }
        });
    }

    private String cleanResult(String result) {
        if (result == null) return "";
        return result.replaceAll("^\"|\"$", "").replace("\\n", "\n").replace("\\\"", "\"");
    }


    private String escapeHtml(String html) {
        if (html == null) return "";
        return html.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("${", "\\${");
    }

    public void getImageCount(ValueCallback<Integer> callback) {
        webView.evaluateJavascript("getImageCountFromAndroid()", result -> {
            try {
                int count = Integer.parseInt(result);
                if (callback != null) callback.onReceiveValue(count);
            } catch (Exception e) {
                if (callback != null) callback.onReceiveValue(-1);
            }
        });
    }
    public void getImageInfo(BiConsumer<Integer, String> callback) {
        webView.evaluateJavascript("getImageCountFromAndroid()", countResult -> {
            try {
                int count = Integer.parseInt(countResult);

                webView.evaluateJavascript("validateImagesFromAndroid()", validationResult -> {
                    if (callback != null) callback.accept(count, validationResult);
                });

            } catch (Exception e) {
                if (callback != null) callback.accept(-1, "Hata");
            }
        });
    }

    public void validateImageIntegrity() {
        webView.evaluateJavascript("validateImageIntegrity()", null);
    }
    public void executeJS(String jsCommand) {
        if (editorLoaded && webView != null) {
            webView.evaluateJavascript(jsCommand, null);
        } else {
            Log.w("WebViewBridge", "⚠️ WebView hazır değil, JS çalıştırılamadı: " + jsCommand);
        }
    }
    public interface HtmlCallback {
        void onHtmlReady(String html);
    }

    public void getBestEffortHtml(HtmlCallback callback) {
        webView.evaluateJavascript("getHtmlWithFormats()", formatResult -> {
            if (formatResult != null && !"null".equals(formatResult)) {
                Log.d("WebViewBridge", "✅ Formatlı HTML alındı");
                callback.onHtmlReady(formatResult);
            } else {
                Log.w("WebViewBridge", "⚠️ Formatlı HTML başarısız, resimli deneniyor...");
                webView.evaluateJavascript("getHtmlWithImages()", imageResult -> {
                    if (imageResult != null && !"null".equals(imageResult)) {
                        Log.d("WebViewBridge", "✅ Resimli HTML alındı");
                        callback.onHtmlReady(imageResult);
                    } else {
                        Log.w("WebViewBridge", "⚠️ Resimli HTML başarısız, düz HTML deneniyor...");
                        webView.evaluateJavascript("getHtml()", plainResult -> {
                            Log.d("WebViewBridge", "✅ Düz HTML alındı");
                            callback.onHtmlReady(plainResult);
                        });
                    }
                });
            }
        });
    }
    public void destroyWebView() {
        if (webView != null) {
            try {
                webView.loadUrl("about:blank");
                webView.destroy();
                Log.d("WebViewBridge", "🌐 WebView temizlendi");
            } catch (Exception e) {
                Log.e("WebViewBridge", "❌ WebView temizleme hatası: " + e.getMessage());
            }
        }
    }
}