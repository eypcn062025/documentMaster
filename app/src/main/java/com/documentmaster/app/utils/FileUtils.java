package com.documentmaster.app.utils;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
public class FileUtils {
    private static final String TAG = "FileUtils";

    public static void copyFile(File source, File destination) throws Exception {
        Log.d(TAG, "🔄 Dosya kopyalanıyor: " + source.getName() + " → " + destination.getName());

        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(destination)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
        }
    }

}
