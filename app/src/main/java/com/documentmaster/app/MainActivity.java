package com.documentmaster.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.documentmaster.app.document.CreateDocument;
import com.documentmaster.app.document.DocumentMenuOperations;
import com.documentmaster.app.document.LoadDocument;
import com.documentmaster.app.document.OpenDocument;

import com.documentmaster.app.permission.PermissionManager;
import com.documentmaster.app.utils.ActionDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements DocumentMenuOperations.DocumentOperationCallback, CreateDocument.CreateDocumentCallback, LoadDocument.LoadDocumentCallback,
        OpenDocument.OpenDocumentCallback, PermissionManager.PermissionCallback{
    private Toolbar toolbar;
    private MaterialButton btnCreate, btnOpenFile, btnViewAll;
    private FloatingActionButton fab;
    private RecyclerView recyclerViewDocuments;
    private LinearLayout emptyState;
    private DocumentAdapter documentAdapter;
    private DocumentMenuOperations documentOperations;
    private CreateDocument createDocument;
    private LoadDocument loadDocument;
    private OpenDocument openDocument;
    private PermissionManager permissionManager;
    private ActionDialog actionDialog;
    private List<Document> documentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        documentOperations = new DocumentMenuOperations(this, this);
        createDocument = new CreateDocument(this, this);
        loadDocument = new LoadDocument(this, this);
        openDocument = new OpenDocument(this, this);
        permissionManager = new PermissionManager(this, this);
        permissionManager.checkPermissions();
        actionDialog = new ActionDialog(this, createDocument, openDocument, loadDocument);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        documentOperations = null;
        createDocument = null;
        loadDocument = null;
        openDocument = null;
        permissionManager = null;
        if (documentList != null) {
            documentList.clear();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        permissionManager.handleResumeCheck();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        btnCreate = findViewById(R.id.btnCreate);
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
                documentOperations.openDocument(document);
            }

            @Override
            public void onDocumentLongClick(Document document) {
                documentOperations.showDocumentOptionsMenu(document);
            }
        });
        recyclerViewDocuments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDocuments.setAdapter(documentAdapter);
    }

    private void setupListeners() {
        btnCreate.setOnClickListener(v -> createDocument.showCreateNewDocumentDialog());
        btnOpenFile.setOnClickListener(v -> openDocument.openFilePicker());
        btnViewAll.setOnClickListener(v -> {});
        fab.setOnClickListener(v -> actionDialog.showQuickActionDialog());
    }

    //----------permission-----------//
    @Override
    public void onPermissionGranted() {
        loadDocument.loadDocuments();
    }

    @Override
    public void onPermissionDenied() {
        loadDocument.loadInternalDocuments();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionManager.handlePermissionResult(requestCode, permissions, grantResults);
    }

    //-----------create---------------//
    @Override
    public void onDocumentCreated() {
        loadDocument.loadDocuments();
    }

    //-------menuOperation-------//

    @Override
    public void onDocumentDeleted(Document document) {
        documentList.remove(document);
        documentAdapter.notifyDataSetChanged();
        updateUI();
    }

    @Override
    public void onDocumentRenamed(Document document) {
        documentAdapter.notifyDataSetChanged();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OpenDocument.getPickFileRequestCode() && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                Log.d("MainActivity", "Seçilen dosya URI: " + selectedFileUri.toString());
                openDocument.processSelectedFile(selectedFileUri);
            } else {
                Toast.makeText(this, "Dosya seçilemedi", Toast.LENGTH_SHORT).show();
            }
        }
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

    @Override
    public void onDocumentsLoaded(List<Document> documents) {
        documentList.clear();
        documentList.addAll(documents);
        updateUI();
    }

    @Override
    public void onDocumentLoadFailed(String error) {
        Toast.makeText(this, "Belgeler yüklenemedi: " + error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDocumentOpened(String filePath) {
        loadDocument.loadDocuments();
    }

}