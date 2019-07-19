package com.xeyqe.myapplication;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DictionaryImport extends MainActivity {
    private static final String[] STORAGE_PERMISSION = {Manifest.permission.READ_EXTERNAL_STORAGE};
    private String lng = "germanstina";
    private VocabViewModel vocabViewModel;


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




    public void readTextFile( String lng) {
        File file = new File(lng);


        BufferedReader reader = null;


        try {
            reader = new BufferedReader(new FileReader(file));

            String line = reader.readLine();

            while (line != null) {
                if (line.contains("___")) {
                    String[] separated = line.split("[_]{3}");

                    vocabViewModel.insert(new Vocab(separated[0], separated[1], lng, false));
                    line = reader.readLine();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
