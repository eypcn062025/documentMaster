package com.documentmaster.app.web;

public interface WebAppCallback {
    void onHtmlChanged(String newHtml);
    void onEditorFocused();
    void onEditorBlurred();
}