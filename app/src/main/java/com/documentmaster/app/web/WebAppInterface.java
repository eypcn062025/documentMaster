package com.documentmaster.app.web;

import android.webkit.JavascriptInterface;
import android.os.Handler;
import android.os.Looper;

public class WebAppInterface {

    private final WebAppCallback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public WebAppInterface(WebAppCallback callback) {
        this.callback = callback;
    }

    @JavascriptInterface
    public void onTextChanged(String html) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onHtmlChanged(html);
            }
        });
    }



}
