package com.xeyqe.myapplication;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DictionaryImport {
    private static final String[] STORAGE_PERMISSION = {Manifest.permission.READ_EXTERNAL_STORAGE};
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

    public void readTextFile(Uri uri, String mLng) {

        ArrayList<Vocab> listOfVocabs = new ArrayList<>();

        InputStream inputStream = null;
        try {
            inputStream = GlobalApplication.getAppContext().getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith("##")) {
                    String[] separated = line.split("\t");
                    listOfVocabs.add(new Vocab(separated[0], separated[1].replaceAll("\\\\n", "<br>"), mLng, false));
                }
                line = reader.readLine();
            }
            vocabViewModel.insertAll(listOfVocabs);

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
