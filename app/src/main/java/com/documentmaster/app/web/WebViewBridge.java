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

    public void destroyWebView() {
        if (webView != null) {
            try {
                webView.loadUrl("about:blank");
                webView.destroy();
            } catch (Exception e) {
            }
        }
    }
}