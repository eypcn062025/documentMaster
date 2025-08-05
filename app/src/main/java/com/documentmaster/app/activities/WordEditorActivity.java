package com.documentmaster.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.documentmaster.app.BaseActivity;
import com.documentmaster.app.R;
import com.documentmaster.app.image.ImageManager;
import com.documentmaster.app.utils.DocumentLoadManager;
import com.documentmaster.app.utils.DocumentSaveManager;
import com.documentmaster.app.utils.EditorDialogs;
import com.documentmaster.app.utils.HtmlUtils;
import com.documentmaster.app.utils.word.WordDocumentHelper;
import com.documentmaster.app.web.WebAppCallback;
import com.documentmaster.app.web.WebViewBridge;
import com.documentmaster.app.web.WebViewManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WordEditorActivity extends BaseActivity implements WebAppCallback, ImageManager.ImageOperationCallback , DocumentSaveManager.SaveCallback
, DocumentLoadManager.LoadCallback{

    // UI Components
    private Toolbar toolbar;
    private WebView webView;
    private WebViewManager webViewManager;
    private WebViewBridge webViewBridge;
    private MaterialButton btnSave, btnSaveAs, btnFormat, btnFont, btnInsertTable;
    private ProgressBar progressBar;

    // Formatting toolbar
    private MaterialButton btnBold, btnItalic, btnUnderline, btnColor, btnSize;
    private MaterialButton btnAlignLeft, btnAlignCenter, btnAlignRight;
    private MaterialButton btnOrderedList, btnUnorderedList, btnUndo, btnRedo;
    private View formattingToolbar;

    // Data
    private String currentFilePath;
    private String originalContent;
    private boolean isContentChanged = false;
    private boolean isNewDocument = false;
    private boolean isWebViewLoaded = false;

    private ExecutorService executorService;

    // Constants
    public static final String EXTRA_FILE_PATH = "extra_file_path";
    public static final String EXTRA_IS_NEW_DOCUMENT = "extra_is_new_document";

    private MaterialButton btnInsertImage, btnDeleteImage, btnImageInfo;
    private ImageManager imageManager;
    private EditorDialogs editorDialogs;
    private DocumentSaveManager saveManager;
    private DocumentLoadManager loadManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_editor_offline);

        initViews();
        setupToolbar();
        setupFormattingToolbar();
        setupListeners();
        webViewManager = new WebViewManager(this, webView);
        webViewBridge = new WebViewBridge(webView);
        imageManager = new ImageManager(this, webViewBridge);
        imageManager.setCallback(this);
        editorDialogs = new EditorDialogs(this, webViewBridge);
        webViewManager.setup(() -> {
            webViewBridge.setEditorLoaded(true);
            isWebViewLoaded = true;
            if (!TextUtils.isEmpty(originalContent)) {
                loadManager.setEditorContent(originalContent, isWebViewLoaded);
            }
        });
        executorService = Executors.newFixedThreadPool(2);
        saveManager = new DocumentSaveManager(this, webViewBridge, executorService, this);
        loadManager = new DocumentLoadManager(this, webViewBridge, executorService, this);
        handleIntent();

    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        webView = findViewById(R.id.webView);
        btnSave = findViewById(R.id.btnSave);
        btnSaveAs = findViewById(R.id.btnSaveAs);
        btnFormat = findViewById(R.id.btnFormat);
        btnFont = findViewById(R.id.btnFont);
        btnInsertTable = findViewById(R.id.btnInsertTable);
        progressBar = findViewById(R.id.progressBar);

        formattingToolbar = findViewById(R.id.formattingToolbar);
        btnBold = findViewById(R.id.btnBold);
        btnItalic = findViewById(R.id.btnItalic);
        btnUnderline = findViewById(R.id.btnUnderline);
        btnColor = findViewById(R.id.btnColor);
        btnSize = findViewById(R.id.btnSize);
        btnAlignLeft = findViewById(R.id.btnAlignLeft);
        btnAlignCenter = findViewById(R.id.btnAlignCenter);
        btnAlignRight = findViewById(R.id.btnAlignRight);
        btnOrderedList = findViewById(R.id.btnOrderedList);
        btnUnorderedList = findViewById(R.id.btnUnorderedList);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);

        btnInsertImage = findViewById(R.id.btnInsertImage);
        btnDeleteImage = findViewById(R.id.btnDeleteImage);
        btnImageInfo = findViewById(R.id.btnImageInfo);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public void onHtmlChanged(String newHtml) {
        if (!isContentChanged && !newHtml.equals(originalContent)) {
            isContentChanged = true;
            updateTitle();
        }
    }

    @Override
    public void onLoadCompleted(boolean success, String content, String fileName, String errorMessage) {
        showProgress(false);
        if (success) {
            originalContent = content;
            if (isWebViewLoaded) {
                loadManager.setEditorContent(originalContent, isWebViewLoaded);
            }
            getSupportActionBar().setTitle(fileName);
        } else {
            finish();
        }
    }
    private void setupFormattingToolbar() {
        formattingToolbar.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveManager.saveDocument(currentFilePath, isNewDocument));
        btnSaveAs.setOnClickListener(v -> saveManager.saveDocumentAs(currentFilePath));
        btnFormat.setOnClickListener(v -> toggleFormattingToolbar());
        btnFont.setOnClickListener(v -> editorDialogs.showFontDialog());
        btnInsertTable.setOnClickListener(v -> editorDialogs.showInsertTableDialog());
        btnBold.setOnClickListener(v -> webViewBridge.executeJS("setBold()"));
        btnItalic.setOnClickListener(v -> webViewBridge.executeJS("setItalic()"));
        btnUnderline.setOnClickListener(v -> webViewBridge.executeJS("setUnderline()"));
        btnColor.setOnClickListener(v -> editorDialogs.showColorPickerDialog());
        btnSize.setOnClickListener(v -> editorDialogs.showFontSizeDialog());
        btnAlignLeft.setOnClickListener(v -> webViewBridge.executeJS("setAlignLeft()"));
        btnAlignCenter.setOnClickListener(v -> webViewBridge.executeJS("setAlignCenter()"));
        btnAlignRight.setOnClickListener(v -> webViewBridge.executeJS("setAlignRight()"));
        btnOrderedList.setOnClickListener(v -> webViewBridge.executeJS("setOrderedList()"));
        btnUnorderedList.setOnClickListener(v -> webViewBridge.executeJS("setUnorderedList()"));
        btnUndo.setOnClickListener(v -> webViewBridge.executeJS("undo()"));
        btnRedo.setOnClickListener(v -> webViewBridge.executeJS("redo()"));
        btnInsertImage.setOnClickListener(v -> imageManager.showImageInsertOptions());
        btnImageInfo.setOnClickListener(v -> imageManager.showImageInfo());
    }

    @Override
    public void onImageInserted() {
        notifyContentChange();
    }

    @Override
    public String getCurrentFileName() {
        if (currentFilePath != null) {
            return new File(currentFilePath).getName();
        }
        return "Yeni Belge";
    }

    @Override
    public boolean isContentChanged() {
        return this.isContentChanged;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (imageManager.handleActivityResult(requestCode, resultCode, data)) {
            return; // ImageManager başarıyla işledi
        }
    }

    private void notifyContentChange() {
        if (!isContentChanged) {
            isContentChanged = true;
            updateTitle();

            webViewBridge.getImageCount(count ->
                    Log.d("WordEditor", "📊 Güncel resim sayısı: " + count)
            );
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        currentFilePath = intent.getStringExtra(EXTRA_FILE_PATH);
        isNewDocument = intent.getBooleanExtra(EXTRA_IS_NEW_DOCUMENT, false);

        if (isNewDocument) {
            getSupportActionBar().setTitle("Yeni Word Belgesi");
            originalContent = "";
        } else if (!TextUtils.isEmpty(currentFilePath)) {
            loadManager.loadDocument(currentFilePath);
        } else {
            Toast.makeText(this, "Dosya yolu bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void toggleFormattingToolbar() {
        if (formattingToolbar.getVisibility() == View.VISIBLE) {
            formattingToolbar.setVisibility(View.GONE);
            btnFormat.setText("Format");
        } else {
            formattingToolbar.setVisibility(View.VISIBLE);
            btnFormat.setText("Gizle");
        }
    }

    private void updateTitle() {
        String title = getSupportActionBar().getTitle().toString();
        if (isContentChanged && !title.endsWith(" *")) {
            getSupportActionBar().setTitle(title + " *");
        } else if (!isContentChanged && title.endsWith(" *")) {
            getSupportActionBar().setTitle(title.substring(0, title.length() - 2));
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        webView.setEnabled(!show);
        btnSave.setEnabled(!show);
        btnSaveAs.setEnabled(!show);
        btnFormat.setEnabled(!show);
        btnFont.setEnabled(!show);
        btnInsertTable.setEnabled(!show);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.word_editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_properties) {
            showDocumentProperties();
            return true;
        } else if (id == R.id.action_word_count) {
            showWordCount();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDocumentProperties() {
        if (currentFilePath != null && !isNewDocument) {
            String properties = WordDocumentHelper.getDocumentProperties(currentFilePath);
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Belge Özellikleri")
                    .setPositiveButton("Tamam", null)
                    .show();
        } else {
            Toast.makeText(this, "Belge henüz kaydedilmedi", Toast.LENGTH_SHORT).show();
        }
    }

    private void showWordCount() {
        webViewBridge.getPlainText(result -> {
            String textContent = HtmlUtils.cleanHtmlResult(result);

            String[] words = textContent.trim().split("\\s+");
            int wordCount = textContent.trim().isEmpty() ? 0 : words.length;
            int charCount = textContent.length();
            int charCountNoSpaces = textContent.replaceAll("\\s", "").length();

            String message = "📖 Kelime Sayısı: " + wordCount + "\n" +
                    "🔤 Karakter Sayısı: " + charCount + "\n" +
                    "🔠 Boşluksuz Karakter: " + charCountNoSpaces;

            new MaterialAlertDialogBuilder(this)
                    .setTitle("İstatistikler")
                    .setMessage(message)
                    .setPositiveButton("Tamam", null)
                    .show();
        });
    }

    @Override
    public void onBackPressed() {
        if (isContentChanged) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Kaydedilmemiş Değişiklikler")
                    .setMessage("Değişiklikler kaydedilmedi. Çıkmak istediğinizden emin misiniz?")
                    .setPositiveButton("Kaydet", (dialog, which) -> {
                        saveManager.saveDocument(currentFilePath, isNewDocument);
                        finish();
                    })
                    .setNegativeButton("Kaydetme", (dialog, which) -> super.onBackPressed())
                    .setNeutralButton("İptal", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isContentChanged && !isNewDocument && currentFilePath != null) {
            saveManager.saveDocument(currentFilePath, false);
        }
    }

    @Override
    protected void onDestroy() {
        if (executorService != null && !executorService.isShutdown()) {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        webViewBridge.destroyWebView();
        saveManager = null;
        loadManager = null;
        super.onDestroy();

    }
    @Override
    public void onSaveStarted() {
        showProgress(true);
    }
    @Override
    public void onSaveCompleted(boolean success, String message) {
        showProgress(false);
        Toast.makeText(this, message, success ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
    }

    @Override
    public void onContentChanged(boolean isChanged) {
        this.isContentChanged = isChanged;
        updateTitle();
    }
    @Override
    public void onFilePathChanged(String newFilePath, String fileName) {
        this.currentFilePath = newFilePath;
        this.isNewDocument = false;
        getSupportActionBar().setTitle(fileName);
    }
}