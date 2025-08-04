package com.documentmaster.app.permission;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PermissionManager {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private final Context context;
    private final PermissionCallback callback;

    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    public PermissionManager(Context context, PermissionCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showPermissionDialog();
            } else {
                if (callback != null) {
                    callback.onPermissionGranted();
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions((android.app.Activity) context,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, PERMISSION_REQUEST_CODE);
            } else {
                if (callback != null) {
                    callback.onPermissionGranted();
                }
            }
        }
    }

    private void showPermissionDialog() {
        new MaterialAlertDialogBuilder(context)
                .setTitle("İzin Gerekli")
                .setMessage("DocumentMaster'ın dosyalarınıza erişebilmesi için izin vermeniz gerekiyor.")
                .setPositiveButton("İzin Ver", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + context.getPackageName()));
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton("İptal", (dialog, which) -> {
                    Toast.makeText(context, "İzin olmadan dosyalar görüntülenemez", Toast.LENGTH_LONG).show();
                    if (callback != null) {
                        callback.onPermissionDenied();
                    }
                })
                .show();
    }

    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                if (callback != null) {
                    callback.onPermissionGranted();
                }
            } else {
                Toast.makeText(context, "İzinler reddedildi. Uygulama düzgün çalışmayabilir.", Toast.LENGTH_LONG).show();
                if (callback != null) {
                    callback.onPermissionDenied();
                }
            }
        }
    }


    public void handleResumeCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            if (callback != null) {
                callback.onPermissionGranted();
            }
        }
    }
    public static int getPermissionRequestCode() {
        return PERMISSION_REQUEST_CODE;
    }
}