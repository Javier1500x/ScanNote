package com.scannote.app;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class ScanNoteApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        com.scannote.app.utils.DocumentParser.init(this);
    }
}
