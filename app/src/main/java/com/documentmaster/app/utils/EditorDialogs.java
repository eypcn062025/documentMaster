package com.documentmaster.app.utils;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.documentmaster.app.R;
import com.documentmaster.app.web.WebViewBridge;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EditorDialogs {

    private final Context context;
    private final WebViewBridge webViewBridge;


    private String currentTextColor = "#000000";
    private String currentFontFamily = "Arial";

    public EditorDialogs(Context context, WebViewBridge webViewBridge) {
        this.context = context;
        this.webViewBridge = webViewBridge;
    }

    public void showColorPickerDialog() {
        String[] colorNames = {"Siyah", "Kırmızı", "Yeşil", "Mavi", "Sarı", "Magenta", "Cyan", "Turuncu", "Mor", "Kahverengi"};
        String[] colorValues = {"#000000", "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#FFA500", "#800080", "#8B4513"};

        new MaterialAlertDialogBuilder(context)
                .setTitle("Yazı Rengi Seçin")
                .setItems(colorNames, (dialog, which) -> {
                    currentTextColor = colorValues[which];
                    webViewBridge.executeJS("setTextColor('" + currentTextColor + "')");
                    Toast.makeText(context, colorNames[which] + " renk seçildi", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    public void showFontSizeDialog() {
        View dialogView = ((android.app.Activity) context).getLayoutInflater().inflate(R.layout.dialog_font_size, null);
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

        new MaterialAlertDialogBuilder(context)
                .setTitle("Yazı Boyutu")
                .setView(dialogView)
                .setPositiveButton("Uygula", (dialog, which) -> {
                    int selectedSize = seekBarSize.getProgress();
                    webViewBridge.executeJS("setFontSize(" + selectedSize + ")");
                    Toast.makeText(context, "Font boyutu: " + sizeNames[selectedSize], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    public void showFontDialog() {
        String[] fonts = {"Arial", "Times New Roman", "Courier New", "Georgia", "Verdana", "Comic Sans MS", "Impact"};

        new MaterialAlertDialogBuilder(context)
                .setTitle("Yazı Tipi Seçin")
                .setItems(fonts, (dialog, which) -> {
                    currentFontFamily = fonts[which];
                    webViewBridge.executeJS("setFontName('" + currentFontFamily + "')");
                    Toast.makeText(context, "Font: " + currentFontFamily, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    public void showInsertTableDialog() {
        View dialogView = ((android.app.Activity) context).getLayoutInflater().inflate(R.layout.dialog_insert_table, null);
        EditText editRows = dialogView.findViewById(R.id.editRows);
        EditText editCols = dialogView.findViewById(R.id.editCols);

        editRows.setText("3");
        editCols.setText("3");

        new MaterialAlertDialogBuilder(context)
                .setTitle("Tablo Ekle")
                .setView(dialogView)
                .setPositiveButton("Ekle", (dialog, which) -> {
                    try {
                        int rows = Integer.parseInt(editRows.getText().toString());
                        int cols = Integer.parseInt(editCols.getText().toString());

                        if (rows > 0 && cols > 0 && rows <= 20 && cols <= 10) {
                            webViewBridge.executeJS("insertTable(" + rows + ", " + cols + ")");
                            Toast.makeText(context, "Tablo eklendi", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Geçersiz tablo boyutu", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Geçersiz sayı", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    // ========== GETTER METHODS (opsiyonel) ==========

    public String getCurrentTextColor() {
        return currentTextColor;
    }

    public String getCurrentFontFamily() {
        return currentFontFamily;
    }

    public void setCurrentTextColor(String color) {
        this.currentTextColor = color;
    }

    public void setCurrentFontFamily(String fontFamily) {
        this.currentFontFamily = fontFamily;
    }
}