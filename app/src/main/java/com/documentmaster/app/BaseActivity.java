package com.documentmaster.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public abstract class BaseActivity extends AppCompatActivity {

    protected SharedPreferences preferences;
    public static final String PREFS_NAME = "DocumentMasterPrefs";
    public static final String PREF_DARK_MODE = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean isDarkMode = preferences.getBoolean(PREF_DARK_MODE, false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    protected void toggleDarkMode() {
        boolean currentMode = preferences.getBoolean(PREF_DARK_MODE, false);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_DARK_MODE, !currentMode);
        editor.apply();


        if (!currentMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        recreate();
    }

    protected boolean isDarkModeEnabled() {
        return preferences.getBoolean(PREF_DARK_MODE, false);
    }
}
