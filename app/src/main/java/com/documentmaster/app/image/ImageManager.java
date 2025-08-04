package com.documentmaster.app.image;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import com.documentmaster.app.image.ImageProcessor;
import com.documentmaster.app.web.WebViewBridge;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.InputStream;
public class ImageManager {
    private static final String TAG = "ImageManager";
    public static final int PICK_IMAGE_REQUEST = 2001;
    public static final int CAPTURE_IMAGE_REQUEST = 2002;

    private final Activity activity;
    private final WebViewBridge webViewBridge;
    private ImageOperationCallback callback;

    public ImageManager(Activity activity, WebViewBridge webViewBridge) {
        this.activity = activity;
        this.webViewBridge = webViewBridge;
    }

    public void setCallback(ImageOperationCallback callback) {
        this.callback = callback;
    }

    public void showImageInsertOptions() {
        String[] options = {"Galeriden Seç", "Kamera ile Çek", "İptal"};

        new MaterialAlertDialogBuilder(activity)
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
            activity.startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "Galeri açma hatası: " + e.getMessage());
            showToast("Galeri açılamadı: " + e.getMessage());
        }
    }


    private void openCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
            } else {
                showToast("Kamera uygulaması bulunamadı");
            }
        } catch (Exception e) {
            Log.e(TAG, "Kamera açma hatası: " + e.getMessage());
            showToast("Kamera açılamadı: " + e.getMessage());
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return false;
        }

        switch (requestCode) {
            case PICK_IMAGE_REQUEST:
                if (data != null && data.getData() != null) {
                    processSelectedImage(data.getData(), "Galeri Resmi");
                    return true;
                }
                break;

            case CAPTURE_IMAGE_REQUEST:
                if (data != null && data.getExtras() != null) {
                    Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                    if (imageBitmap != null) {
                        processCapturedImage(imageBitmap);
                        return true;
                    }
                }
                break;
        }

        return false;
    }

    private void processSelectedImage(Uri imageUri, String imageName) {
        new Thread(() -> {
            try {
                Log.d(TAG, "🖼️ Resim işleniyor: " + imageUri.toString());

                InputStream inputStream = activity.getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                if (bitmap != null) {
                    // Resmi optimize et
                    Bitmap optimizedBitmap = ImageProcessor.optimize(bitmap, 800, 600);

                    // Base64'e çevir
                    String base64Data = ImageProcessor.toBase64(optimizedBitmap, "PNG");
                    String mimeType = "image/png";

                    activity.runOnUiThread(() -> {
                        insertImageToEditor(base64Data, mimeType, imageName);
                    });

                    Log.d(TAG, "✅ Resim başarıyla işlendi ve eklendi");
                } else {
                    activity.runOnUiThread(() -> {
                        showErrorToast("Resim yüklenemedi");
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Resim işleme hatası: " + e.getMessage());
                activity.runOnUiThread(() -> {
                    showErrorToast("Resim işleme hatası: " + e.getMessage());
                });
            }
        }).start();
    }


    private void processCapturedImage(Bitmap bitmap) {
        new Thread(() -> {
            try {
                Log.d(TAG, "📷 Çekilen resim işleniyor...");

                // Resmi optimize et
                Bitmap optimizedBitmap = ImageProcessor.optimize(bitmap, 800, 600);

                // Base64'e çevir
                String base64Data = ImageProcessor.toBase64(optimizedBitmap, "JPEG");
                String mimeType = "image/jpeg";

                activity.runOnUiThread(() -> {
                    insertImageToEditor(base64Data, mimeType, "Kamera resmi");
                });

                Log.d(TAG, "✅ Kamera resmi başarıyla eklendi");

            } catch (Exception e) {
                Log.e(TAG, "❌ Kamera resmi işleme hatası: " + e.getMessage());
                activity.runOnUiThread(() -> {
                    showErrorToast("Kamera resmi işleme hatası: " + e.getMessage());
                });
            }
        }).start();
    }


    private void insertImageToEditor(String base64Data, String mimeType, String imageName) {
        webViewBridge.insertImage(base64Data, mimeType, imageName,
                () -> {
                    showSuccessToast(imageName + " eklendi");
                    if (callback != null) {
                        callback.onImageInserted();
                    }
                },
                () -> showErrorToast("Resim eklenemedi")
        );
    }


    public void showImageInfo() {
        if (webViewBridge == null) {
            showWarningToast("Editör henüz hazır değil");
            return;
        }

        webViewBridge.getImageInfo((imageCount, validationResult) -> {
            Log.d(TAG, "📊 Resim doğrulama sonucu: " + validationResult);

            String currentFileName = "Bilinmeyen";
            String contentStatus = "Bilinmeyen";

            if (callback != null) {
                currentFileName = callback.getCurrentFileName();
                contentStatus = callback.isContentChanged() ? "Değiştirildi" : "Kaydedildi";
            }

            String message = "📊 Resim Bilgileri:\n\n" +
                    "📷 Toplam Resim: " + imageCount + "\n" +
                    "📁 Belge: " + currentFileName + "\n" +
                    "💾 Durum: " + contentStatus;

            new MaterialAlertDialogBuilder(activity)
                    .setTitle("🖼️ Resim Bilgileri")
                    .setMessage(message)
                    .setPositiveButton("Tamam", null)
                    .setNeutralButton("Resimleri Doğrula", (dialog, which) -> {
                        webViewBridge.validateImageIntegrity();
                        showToast("🔍 Resimler doğrulandı (konsola bakın)");
                    })
                    .show();
        });
    }

    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccessToast(String message) {
        Toast.makeText(activity, "✅ " + message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorToast(String message) {
        Toast.makeText(activity, "❌ " + message, Toast.LENGTH_SHORT).show();
    }

    private void showWarningToast(String message) {
        Toast.makeText(activity, "⚠️ " + message, Toast.LENGTH_SHORT).show();
    }

    public interface ImageOperationCallback {
        void onImageInserted();
        String getCurrentFileName();
        boolean isContentChanged();
    }

}
