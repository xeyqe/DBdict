package com.xeyqe.myapplication;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import com.ichi2.anki.api.AddContentApi;

import static com.ichi2.anki.api.AddContentApi.READ_WRITE_PERMISSION;

public class PermissionChecker {
    private static final String[] STORAGE_READ_PERMISSION = { Manifest.permission.READ_EXTERNAL_STORAGE };
    private static final String[] STORAGE_WRITE_PERMISSION = { Manifest.permission.WRITE_EXTERNAL_STORAGE };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int AD_PERM_REQUEST = 0;


    public static void checkReadPermission(Activity activity) {
        if (shouldRequestPermission()) {
            int permission = ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        STORAGE_READ_PERMISSION,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
        }
    }

    public static boolean doIHaveReadPermission(Activity activity) {
        if (shouldRequestPermission()) {
            return ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED;
        } else
            return true;
    }

    public static void checkWritePermission(Activity activity) {
        if (shouldRequestPermission()) {
            int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        STORAGE_WRITE_PERMISSION,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
        }
    }

    public static boolean doIHaveWritePermission(Activity activity) {
        if (shouldRequestPermission()) {
            return ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED;
        } else
            return true;
    }

    public static void checkAnkiApiPermission(Activity activity) {
        if (isApiAvailable(activity)) {
            if  (shouldRequestPermission()) {
                int permission = ActivityCompat.checkSelfPermission(
                        activity.getApplication(),
                        READ_WRITE_PERMISSION);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            activity, new String[]{READ_WRITE_PERMISSION}, AD_PERM_REQUEST
                    );
                }
            }
        }
    }

    public static boolean doIHaveAnkiApiPermission(Activity activity) {
        if (isApiAvailable(activity)) {
            if (shouldRequestPermission()) {
                int permission = ActivityCompat.checkSelfPermission(
                        activity.getApplication(),
                        READ_WRITE_PERMISSION);
                return permission == PackageManager.PERMISSION_GRANTED;
            } else
                return true;
        } else
            return false;
    }

    private static boolean isApiAvailable(Context context) {
        return AddContentApi.getAnkiDroidPackageName(context) != null;
    }

    private static boolean shouldRequestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        } else
            return true;
    }
}
