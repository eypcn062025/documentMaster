package com.documentmaster.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.documentmaster.app.activities.WordEditorActivity;
import com.documentmaster.app.utils.WordDocumentHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.documentmaster.app.utils.FileManagerHelper;

import android.content.ActivityNotFoundException;

public class MainActivity extends BaseActivity {
    private Toolbar toolbar;
    private MaterialButton btnCreatePdf, btnOpenFile, btnViewAll;
    private FloatingActionButton fab;
    private RecyclerView recyclerViewDocuments;
    private LinearLayout emptyState;
    private List<Document> documentList;
    private DocumentAdapter documentAdapter;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int PICK_FILE_REQUEST = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        checkPermissions();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        btnCreatePdf = findViewById(R.id.btnCreatePdf);
        btnOpenFile = findViewById(R.id.btnOpenFile);
        btnViewAll = findViewById(R.id.btnViewAll);
        fab = findViewById(R.id.fab);
        recyclerViewDocuments = findViewById(R.id.recyclerViewDocuments);
        emptyState = findViewById(R.id.emptyState);
        documentList = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("DocumentMaster");
        }
    }

    private void setupRecyclerView() {
        documentAdapter = new DocumentAdapter(documentList, new DocumentAdapter.OnDocumentClickListener() {
            @Override
            public void onDocumentClick(Document document) {
                openDocument(document);
            }

            @Override
            public void onDocumentLongClick(Document document) {
                showDocumentOptions(document);
            }
        });

        recyclerViewDocuments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDocuments.setAdapter(documentAdapter);
    }

    private void setupListeners() {
        btnCreatePdf.setOnClickListener(v -> showCreateNewDocumentDialog());
        btnOpenFile.setOnClickListener(v -> openFilePicker());
        btnViewAll.setOnClickListener(v -> showAllDocuments());
        fab.setOnClickListener(v -> showQuickActionDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            Toast.makeText(this, "Arama özelliği yakında!", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_dark_mode) {
            toggleDarkMode();
            return true;
        } else if (id == R.id.action_settings) {
            Toast.makeText(this, "Ayarlar yakında!", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showPermissionDialog();
            } else {
                loadDocuments();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, PERMISSION_REQUEST_CODE);
            } else {
                loadDocuments();
            }
        }
    }

    private void showPermissionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("İzin Gerekli")
                .setMessage("DocumentMaster'ın dosyalarınıza erişebilmesi için izin vermeniz gerekiyor.")
                .setPositiveButton("İzin Ver", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("İptal", (dialog, which) -> {
                    Toast.makeText(this, "İzin olmadan dosyalar görüntülenemez", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                loadDocuments();
            } else {
                Toast.makeText(this, "İzinler reddedildi. Uygulama düzgün çalışmayabilir.", Toast.LENGTH_LONG).show();
                loadInternalDocuments();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            loadDocuments();
        }
    }

    private void loadDocuments() {
        documentList.clear();
        loadInternalDocuments();
        loadDownloadsDocuments();
        loadDocumentsFromFolder();
        updateUI();
    }

    private void loadInternalDocuments() {
        File internalDir = new File(getFilesDir(), "Documents");
        if (internalDir.exists()) {
            loadDocumentsFromDirectory(internalDir);
        }
    }

    private void loadDownloadsDocuments() {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloadsDir != null && downloadsDir.exists()) {
                loadDocumentsFromDirectory(downloadsDir);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Downloads klasörü yüklenemedi: " + e.getMessage());
        }
    }

    private void loadDocumentsFromFolder() {
        try {
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (documentsDir != null && documentsDir.exists()) {
                loadDocumentsFromDirectory(documentsDir);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Documents klasörü yüklenemedi: " + e.getMessage());
        }
    }

    private void loadDocumentsFromDirectory(File directory) {
        try {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && isSupportedFile(file.getName())) {
                        Document document = new Document(file.getAbsolutePath());
                        documentList.add(document);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Dizin tarama hatası: " + e.getMessage());
        }
    }

    private boolean isSupportedFile(String fileName) {
        if (fileName == null) return false;

        String[] supportedExtensions = {
                ".docx", ".doc",
                ".html", ".htm", ".txt",
                ".pdf", ".xls", ".xlsx", ".ppt", ".pptx", ".csv"
        };

        String lowerCaseFileName = fileName.toLowerCase();
        for (String extension : supportedExtensions) {
            if (lowerCaseFileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private void updateUI() {
        if (documentList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerViewDocuments.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerViewDocuments.setVisibility(View.VISIBLE);
            documentAdapter.notifyDataSetChanged();
        }
    }

    private void showCreateNewDocumentDialog() {
        String[] options = {"Word Belgesi (.docx)", "Metin Belgesi (.txt)", "PDF Belgesi"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Yeni Belge Oluştur")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            createNewWordDocument();
                            break;
                        case 1:
                            createNewTextDocument();
                            break;
                        case 2:
                            createNewPdf();
                            break;
                    }
                })
                .show();
    }

    private void createNewWordDocument() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_document, null);
        TextInputEditText editFileName = dialogView.findViewById(R.id.editFileName);
        editFileName.setHint("Word belgesi adı");
        editFileName.setText("Yeni Belge");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Yeni Word Belgesi")
                .setView(dialogView)
                .setPositiveButton("Oluştur", (dialog, which) -> {
                    String fileName = editFileName.getText().toString().trim();
                    if (!fileName.isEmpty()) {
                        createWordDocumentFile(fileName);
                    } else {
                        Toast.makeText(this, "Dosya adı boş olamaz", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void createWordDocumentFile(String fileName) {
        try {
            File documentsDir = new File(getFilesDir(), "Documents");
            if (!documentsDir.exists()) {
                boolean created = documentsDir.mkdirs();
                Log.d("MainActivity", "Documents dizini oluşturuldu: " + created);
            }

            if (!fileName.endsWith(".docx")) {
                fileName += ".docx";
            }

            String filePath = new File(documentsDir, fileName).getAbsolutePath();
            Log.d("MainActivity", "Yeni DOCX dosya yolu: " + filePath);

            boolean success = WordDocumentHelper.createWordDocument(filePath, "");

            if (success) {
                Toast.makeText(this, "DOCX belgesi oluşturuldu: " + fileName, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, WordEditorActivity.class);
                intent.putExtra(WordEditorActivity.EXTRA_FILE_PATH, filePath);
                intent.putExtra(WordEditorActivity.EXTRA_IS_NEW_DOCUMENT, true);
                startActivity(intent);
                loadDocuments();
            } else {
                Toast.makeText(this, "DOCX belgesi oluşturulamadı", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("MainActivity", "DOCX belge oluşturma hatası: " + e.getMessage());
            Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void createNewTextDocument() {
        Toast.makeText(this, "Metin editörü geliştiriliyor...", Toast.LENGTH_SHORT).show();
    }

    private void createNewPdf() {
        Toast.makeText(this, "PDF oluşturma özelliği geliştiriliyor...", Toast.LENGTH_SHORT).show();
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            String[] mimeTypes = {
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/msword",
                    "application/pdf",
                    "text/plain",
                    "text/html",
                    "*/*"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            startActivityForResult(Intent.createChooser(intent, "Word Belgesi Seç"), PICK_FILE_REQUEST);
        } catch (ActivityNotFoundException e) {
            Log.e("MainActivity", "Dosya seçici bulunamadı: " + e.getMessage());
            try {
                Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
                fallbackIntent.setType("*/*");
                fallbackIntent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(fallbackIntent, PICK_FILE_REQUEST);
            } catch (Exception ex) {
                Toast.makeText(this, "Dosya seçici açılamadı. Lütfen bir dosya yöneticisi uygulaması yükleyin.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                Log.d("MainActivity", "Seçilen dosya URI: " + selectedFileUri.toString());
                processSelectedFile(selectedFileUri);
            } else {
                Toast.makeText(this, "Dosya seçilemedi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processSelectedFile(Uri fileUri) {
        try {
            String fileName = FileManagerHelper.getFileNameFromUri(this, fileUri);
            Log.d("MainActivity", "Dosya adı: " + fileName);

            long fileSize = FileManagerHelper.getFileSizeFromUri(this, fileUri);
            Log.d("MainActivity", "Dosya boyutu: " + fileSize + " bytes");

            if (fileSize > 50 * 1024 * 1024) {
                Toast.makeText(this, "Dosya çok büyük. Maksimum 50MB desteklenir.", Toast.LENGTH_LONG).show();
                return;
            }

            if (fileName != null && !isSupportedFile(fileName)) {
                Toast.makeText(this, "Desteklenmeyen dosya formatı: " + fileName, Toast.LENGTH_LONG).show();
                return;
            }

            showProgressDialog("Dosya açılıyor...");

            new Thread(() -> {
                try {
                    String filePath = null;

                    filePath = FileManagerHelper.getRealPathFromURI(this, fileUri);

                    if (filePath == null || !new File(filePath).exists()) {
                        Log.d("MainActivity", "Gerçek yol alınamadı, temp'e kopyalanıyor...");
                        filePath = FileManagerHelper.copyFileFromUri(this, fileUri);
                    }

                    if (filePath != null && new File(filePath).exists()) {
                        final String finalPath = filePath;

                        runOnUiThread(() -> {
                            hideProgressDialog();
                            openDocumentFromPath(finalPath, fileName);
                        });
                    } else {
                        runOnUiThread(() -> {
                            hideProgressDialog();
                            Toast.makeText(this, "Dosya erişimi başarısız", Toast.LENGTH_LONG).show();
                        });
                    }

                } catch (Exception e) {
                    Log.e("MainActivity", "Dosya işleme hatası: " + e.getMessage());
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        Toast.makeText(this, "Dosya açma hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();

        } catch (Exception e) {
            Log.e("MainActivity", "Dosya seçim işleme hatası: " + e.getMessage());
            Toast.makeText(this, "Dosya işleme hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private android.app.ProgressDialog progressDialog;

    private void showProgressDialog(String message) {
        runOnUiThread(() -> {
            if (progressDialog == null) {
                progressDialog = new android.app.ProgressDialog(this);
                progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
            }
            progressDialog.setMessage(message);
            progressDialog.show();
        });
    }

    private void hideProgressDialog() {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }

    private void openDocumentFromPath(String filePath, String displayName) {
        try {
            Log.d("MainActivity", "🚀 Belge açma başlıyor...");

            debugFileInfo(filePath);

            String fileName = displayName != null ? displayName : new File(filePath).getName();
            String lowerFileName = fileName.toLowerCase();

            if (lowerFileName.endsWith(".docx") || lowerFileName.endsWith(".doc") ||
                    lowerFileName.endsWith(".html") || lowerFileName.endsWith(".htm") ||
                    lowerFileName.endsWith(".txt")) {

                if (WordDocumentHelper.isValidWordDocument(filePath)) {
                    Log.d("MainActivity", "✅ Geçerli belge, editör açılıyor...");

                    Intent intent = new Intent(this, WordEditorActivity.class);
                    intent.putExtra(WordEditorActivity.EXTRA_FILE_PATH, filePath);
                    intent.putExtra(WordEditorActivity.EXTRA_IS_NEW_DOCUMENT, false);
                    intent.putExtra("DISPLAY_NAME", displayName);
                    startActivity(intent);

                    loadDocuments();

                } else {
                    Log.w("MainActivity", "❌ Geçersiz belge: " + filePath);
                    Toast.makeText(this, "❌ Belge açılamadı - dosya bozulmuş veya desteklenmiyor", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.w("MainActivity", "⚠️ Desteklenmeyen format: " + fileName);
                Toast.makeText(this, "⚠️ Bu dosya türü için editör geliştiriliyor: " + fileName, Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("MainActivity", "❌ Belge açma hatası: " + e.getMessage(), e);
            Toast.makeText(this, "❌ Belge açma hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openSelectedFile(Uri fileUri) {
        try {
            String filePath = fileUri.getPath();
            if (filePath != null) {
                Document document = new Document(filePath);
                openDocument(document);
            } else {
                Toast.makeText(this, "Dosya yolu alınamadı", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Dosya açma hatası: " + e.getMessage());
            Toast.makeText(this, "Dosya açılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showQuickActionDialog() {
        String[] options = {"Yeni Word Belgesi", "Dosya Aç", "Tarama Yap", "Son Belgeleri Görüntüle"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Hızlı İşlemler")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            createNewWordDocument();
                            break;
                        case 1:
                            openFilePicker();
                            break;
                        case 2:
                            Toast.makeText(this, "Tarama özelliği geliştiriliyor...", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            showRecentDocuments();
                            break;
                    }
                })
                .show();
    }

    private void showRecentDocuments() {
        if (documentList.isEmpty()) {
            Toast.makeText(this, "Henüz hiç belge açılmamış", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Document> recentDocs = new ArrayList<>();
        int count = Math.min(5, documentList.size());
        for (int i = 0; i < count; i++) {
            recentDocs.add(documentList.get(i));
        }

        String[] docNames = new String[recentDocs.size()];
        for (int i = 0; i < recentDocs.size(); i++) {
            docNames[i] = recentDocs.get(i).getName();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Son Belgeler")
                .setItems(docNames, (dialog, which) -> {
                    openDocument(recentDocs.get(which));
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showAllDocuments() {
        if (documentList.isEmpty()) {
            Toast.makeText(this, "Henüz hiç belge bulunamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Tüm belgeler sayfası geliştiriliyor... (" + documentList.size() + " belge bulundu)", Toast.LENGTH_SHORT).show();
    }

    private void openDocument(Document document) {
        if (document == null || document.getPath() == null) {
            Toast.makeText(this, "Geçersiz belge", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = document.getName().toLowerCase();
        String filePath = document.getPath();

        Log.d("MainActivity", "Açılmaya çalışılan dosya: " + filePath);
        Log.d("MainActivity", "Dosya türü: " + fileName);

        try {
            if (fileName.endsWith(".docx") || fileName.endsWith(".doc") ||
                    fileName.endsWith(".html") || fileName.endsWith(".htm") || fileName.endsWith(".txt")) {

                if (WordDocumentHelper.isValidWordDocument(filePath)) {
                    Log.d("MainActivity", "Geçerli Word belgesi, editör açılıyor...");
                    Intent intent = new Intent(this, WordEditorActivity.class);
                    intent.putExtra(WordEditorActivity.EXTRA_FILE_PATH, filePath);
                    intent.putExtra(WordEditorActivity.EXTRA_IS_NEW_DOCUMENT, false);
                    startActivity(intent);
                } else {
                    Log.e("MainActivity", "Geçersiz Word belgesi: " + filePath);
                    Toast.makeText(this, "Belge açılamadı - dosya bozulmuş olabilir", Toast.LENGTH_LONG).show();
                }
            } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                Toast.makeText(this, "Excel editörü geliştiriliyor...", Toast.LENGTH_SHORT).show();
            } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
                Toast.makeText(this, "PowerPoint editörü geliştiriliyor...", Toast.LENGTH_SHORT).show();
            } else if (fileName.endsWith(".pdf")) {
                Toast.makeText(this, "PDF görüntüleyici geliştiriliyor...", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(new File(document.getPath())), "*/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("MainActivity", "Sistem editörü açma hatası: " + e.getMessage());
                    Toast.makeText(this, "Dosya açılamadı", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Belge açma genel hatası: " + e.getMessage());
            Toast.makeText(this, "Belge açılırken hata oluştu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDocumentOptions(Document document) {
        if (document == null) {
            Toast.makeText(this, "Geçersiz belge", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Aç", "Düzenle", "Paylaş", "Sil", "Yeniden Adlandır", "Özellikler"};

        new MaterialAlertDialogBuilder(this)
                .setTitle(document.getName())
                .setItems(options, (dialog, which) -> {
                    try {
                        switch (which) {
                            case 0:
                                openDocument(document);
                                break;
                            case 1:
                                editDocument(document);
                                break;
                            case 2:
                                shareDocument(document);
                                break;
                            case 3:
                                deleteDocument(document);
                                break;
                            case 4:
                                renameDocument(document);
                                break;
                            case 5:
                                showDocumentProperties(document);
                                break;
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Belge işlemi hatası: " + e.getMessage());
                        Toast.makeText(this, "İşlem başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void editDocument(Document document) {
        if (document == null || document.getPath() == null) {
            Toast.makeText(this, "Geçersiz belge", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = document.getName().toLowerCase();

        try {
            if (fileName.endsWith(".docx") || fileName.endsWith(".doc") ||
                    fileName.endsWith(".html") || fileName.endsWith(".htm") || fileName.endsWith(".txt")) {
                Intent intent = new Intent(this, WordEditorActivity.class);
                intent.putExtra(WordEditorActivity.EXTRA_FILE_PATH, document.getPath());
                intent.putExtra(WordEditorActivity.EXTRA_IS_NEW_DOCUMENT, false);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Bu dosya türü henüz düzenlenemez", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Düzenleme hatası: " + e.getMessage());
            Toast.makeText(this, "Düzenleme başlatılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareDocument(Document document) {
        if (document == null || document.getPath() == null) {
            Toast.makeText(this, "Geçersiz belge", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(document.getPath())));
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, document.getName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "DocumentMaster ile paylaşılan belge: " + document.getName());
            startActivity(Intent.createChooser(shareIntent, "Belgeyi Paylaş"));
        } catch (Exception e) {
            Log.e("MainActivity", "Paylaşma hatası: " + e.getMessage());
            Toast.makeText(this, "Paylaşma hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteDocument(Document document) {
        if (document == null || document.getPath() == null) {
            Toast.makeText(this, "Geçersiz belge", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Belgeyi Sil")
                .setMessage("Bu belgeyi silmek istediğinize emin misiniz?\n\n📄 " + document.getName() +
                        "\n📏 " + document.getFormattedSize() +
                        "\n📅 " + document.getFormattedDate())
                .setPositiveButton("Sil", (dialog, which) -> {
                    try {
                        File file = new File(document.getPath());
                        if (file.exists() && file.delete()) {
                            documentList.remove(document);
                            documentAdapter.notifyDataSetChanged();
                            updateUI();
                            Toast.makeText(this, "✅ Belge silindi: " + document.getName(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "❌ Belge silinemedi", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Silme hatası: " + e.getMessage());
                        Toast.makeText(this, "Silme hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .setIcon(android.R.drawable.ic_menu_delete)
                .show();
    }

    private void renameDocument(Document document) {
        if (document == null || document.getPath() == null) {
            Toast.makeText(this, "Geçersiz belge", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rename, null);
        TextInputEditText editNewName = dialogView.findViewById(R.id.editNewName);

        String currentName = document.getName();
        String nameWithoutExtension = currentName.contains(".") ?
                currentName.substring(0, currentName.lastIndexOf(".")) : currentName;
        editNewName.setText(nameWithoutExtension);
        editNewName.selectAll();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Yeniden Adlandır")
                .setMessage("Mevcut ad: " + document.getName())
                .setView(dialogView)
                .setPositiveButton("Değiştir", (dialog, which) -> {
                    String newName = editNewName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        performRename(document, newName);
                    } else {
                        Toast.makeText(this, "Dosya adı boş olamaz", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void performRename(Document document, String newName) {
        try {
            File oldFile = new File(document.getPath());
            String extension = "";
            String oldName = oldFile.getName();

            if (oldName.contains(".")) {
                extension = oldName.substring(oldName.lastIndexOf("."));
            }

            File newFile = new File(oldFile.getParent(), newName + extension);

            if (newFile.exists()) {
                Toast.makeText(this, "Bu isimde bir dosya zaten var", Toast.LENGTH_SHORT).show();
                return;
            }

            if (oldFile.renameTo(newFile)) {
                document.setName(newName + extension);
                document.setPath(newFile.getAbsolutePath());
                documentAdapter.notifyDataSetChanged();
                Toast.makeText(this, "✅ Dosya adı değiştirildi: " + newName + extension, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Dosya adı değiştirilemedi", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Yeniden adlandırma hatası: " + e.getMessage());
            Toast.makeText(this, "Yeniden adlandırma hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDocumentProperties(Document document) {
        if (document == null || document.getPath() == null) {
            Toast.makeText(this, "Geçersiz belge", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String properties;

            if (document.getName().toLowerCase().endsWith(".docx") ||
                    document.getName().toLowerCase().endsWith(".doc") ||
                    document.getName().toLowerCase().endsWith(".html") ||
                    document.getName().toLowerCase().endsWith(".htm") ||
                    document.getName().toLowerCase().endsWith(".txt")) {
                properties = WordDocumentHelper.getDocumentProperties(document.getPath());
            } else {

                File file = new File(document.getPath());
                properties = "📄 Ad: " + document.getName() + "\n" +
                        "🏷️ Tür: " + document.getType() + "\n" +
                        "📏 Boyut: " + document.getFormattedSize() + "\n" +
                        "📅 Tarih: " + document.getFormattedDate() + "\n" +
                        "📁 Konum: " + document.getPath() + "\n" +
                        "🔐 Okunabilir: " + (file.canRead() ? "Evet" : "Hayır") + "\n" +
                        "✏️ Yazılabilir: " + (file.canWrite() ? "Evet" : "Hayır");
            }

            new MaterialAlertDialogBuilder(this)
                    .setTitle("📋 Belge Özellikleri")
                    .setMessage(properties)
                    .setPositiveButton("Tamam", null)
                    .setNeutralButton("Paylaş", (dialog, which) -> shareDocument(document))
                    .show();
        } catch (Exception e) {
            Log.e("MainActivity", "Özellikler gösterme hatası: " + e.getMessage());
            Toast.makeText(this, "Özellikler gösterilemiyor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup işlemleri
        if (documentList != null) {
            documentList.clear();
        }
    }

    private void debugFileInfo(String filePath) {
        try {
            File file = new File(filePath);
            Log.d("MainActivity", "=== DOSYA DEBUG BİLGİLERİ ===");
            Log.d("MainActivity", "📁 Dosya yolu: " + filePath);
            Log.d("MainActivity", "📄 Dosya adı: " + file.getName());
            Log.d("MainActivity", "📏 Dosya boyutu: " + file.length() + " bytes");
            Log.d("MainActivity", "📅 Son değişiklik: " + new java.util.Date(file.lastModified()));
            Log.d("MainActivity", "✅ Var mı: " + file.exists());
            Log.d("MainActivity", "👁️ Okunabilir: " + file.canRead());
            Log.d("MainActivity", "✏️ Yazılabilir: " + file.canWrite());
            Log.d("MainActivity", "============================");

            // WordDocumentHelper ile test
            if (file.exists()) {
                boolean isValid = WordDocumentHelper.isValidWordDocument(filePath);
                Log.d("MainActivity", "📋 WordDocumentHelper geçerlilik: " + isValid);

                if (isValid) {
                    WordDocumentHelper.WordContent content = WordDocumentHelper.readWordDocument(filePath);
                    Log.d("MainActivity", "📖 İçerik okuma başarılı: " + content.isSuccess());
                    if (content.isSuccess()) {
                        String contentText = content.getContent();
                        Log.d("MainActivity", "📝 İçerik uzunluğu: " +
                                (contentText != null ? contentText.length() : 0) + " karakter");
                        Log.d("MainActivity", "📝 İçerik önizleme: " +
                                (contentText != null ? contentText.substring(0, Math.min(100, contentText.length())) : "null"));
                    }
                }
            }

        } catch (Exception e) {
            Log.e("MainActivity", "❌ Debug hatası: " + e.getMessage());
        }
    }

}