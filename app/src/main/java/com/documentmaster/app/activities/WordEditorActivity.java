package com.documentmaster.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.documentmaster.app.BaseActivity;
import com.documentmaster.app.R;
import com.documentmaster.app.image.ImageManager;
import com.documentmaster.app.image.ImageProcessor;
import com.documentmaster.app.utils.FileUtils;
import com.documentmaster.app.utils.HtmlUtils;
import com.documentmaster.app.utils.WordDocumentHelper;
import com.documentmaster.app.web.WebAppCallback;
import com.documentmaster.app.web.WebViewBridge;
import com.documentmaster.app.web.WebViewManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

public class WordEditorActivity extends BaseActivity implements WebAppCallback, ImageManager.ImageOperationCallback {

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

    // Formatting state
    private String currentFontFamily = "Arial";
    private int currentFontSize = 14;
    private String currentTextColor = "#000000";

    // Threading
    private ExecutorService executorService;

    // Constants
    public static final String EXTRA_FILE_PATH = "extra_file_path";
    public static final String EXTRA_IS_NEW_DOCUMENT = "extra_is_new_document";

    private MaterialButton btnInsertImage, btnDeleteImage, btnImageInfo;
    private ImageManager imageManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_editor_offline);

        initViews();
        setupToolbar();
        webViewManager = new WebViewManager(this, webView);
        webViewBridge = new WebViewBridge(webView);
        imageManager = new ImageManager(this, webViewBridge);
        imageManager.setCallback(this);
        webViewManager.setup(() -> {
            webViewBridge.setEditorLoaded(true);
            isWebViewLoaded=true;
            if (!TextUtils.isEmpty(originalContent)) {
                webViewBridge.setHtml(originalContent);
            }
        });
        setupFormattingToolbar();
        setupListeners();

        executorService = Executors.newFixedThreadPool(2);
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
    public void onEditorFocused() {

    }
    @Override
    public void onEditorBlurred() {

    }
    private void setupFormattingToolbar() {
        formattingToolbar.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveDocument());
        btnSaveAs.setOnClickListener(v -> saveDocumentAs());
        btnFormat.setOnClickListener(v -> toggleFormattingToolbar());
        btnFont.setOnClickListener(v -> showFontDialog());
        btnInsertTable.setOnClickListener(v -> showInsertTableDialog());

        // Formatting buttons
        btnBold.setOnClickListener(v -> webViewBridge.executeJS("setBold()"));
        btnItalic.setOnClickListener(v -> webViewBridge.executeJS("setItalic()"));
        btnUnderline.setOnClickListener(v -> webViewBridge.executeJS("setUnderline()"));
        btnColor.setOnClickListener(v -> showColorPickerDialog());
        btnSize.setOnClickListener(v -> showFontSizeDialog());

        // Alignment buttons
        btnAlignLeft.setOnClickListener(v -> webViewBridge.executeJS("setAlignLeft()"));
        btnAlignCenter.setOnClickListener(v -> webViewBridge.executeJS("setAlignCenter()"));
        btnAlignRight.setOnClickListener(v -> webViewBridge.executeJS("setAlignRight()"));

        // List buttons
        btnOrderedList.setOnClickListener(v -> webViewBridge.executeJS("setOrderedList()"));
        btnUnorderedList.setOnClickListener(v -> webViewBridge.executeJS("setUnorderedList()"));

        // Undo/Redo
        btnUndo.setOnClickListener(v -> webViewBridge.executeJS("undo()"));
        btnRedo.setOnClickListener(v -> webViewBridge.executeJS("redo()"));
        // Resim butonları
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
            loadDocument();
        } else {
            Toast.makeText(this, "Dosya yolu bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadDocument() {
        if (executorService == null || TextUtils.isEmpty(currentFilePath)) {
            Toast.makeText(this, "Dosya yolu hatası", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showProgress(true);

        executorService.execute(() -> {
            try {
                WordDocumentHelper.WordContent result = WordDocumentHelper.readWordDocument(currentFilePath);

                runOnUiThread(() -> {
                    showProgress(false);

                    if (result.isSuccess()) {
                        originalContent = HtmlUtils.convertToHtml(result.getContent());

                        if (isWebViewLoaded) {
                            setEditorContent(originalContent);
                        }

                        File file = new File(currentFilePath);
                        getSupportActionBar().setTitle(file.getName());

                        Toast.makeText(this, "Belge yüklendi", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Belge yüklenemedi: " + result.getError(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(this, "Yükleme hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void setEditorContent(String content) {
        if (!isWebViewLoaded || content == null) {
            Log.d("WordEditor", "WebView hazır değil veya içerik null");
            return;
        }

        try {

            String cleanContent = HtmlUtils.normalizeHtmlForEditor(content);

            String jsContent = cleanContent
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            String jsCommand = "setHtml('" + jsContent + "')";

            Log.d("WordEditor", "Editöre gönderilen komut: " + jsCommand.substring(0, Math.min(200, jsCommand.length())));

            webViewBridge.executeJS(jsCommand);

        } catch (Exception e) {
            Log.e("WordEditor", "Editöre içerik yükleme hatası: " + e.getMessage());
            // Fallback
            webViewBridge.executeJS("setHtml('<p>İçerik yüklenemedi</p>')");
        }
    }

    private void saveDocument() {
        if (isNewDocument) {
            saveDocumentAs();
            return;
        }

        if (executorService == null || currentFilePath == null || currentFilePath.isEmpty()) {
            Toast.makeText(this, "❌ Kaydetme hatası - dosya yolu bulunamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("WordEditor", "💾 KAYDETME BAŞLIYOR: " + currentFilePath);
        showProgress(true);

        webViewBridge.getBestEffortHtml(html -> processSaveResult(html));
    }

    private void processSaveResult(String result) {
        String htmlContent =HtmlUtils.cleanHtmlResultAdvanced(result);
        Log.d("WordEditor", "🧹 Temizlenmiş HTML uzunluğu: " +
                (htmlContent != null ? htmlContent.length() : 0));

        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "❌ İçerik boş - kaydetme iptal edildi", Toast.LENGTH_SHORT).show();
            });
            return;
        }


        int imageCount =HtmlUtils.countImagesInHtml(htmlContent);
        Log.d("WordEditor", "🖼️ HTML'de " + imageCount + " resim bulundu");


        executorService.execute(() -> {
            try {
                Log.d("WordEditor", "💾 DOCX kaydetme işlemi başlıyor...");

                File originalFile = new File(currentFilePath);
                File backupFile = new File(currentFilePath + ".backup");

                if (originalFile.exists()) {
                    try {
                        FileUtils.copyFile(originalFile, backupFile);
                        Log.d("WordEditor", "🔒 Yedek oluşturuldu");
                    } catch (Exception e) {
                        Log.w("WordEditor", "⚠️ Yedekleme hatası: " + e.getMessage());
                    }
                }

                long startTime = System.currentTimeMillis();
                boolean success = WordDocumentHelper.saveHtmlToDocx(currentFilePath, htmlContent);
                long endTime = System.currentTimeMillis();

                Log.d("WordEditor", "⏱️ Kaydetme süresi: " + (endTime - startTime) + "ms");

                runOnUiThread(() -> {
                    showProgress(false);

                    if (success) {
                        handleSaveSuccess(backupFile, htmlContent, imageCount);
                    } else {
                        handleSaveFailure(backupFile);
                    }
                });

            } catch (Exception e) {
                Log.e("WordEditor", "❌ Kaydetme exception: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(this, "❌ Kaydetme hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void handleSaveSuccess(File backupFile, String htmlContent, int imageCount) {
        File savedFile = new File(currentFilePath);

        if (savedFile.exists() && savedFile.length() > 0) {
            isContentChanged = false;
            originalContent = htmlContent;
            updateTitle();

            String message = "✅ Belge kaydedildi!";
            if (imageCount > 0) {
                message += " (" + imageCount + " resim dahil)";
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            Log.d("WordEditor", "✅ Kaydetme başarılı - Dosya: " + savedFile.length() + " bytes");

            if (backupFile.exists()) {
                backupFile.delete();
            }

            testDocumentReadability();

        } else {
            handleSaveFailure(backupFile);
        }
    }

    private void handleSaveFailure(File backupFile) {
        Log.e("WordEditor", "❌ Kaydetme başarısız - dosya oluşmadı");

        // Yedekten geri yükle
        if (backupFile.exists()) {
            try {
                File originalFile = new File(currentFilePath);
                FileUtils.copyFile(backupFile, originalFile);
                backupFile.delete();
                Log.d("WordEditor", "🔄 Yedekten geri yüklendi");
            } catch (Exception e) {
                Log.e("WordEditor", "❌ Geri yükleme hatası: " + e.getMessage());
            }
        }

        Toast.makeText(this, "❌ Belge kaydedilemedi - lütfen tekrar deneyin", Toast.LENGTH_LONG).show();
    }

    private void testDocumentReadability() {

        if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
            Log.w("WordEditor", "⚠️ ExecutorService kullanılamaz durumda, test okuma iptal edildi");
            return;
        }

        try {
            executorService.execute(() -> {
                try {
                    Thread.sleep(1000); // 1 saniye bekle

                    WordDocumentHelper.WordContent testResult =
                            WordDocumentHelper.readWordDocument(currentFilePath);

                    if (testResult.isSuccess()) {
                        String content = testResult.getContent();
                        int imageCountAfterSave = HtmlUtils.countImagesInHtml(content);

                        Log.d("WordEditor", "✅ Test okuma başarılı");
                        Log.d("WordEditor", "📝 Okunan içerik uzunluğu: " +
                                (content != null ? content.length() : 0));
                        Log.d("WordEditor", "🖼️ Okunan resim sayısı: " + imageCountAfterSave);

                        runOnUiThread(() -> {
                            Log.d("WordEditor", "🎉 Belge test edildi ve okunabilir durumda!");
                        });
                    } else {
                        Log.w("WordEditor", "⚠️ Test okuma hatası: " + testResult.getError());
                    }

                } catch (Exception e) {
                    Log.e("WordEditor", "❌ Test okuma exception: " + e.getMessage());
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            Log.w("WordEditor", "⚠️ Test okuma görevi reddedildi - ExecutorService kapatılmış: " + e.getMessage());
        }
    }


    private void saveDocumentAs() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_save_as, null);
        EditText editFileName = dialogView.findViewById(R.id.editFileName);

        if (!isNewDocument && currentFilePath != null) {
            File currentFile = new File(currentFilePath);
            String nameWithoutExtension = currentFile.getName().replaceFirst("[.][^.]+$", "");
            editFileName.setText(nameWithoutExtension);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Farklı Kaydet")
                .setView(dialogView)
                .setPositiveButton("Kaydet", (dialog, which) -> {
                    String fileName = editFileName.getText().toString().trim();
                    if (!TextUtils.isEmpty(fileName)) {
                        performSaveAs(fileName);
                    } else {
                        Toast.makeText(this, "Dosya adı boş olamaz", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void performSaveAs(String fileName) {
        if (executorService == null) {
            Toast.makeText(this, "Sistem hatası", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalFileName = fileName;
        if (!finalFileName.endsWith(".docx")) {
            finalFileName += ".docx";
        }

        File documentsDir = new File(getFilesDir(), "Documents");
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }

        String newFilePath = new File(documentsDir, finalFileName).getAbsolutePath();
        final String fileNameToSave = finalFileName;

        showProgress(true);

        webViewBridge.getHtml(result -> {
            String htmlContent = HtmlUtils.cleanHtmlResult(result);

            executorService.execute(() -> {
                try {
                    boolean success = WordDocumentHelper.saveHtmlToDocx(newFilePath, htmlContent);

                    runOnUiThread(() -> {
                        showProgress(false);

                        if (success) {
                            currentFilePath = newFilePath;
                            isNewDocument = false;
                            isContentChanged = false;
                            originalContent = htmlContent;

                            getSupportActionBar().setTitle(fileNameToSave);
                            Toast.makeText(this, "Belge kaydedildi: " + fileNameToSave, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Belge kaydedilemedi", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(this, "Kaydetme hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
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

    private void showColorPickerDialog() {
        String[] colorNames = {"Siyah", "Kırmızı", "Yeşil", "Mavi", "Sarı", "Magenta", "Cyan", "Turuncu", "Mor", "Kahverengi"};
        String[] colorValues = {"#000000", "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#FFA500", "#800080", "#8B4513"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Yazı Rengi Seçin")
                .setItems(colorNames, (dialog, which) -> {
                    currentTextColor = colorValues[which];
                    webViewBridge.executeJS("setTextColor('" + currentTextColor + "')");
                    Toast.makeText(this, colorNames[which] + " renk seçildi", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showFontSizeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_font_size, null);
        SeekBar seekBarSize = dialogView.findViewById(R.id.seekBarSize);
        TextView textSizePreview = dialogView.findViewById(R.id.textSizePreview);

        seekBarSize.setMin(1);
        seekBarSize.setMax(7);
        seekBarSize.setProgress(3); // Default size 3
        textSizePreview.setText("Orta");

        String[] sizeNames = {"", "Çok Küçük", "Küçük", "Orta", "Büyük", "Çok Büyük", "Çok Daha Büyük", "En Büyük"};

        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress > 0 && progress < sizeNames.length) {
                    textSizePreview.setText(sizeNames[progress]);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("Yazı Boyutu")
                .setView(dialogView)
                .setPositiveButton("Uygula", (dialog, which) -> {
                    int selectedSize = seekBarSize.getProgress();
                    webViewBridge.executeJS("setFontSize(" + selectedSize + ")");
                    Toast.makeText(this, "Font boyutu: " + sizeNames[selectedSize], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showFontDialog() {
        String[] fonts = {"Arial", "Times New Roman", "Courier New", "Georgia", "Verdana", "Comic Sans MS", "Impact"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Yazı Tipi Seçin")
                .setItems(fonts, (dialog, which) -> {
                    currentFontFamily = fonts[which];
                    webViewBridge.executeJS("setFontName('" + currentFontFamily + "')");
                    Toast.makeText(this, "Font: " + currentFontFamily, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showInsertTableDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_insert_table, null);
        EditText editRows = dialogView.findViewById(R.id.editRows);
        EditText editCols = dialogView.findViewById(R.id.editCols);

        editRows.setText("3");
        editCols.setText("3");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Tablo Ekle")
                .setView(dialogView)
                .setPositiveButton("Ekle", (dialog, which) -> {
                    try {
                        int rows = Integer.parseInt(editRows.getText().toString());
                        int cols = Integer.parseInt(editCols.getText().toString());

                        if (rows > 0 && cols > 0 && rows <= 20 && cols <= 10) {
                            webViewBridge.executeJS("insertTable(" + rows + ", " + cols + ")");
                            Toast.makeText(this, "Tablo eklendi", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Geçersiz tablo boyutu", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Geçersiz sayı", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
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
                        saveDocument();
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
            autoSaveDocument();
        }
    }

    private void autoSaveDocument() {
        if (executorService != null && !executorService.isShutdown()) {
            webViewBridge.getHtml(result -> {
                String htmlContent = HtmlUtils.cleanHtmlResult(result);

                executorService.execute(() -> {
                    try {
                        boolean success = WordDocumentHelper.saveHtmlToDocx(currentFilePath, htmlContent);
                        if (success) {
                            runOnUiThread(() -> {
                                isContentChanged = false;
                                originalContent = htmlContent;
                                updateTitle();
                            });
                        }
                    } catch (Exception e) {
                        android.util.Log.e("WordEditor", "Otomatik kaydetme hatası: " + e.getMessage());
                    }
                });
            });
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("WordEditor", "🔄 Activity destroy ediliyor...");

        // ExecutorService'i güvenli şekilde kapat
        if (executorService != null && !executorService.isShutdown()) {
            try {
                Log.d("WordEditor", "⏹️ ExecutorService kapatılıyor...");
                executorService.shutdown();

                // 5 saniye bekle, ardından zorla kapat
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    Log.w("WordEditor", "⚠️ ExecutorService 5 saniyede kapanmadı, zorla kapatılıyor...");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Log.w("WordEditor", "⚠️ ExecutorService kapatma kesintiye uğradı");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        webViewBridge.destroyWebView();
        super.onDestroy();
        Log.d("WordEditor", "✅ Activity destroy tamamlandı");
    }
}