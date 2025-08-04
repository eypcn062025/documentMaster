package com.documentmaster.app.image;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class ImageProcessor {

    public static Bitmap optimize(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);

        if (ratio >= 1.0f) return bitmap; // zaten küçükse, değiştirme

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    public static String toBase64(Bitmap bitmap, String format) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Bitmap.CompressFormat compressFormat;
        if ("JPEG".equalsIgnoreCase(format)) {
            compressFormat = Bitmap.CompressFormat.JPEG;
        } else if ("PNG".equalsIgnoreCase(format)) {
            compressFormat = Bitmap.CompressFormat.PNG;
        } else {
            throw new IllegalArgumentException("Desteklenmeyen format: " + format);
        }

        bitmap.compress(compressFormat, 80, outputStream);
        byte[] byteArray = outputStream.toByteArray();

        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
}