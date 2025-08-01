package com.documentmaster.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileManagerHelper {

    private static final String TAG = "FileManagerHelper";

    public static String getRealPathFromURI(Context context, Uri uri) {
        String realPath = null;

        if (DocumentsContract.isDocumentUri(context, uri)) {
            realPath = getPathFromDocumentUri(context, uri);
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            realPath = getPathFromContentUri(context, uri);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            realPath = uri.getPath();
        }

        Log.d(TAG, "URI: " + uri.toString());
        Log.d(TAG, "Real Path: " + realPath);

        return realPath;
    }

    private static String getPathFromDocumentUri(Context context, Uri uri) {
        try {
            String documentId = DocumentsContract.getDocumentId(uri);

            if (isExternalStorageDocument(uri)) {
                String[] split = documentId.split(":");
                String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return context.getExternalFilesDir(null) + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {

                return getPathFromDownloadsProvider(context, documentId);
            } else if (isMediaDocument(uri)) {

                return getPathFromMediaProvider(context, documentId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Document URI parse hatası: " + e.getMessage());
        }

        return null;
    }

    private static String getPathFromContentUri(Context context, Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Content URI parse hatası: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    private static String getPathFromDownloadsProvider(Context context, String documentId) {
        try {
            if (documentId.startsWith("raw:")) {
                return documentId.replaceFirst("raw:", "");
            }

            Uri contentUri = Uri.parse("content://downloads/public_downloads");
            contentUri = Uri.withAppendedPath(contentUri, documentId);

            return getPathFromContentUri(context, contentUri);
        } catch (Exception e) {
            Log.e(TAG, "Downloads provider hatası: " + e.getMessage());
        }

        return null;
    }

    private static String getPathFromMediaProvider(Context context, String documentId) {
        try {
            String[] split = documentId.split(":");
            String type = split[0];
            String id = split[1];

            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            } else if ("document".equals(type)) {
                contentUri = MediaStore.Files.getContentUri("external");
            }

            if (contentUri != null) {
                String selection = "_id=?";
                String[] selectionArgs = {id};

                return getPathFromContentUri(context, Uri.withAppendedPath(contentUri, id));
            }
        } catch (Exception e) {
            Log.e(TAG, "Media provider hatası: " + e.getMessage());
        }

        return null;
    }

    public static String getFileNameFromUri(Context context, Uri uri) {
        String fileName = null;

        if ("content".equals(uri.getScheme())) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (fileName == null) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }

        return fileName;
    }

    public static String copyFileFromUri(Context context, Uri uri) {
        try {
            String fileName = getFileNameFromUri(context, uri);
            if (fileName == null) {
                fileName = "temp_document.docx";
            }

            File tempDir = new File(context.getCacheDir(), "temp_documents");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            File tempFile = new File(tempDir, fileName);

            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            Log.d(TAG, "Dosya temp'e kopyalandı: " + tempFile.getAbsolutePath());
            return tempFile.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Dosya kopyalama hatası: " + e.getMessage());
            return null;
        }
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static long getFileSizeFromUri(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Dosya boyutu alma hatası: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }
}