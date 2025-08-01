package com.documentmaster.app;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Document {
    private String name;
    private String path;
    private String type;
    private long size;
    private Date lastModified;
    private boolean isEncrypted;
    private boolean isFavorite;

    public Document(String path) {
        File file = new File(path);
        this.path = path;
        this.name = file.getName();
        this.size = file.length();
        this.lastModified = new Date(file.lastModified());
        this.type = getFileType(file.getName());
        this.isEncrypted = false;
        this.isFavorite = false;
    }

    // Constructor for new documents
    public Document(String name, String type) {
        this.name = name;
        this.type = type;
        this.size = 0;
        this.lastModified = new Date();
        this.isEncrypted = false;
        this.isFavorite = false;
    }

    private String getFileType(String fileName) {
        if (fileName.contains(".")) {
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            switch (extension) {
                case "pdf": return "PDF";
                case "doc":
                case "docx": return "Word";
                case "xls":
                case "xlsx": return "Excel";
                case "ppt":
                case "pptx": return "PowerPoint";
                case "txt": return "Text";
                case "rtf": return "RTF";
                case "csv": return "CSV";
                default: return "Unknown";
            }
        }
        return "Unknown";
    }

    public String getFormattedSize() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(lastModified);
    }

    public int getTypeIcon() {
        switch (type) {
            case "PDF": return android.R.drawable.ic_menu_edit;
            case "Word": return android.R.drawable.ic_menu_edit;
            case "Excel": return android.R.drawable.ic_menu_edit;
            case "PowerPoint": return android.R.drawable.ic_menu_edit;
            case "Text": return android.R.drawable.ic_menu_edit;
            default: return android.R.drawable.ic_menu_agenda;
        }
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }

    public boolean isEncrypted() { return isEncrypted; }
    public void setEncrypted(boolean encrypted) { isEncrypted = encrypted; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}