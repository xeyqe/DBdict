package com.xeyqe.myapplication;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DictionaryImport {
    private static final String[] STORAGE_PERMISSION = {Manifest.permission.READ_EXTERNAL_STORAGE};

    public static void checkPermission(Activity activity) {
        int permissionExternalMemory = ActivityCompat.checkSelfPermission(
                GlobalApplication.getAppContext(), Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionExternalMemory != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    STORAGE_PERMISSION,
                    1
            );
        }
    }
}
