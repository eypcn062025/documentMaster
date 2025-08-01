package com.documentmaster.app;

import androidx.multidex.MultiDexApplication;

public class DocumentMasterApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        // Apache POI kütüphanesi için gerekli initialization
    }
}