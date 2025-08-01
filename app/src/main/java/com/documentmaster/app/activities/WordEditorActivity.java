package com.documentmaster.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.documentmaster.app.BaseActivity;
import com.documentmaster.app.R;
import com.documentmaster.app.utils.WordDocumentHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class WordEditorActivity extends BaseActivity {

    // UI Components
    private Toolbar toolbar;
    private WebView webView;
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
    private static final int PICK_IMAGE_REQUEST = 2001;
    private static final int CAPTURE_IMAGE_REQUEST = 2002;


    private MaterialButton btnInsertImage, btnDeleteImage, btnImageInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_editor_offline);

        initViews();
        setupToolbar();
        setupWebView();
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

        // Formatting toolbar
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
        // Resim butonları
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

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isWebViewLoaded = true;

                if (!TextUtils.isEmpty(originalContent)) {
                    setEditorContent(originalContent);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false; // Tüm URL'leri WebView içinde handle et
            }
        });

        loadLocalHtml();
    }

    private void loadLocalHtml() {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.richeditor);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            String htmlContent = new String(buffer);
            webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null);
        } catch (Exception e) {
            android.util.Log.e("WordEditor", "HTML yükleme hatası: " + e.getMessage());
            Toast.makeText(this, "Editör yüklenemedi", Toast.LENGTH_SHORT).show();
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void onTextChanged(String html) {
            runOnUiThread(() -> {
                if (!isContentChanged && !html.equals(originalContent)) {
                    isContentChanged = true;
                    updateTitle();
                }
            });
        }

        @JavascriptInterface
        public void onFocus() {
            // Focus olayı
        }

        @JavascriptInterface
        public void onBlur() {
            // Blur olayı
        }
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
        btnBold.setOnClickListener(v -> executeJS("setBold()"));
        btnItalic.setOnClickListener(v -> executeJS("setItalic()"));
        btnUnderline.setOnClickListener(v -> executeJS("setUnderline()"));
        btnColor.setOnClickListener(v -> showColorPickerDialog());
        btnSize.setOnClickListener(v -> showFontSizeDialog());

        // Alignment buttons
        btnAlignLeft.setOnClickListener(v -> executeJS("setAlignLeft()"));
        btnAlignCenter.setOnClickListener(v -> executeJS("setAlignCenter()"));
        btnAlignRight.setOnClickListener(v -> executeJS("setAlignRight()"));

        // List buttons
        btnOrderedList.setOnClickListener(v -> executeJS("setOrderedList()"));
        btnUnorderedList.setOnClickListener(v -> executeJS("setUnorderedList()"));

        // Undo/Redo
        btnUndo.setOnClickListener(v -> executeJS("undo()"));
        btnRedo.setOnClickListener(v -> executeJS("redo()"));
        // Resim butonları
        btnInsertImage.setOnClickListener(v -> showImageInsertOptions());
        btnDeleteImage.setOnClickListener(v -> deleteSelectedImage());
        btnImageInfo.setOnClickListener(v -> showImageInfo());
    }

    private void showImageInsertOptions() {
        String[] options = {"Galeriden Seç", "Kamera ile Çek", "İptal"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Resim Ekle")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openImagePicker();
                            break;
                        case 1:
                            openCamera();
                            break;
                        case 2:
                            // İptal - hiçbir şey yapma
                            break;
                    }
                })
                .show();
    }

    private void openImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Log.e("WordEditor", "Galeri açma hatası: " + e.getMessage());
            Toast.makeText(this, "Galeri açılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
            } else {
                Toast.makeText(this, "Kamera uygulaması bulunamadı", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("WordEditor", "Kamera açma hatası: " + e.getMessage());
            Toast.makeText(this, "Kamera açılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PICK_IMAGE_REQUEST:
                    if (data != null && data.getData() != null) {
                        processSelectedImage(data.getData(), "Galeri Resmi");
                    }
                    break;

                case CAPTURE_IMAGE_REQUEST:
                    if (data != null && data.getExtras() != null) {
                        Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                        if (imageBitmap != null) {
                            processCapturedImage(imageBitmap);
                        }
                    }
                    break;
            }
        }
    }

    private void processSelectedImage(Uri imageUri, String imageName) {
        new Thread(() -> {
            try {
                Log.d("WordEditor", "🖼️ Resim işleniyor: " + imageUri.toString());

                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                if (bitmap != null) {

                    Bitmap optimizedBitmap = optimizeBitmap(bitmap, 800, 600);


                    String base64Data = bitmapToBase64(optimizedBitmap, "PNG");
                    String mimeType = "image/png";

                    runOnUiThread(() -> {
                        insertImageToEditor(base64Data, mimeType, imageName);
                    });

                    Log.d("WordEditor", "✅ Resim başarıyla işlendi ve eklendi");
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "❌ Resim yüklenemedi", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                Log.e("WordEditor", "❌ Resim işleme hatası: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Resim işleme hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void processCapturedImage(Bitmap bitmap) {
        new Thread(() -> {
            try {
                Log.d("WordEditor", "📷 Çekilen resim işleniyor...");


                Bitmap optimizedBitmap = optimizeBitmap(bitmap, 800, 600);


                String base64Data = bitmapToBase64(optimizedBitmap, "JPEG");
                String mimeType = "image/jpeg";


                runOnUiThread(() -> {
                    insertImageToEditor(base64Data, mimeType, "Kamera Resmi");
                });

                Log.d("WordEditor", "✅ Kamera resmi başarıyla eklendi");

            } catch (Exception e) {
                Log.e("WordEditor", "❌ Kamera resmi işleme hatası: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Kamera resmi işleme hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


    private Bitmap optimizeBitmap(Bitmap originalBitmap, int maxWidth, int maxHeight) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return originalBitmap; // Zaten uygun boyutta
        }

        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
    }

    private String bitmapToBase64(Bitmap bitmap, String format) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Bitmap.CompressFormat compressFormat = format.equalsIgnoreCase("PNG")
                ? Bitmap.CompressFormat.PNG
                : Bitmap.CompressFormat.JPEG;

        int quality = format.equalsIgnoreCase("PNG") ? 100 : 85;
        bitmap.compress(compressFormat, quality, outputStream);

        byte[] imageBytes = outputStream.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
    }

    private void insertImageToEditor(String base64Data, String mimeType, String imageName) {
        if (!isWebViewLoaded) {
            Toast.makeText(this, "⚠️ Editör henüz hazır değil, lütfen bekleyin...", Toast.LENGTH_SHORT).show();
            return;
        }

        String jsCommand = String.format("insertImageFromAndroid('%s', '%s', '%s')",
                base64Data, mimeType, imageName);

        webView.evaluateJavascript(jsCommand, result -> {
            Log.d("WordEditor", "📝 Resim ekleme sonucu: " + result);

            if ("true".equals(result)) {
                Toast.makeText(this, "✅ " + imageName + " eklendi", Toast.LENGTH_SHORT).show();
                notifyContentChange();
            } else {
                Toast.makeText(this, "❌ Resim eklenemedi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSelectedImage() {
        if (!isWebViewLoaded) {
            Toast.makeText(this, "⚠️ Editör henüz hazır değil", Toast.LENGTH_SHORT).show();
            return;
        }

        webView.evaluateJavascript("deleteSelectedImageFromAndroid()", result -> {
            Log.d("WordEditor", "🗑️ Resim silme sonucu: " + result);

            if ("true".equals(result)) {
                Toast.makeText(this, "✅ Resim silindi", Toast.LENGTH_SHORT).show();
                notifyContentChange();
            } else {
                Toast.makeText(this, "ℹ️ Silinecek resim seçilmedi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void notifyContentChange() {
        if (!isContentChanged) {
            isContentChanged = true;
            updateTitle();

            if (isWebViewLoaded) {
                webView.evaluateJavascript("getImageCountFromAndroid()", result -> {
                    try {
                        int imageCount = Integer.parseInt(result);
                        Log.d("WordEditor", "📊 Güncel resim sayısı: " + imageCount);
                    } catch (Exception e) {
                        // Ignore
                    }
                });
            }
        }
    }

    private void showImageInfo() {
        if (!isWebViewLoaded) {
            Toast.makeText(this, "⚠️ Editör henüz hazır değil", Toast.LENGTH_SHORT).show();
            return;
        }

        webView.evaluateJavascript("getImageCountFromAndroid()", result -> {
            try {
                int imageCount = Integer.parseInt(result);

                webView.evaluateJavascript("validateImagesFromAndroid()", validationResult -> {
                    Log.d("WordEditor", "📊 Resim doğrulama sonucu: " + validationResult);

                    String message = "📊 Resim Bilgileri:\n\n" +
                            "📷 Toplam Resim: " + imageCount + "\n" +
                            "📁 Belge: " + (currentFilePath != null ? new java.io.File(currentFilePath).getName() : "Yeni Belge") + "\n" +
                            "💾 Durum: " + (isContentChanged ? "Değiştirildi" : "Kaydedildi");

                    new MaterialAlertDialogBuilder(this)
                            .setTitle("🖼️ Resim Bilgileri")
                            .setMessage(message)
                            .setPositiveButton("Tamam", null)
                            .setNeutralButton("Resimleri Doğrula", (dialog, which) -> {
                                webView.evaluateJavascript("validateImageIntegrity()", null);
                                Toast.makeText(this, "🔍 Resimler doğrulandı (konsola bakın)", Toast.LENGTH_SHORT).show();
                            })
                            .show();
                });

            } catch (NumberFormatException e) {
                Toast.makeText(this, "❌ Resim sayısı alınamadı", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void executeJS(String jsCommand) {
        if (isWebViewLoaded) {
            webView.evaluateJavascript(jsCommand, null);
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
                        originalContent = convertToHtml(result.getContent());

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

            String cleanContent = normalizeHtmlForEditor(content);

            String jsContent = cleanContent
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            String jsCommand = "setHtml('" + jsContent + "')";

            Log.d("WordEditor", "Editöre gönderilen komut: " + jsCommand.substring(0, Math.min(200, jsCommand.length())));

            executeJS(jsCommand);

        } catch (Exception e) {
            Log.e("WordEditor", "Editöre içerik yükleme hatası: " + e.getMessage());
            // Fallback
            executeJS("setHtml('<p>İçerik yüklenemedi</p>')");
        }
    }

    private String normalizeHtmlForEditor(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "<p><br></p>";
        }

        String normalized = html

                .replaceAll("\\\\\"", "\"")
                .replaceAll("\\\\n", "\n")
                .replaceAll("\\\\r", "")
                .replaceAll("\\\\/", "/")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .trim();

        if (!normalized.contains("<")) {
            String[] lines = normalized.split("\n");
            StringBuilder htmlBuilder = new StringBuilder();
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    htmlBuilder.append("<p><br></p>");
                } else {
                    htmlBuilder.append("<p>").append(escapeHtml(line.trim())).append("</p>");
                }
            }
            normalized = htmlBuilder.toString();
        }

        Log.d("WordEditor", "HTML normalize edildi");
        return normalized;
    }

    private String convertToHtml(String content) {
        if (content == null) return "<p><br></p>";


        if (content.trim().startsWith("<") && (content.contains("<p>") || content.contains("<div>") || content.contains("<table"))) {
            Log.d("WordEditor", "İçerik zaten HTML formatında");
            return content;
        }

        StringBuilder html = new StringBuilder();
        html.append("<div>");

        String[] lines = content.split("\n");

        boolean inTable = false;
        StringBuilder tableBuilder = null;

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) {
                if (!inTable) {
                    html.append("<p><br></p>");
                }
                continue;
            }


            if (isTableLine(line)) {
                if (!inTable) {

                    inTable = true;
                    tableBuilder = new StringBuilder();
                    tableBuilder.append("<table border=\"1\" style=\"border-collapse: collapse; width: 100%; margin: 10px 0;\">");
                }

                if (line.contains("│")) {

                    tableBuilder.append(convertTableLineToHtml(line));
                }


            } else {

                if (inTable && tableBuilder != null) {
                    tableBuilder.append("</table>");
                    html.append(tableBuilder.toString());
                    inTable = false;
                    tableBuilder = null;
                }


                html.append("<p>").append(escapeHtml(line)).append("</p>");
            }
        }

        if (inTable && tableBuilder != null) {
            tableBuilder.append("</table>");
            html.append(tableBuilder.toString());
        }

        html.append("</div>");

        String result = html.toString();
        Log.d("WordEditor", "Text → HTML dönüştürüldü: " + result.substring(0, Math.min(200, result.length())));

        return result;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    private boolean isTableLine(String line) {
        return line.contains("│") || line.contains("┌") || line.contains("├") ||
                line.contains("└") || line.contains("─");
    }

    private String convertTableLineToHtml(String line) {
        if (!line.contains("│")) {
            return "";
        }


        String[] cells = line.split("│");
        StringBuilder tableRow = new StringBuilder("<tr>");

        for (String cell : cells) {
            String cleanCell = cell.trim();
            if (!cleanCell.isEmpty() &&
                    !cleanCell.equals("┌") && !cleanCell.equals("├") &&
                    !cleanCell.equals("└") && !cleanCell.equals("┐") &&
                    !cleanCell.equals("┤") && !cleanCell.equals("┘") &&
                    !cleanCell.matches("[-─]+")) {

                tableRow.append("<td style=\"padding: 8px; border: 1px solid #333;\">")
                        .append(escapeHtml(cleanCell))
                        .append("</td>");
            }
        }

        tableRow.append("</tr>");
        return tableRow.toString();
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

        Log.d("WordEditor", "💾 GELİŞTİRİLMİŞ KAYDETME BAŞLIYOR: " + currentFilePath);
        showProgress(true);

        webView.evaluateJavascript("getHtmlWithFormats()", formatResult -> {
            if (formatResult != null && !formatResult.equals("null")) {
                Log.d("WordEditor", "📝 Format korumalı HTML alındı");
                processSaveResult(formatResult);
            } else {

                Log.w("WordEditor", "⚠️ Format korumalı HTML başarısız, resimli HTML deneniyor...");
                webView.evaluateJavascript("getHtmlWithImages()", imageResult -> {
                    if (imageResult != null && !imageResult.equals("null")) {
                        processSaveResult(imageResult);
                    } else {

                        Log.w("WordEditor", "⚠️ Resimli HTML başarısız, normal HTML deneniyor...");
                        webView.evaluateJavascript("getHtml()", result -> {
                            processSaveResult(result);
                        });
                    }
                });
            }
        });
    }

    private void processSaveResult(String result) {
        String htmlContent = cleanHtmlResultAdvanced(result);
        Log.d("WordEditor", "🧹 Temizlenmiş HTML uzunluğu: " +
                (htmlContent != null ? htmlContent.length() : 0));

        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "❌ İçerik boş - kaydetme iptal edildi", Toast.LENGTH_SHORT).show();
            });
            return;
        }


        int imageCount = countImagesInHtml(htmlContent);
        Log.d("WordEditor", "🖼️ HTML'de " + imageCount + " resim bulundu");


        executorService.execute(() -> {
            try {
                Log.d("WordEditor", "💾 DOCX kaydetme işlemi başlıyor...");

                File originalFile = new File(currentFilePath);
                File backupFile = new File(currentFilePath + ".backup");

                if (originalFile.exists()) {
                    try {
                        copyFile(originalFile, backupFile);
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
                copyFile(backupFile, originalFile);
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
                        int imageCountAfterSave = countImagesInHtml(content);

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

    private String cleanHtmlResultAdvanced(String result) {
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
                .replace("\\u0027", "'")
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

        cleaned = cleanHtmlPreservingImages(cleaned);

        Log.d("WordEditor", "✅ Gelişmiş temizleme tamamlandı - Son uzunluk: " + cleaned.length());
        return cleaned;
    }

    private String cleanHtmlPreservingImages(String html) {

        java.util.Map<String, String> imageMap = new java.util.HashMap<>();
        Pattern imagePattern = Pattern.compile("data:image/[^;]+;base64,[A-Za-z0-9+/=]+");
        Matcher imageMatcher = imagePattern.matcher(html);

        int imageIndex = 0;
        StringBuffer sb = new StringBuffer();

        while (imageMatcher.find()) {
            String imageData = imageMatcher.group();
            String placeholder = "IMAGE_PLACEHOLDER_" + imageIndex;
            imageMap.put(placeholder, imageData);
            imageMatcher.appendReplacement(sb, placeholder);
            imageIndex++;
        }
        imageMatcher.appendTail(sb);


        String cleanedHtml = sb.toString()
                .replaceAll("\\s+", " ")  // Çoklu boşlukları tek boşluğa çevir
                .trim();


        for (java.util.Map.Entry<String, String> entry : imageMap.entrySet()) {
            cleanedHtml = cleanedHtml.replace(entry.getKey(), entry.getValue());
        }

        Log.d("WordEditor", "🖼️ " + imageMap.size() + " resim korunarak HTML temizlendi");
        return cleanedHtml;
    }


    private int countImagesInHtml(String html) {
        if (html == null) return 0;

        Pattern pattern = Pattern.compile("<img[^>]*src=\"data:image/[^\"]+\"[^>]*>");
        Matcher matcher = pattern.matcher(html);

        int count = 0;
        while (matcher.find()) {
            count++;
        }

        return count;
    }

    private void copyFile(File source, File destination) throws Exception {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(source);
             java.io.FileOutputStream fos = new java.io.FileOutputStream(destination)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
        }
    }

    private void verifyDocument() {
        if (executorService == null || currentFilePath == null) return;

        executorService.execute(() -> {
            try {
                Thread.sleep(500); // Biraz bekle


                WordDocumentHelper.WordContent result = WordDocumentHelper.readWordDocument(currentFilePath);

                if (result.isSuccess()) {
                    Log.d("WordEditor", "✅ Doğrulama başarılı - Belge okunabilir");

                    runOnUiThread(() -> {
                        // UI'da başarı göster (opsiyonel)
                        Log.d("WordEditor", "📄 Belge doğrulandı ve okunabilir durumda");
                    });
                } else {
                    Log.w("WordEditor", "⚠️ Doğrulama hatası: " + result.getError());
                }

            } catch (Exception e) {
                Log.e("WordEditor", "❌ Doğrulama exception: " + e.getMessage());
            }
        });
    }

    private void loadDocumentForVerification() {
        if (executorService == null) return;

        executorService.execute(() -> {
            try {
                // Kısa bir bekleme
                Thread.sleep(500);

                WordDocumentHelper.WordContent result = WordDocumentHelper.readWordDocument(currentFilePath);

                if (result.isSuccess()) {
                    Log.d("WordEditor", "Doğrulama - Okunan içerik: " +
                            result.getContent().substring(0, Math.min(200, result.getContent().length())));

                    runOnUiThread(() -> {
                        originalContent = result.getContent();
                        // İçeriği editöre yeniden yükleme (opsiyonel)
                        // setEditorContent(originalContent);
                    });
                }
            } catch (Exception e) {
                Log.e("WordEditor", "Doğrulama hatası: " + e.getMessage());
            }
        });
    }

    private String cleanHtmlResult(String result) {
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

        Log.d("WordEditor", "✅ Temizlenmiş sonuç uzunluğu: " + cleaned.length());
        Log.d("WordEditor", "📝 Temizlenmiş içerik örneği: " +
                cleaned.substring(0, Math.min(150, cleaned.length())));

        return cleaned;
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

        webView.evaluateJavascript("getHtml()", result -> {
            String htmlContent = cleanHtmlResult(result);

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
                    executeJS("setTextColor('" + currentTextColor + "')");
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
                    executeJS("setFontSize(" + selectedSize + ")");
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
                    executeJS("setFontName('" + currentFontFamily + "')");
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
                            executeJS("insertTable(" + rows + ", " + cols + ")");
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
        webView.evaluateJavascript("getText()", result -> {
            String textContent = cleanHtmlResult(result);

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
            webView.evaluateJavascript("getHtml()", result -> {
                String htmlContent = cleanHtmlResult(result);

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

        // WebView'i temizle
        if (webView != null) {
            try {
                webView.loadUrl("about:blank");
                webView.destroy();
                webView = null;
                Log.d("WordEditor", "🌐 WebView temizlendi");
            } catch (Exception e) {
                Log.e("WordEditor", "❌ WebView temizleme hatası: " + e.getMessage());
            }
        }

        super.onDestroy();
        Log.d("WordEditor", "✅ Activity destroy tamamlandı");
    }
}