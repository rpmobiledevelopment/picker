package com.rp.picture.onPermission;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

public class AppPermission {

    private String TAG = PermissionConstant.class.getSimpleName();

    public AppPermission() { }

    public AppPermission(Activity mActivity,FragmentManager supportFragmentManager, String permission) {
        // Add PermissionsFragment dynamically if it's not already added
        if (supportFragmentManager.findFragmentByTag("PermissionsFragment") == null) {
            supportFragmentManager.beginTransaction()
                    .add(new PermissionsFragment(), "PermissionsFragment")
                    .commitNow();
        }

        // Request permissions using the fragment
        PermissionsFragment fragment = (PermissionsFragment) supportFragmentManager.findFragmentByTag("PermissionsFragment");
        if (fragment != null) {
            fragment.requestPermissions(mActivity,permission);
        }
    }

    public AppPermission(FragmentManager supportFragmentManager, String permission) {
        // Add PermissionsFragment dynamically if it's not already added
        if (supportFragmentManager.findFragmentByTag("PermissionsFragment") == null) {
            supportFragmentManager.beginTransaction()
                    .add(new PermissionsFragment(), "PermissionsFragment")
                    .commitNow();
        }

        // Request permissions using the fragment
        PermissionsFragment fragment = (PermissionsFragment) supportFragmentManager.findFragmentByTag("PermissionsFragment");
        if (fragment != null) {
            fragment.requestPermissions(permission);
        }
    }


    public boolean isPermission(Context mActivity, String opt) {

        int currentAPIVersion = Build.VERSION.SDK_INT;

        switch (opt) {
            case "DOWNLOAD_ACCESS_":
            case "DOWNLOAD_ACCESS":
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                        ((ContextCompat.checkSelfPermission(mActivity, CAMERA) == PackageManager.PERMISSION_GRANTED))
                        : ((ContextCompat.checkSelfPermission(mActivity, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(mActivity, CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(mActivity, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
            case "NOTIFICATION":
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ((ContextCompat.checkSelfPermission(mActivity, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED));
            case "RECORD_AUDIO":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ((ContextCompat.checkSelfPermission(mActivity, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED));
                }else {
                    return ((ContextCompat.checkSelfPermission(mActivity, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                            (ContextCompat.checkSelfPermission(mActivity, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) &&
                            (ContextCompat.checkSelfPermission(mActivity, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
                }

            case "LOCATION":
                return currentAPIVersion < Build.VERSION_CODES.M ||
                        ((ContextCompat.checkSelfPermission(mActivity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                                (ContextCompat.checkSelfPermission(mActivity, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED));
            default:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                        ((ContextCompat.checkSelfPermission(mActivity, READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) &&
                                (ContextCompat.checkSelfPermission(mActivity, READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) &&
                                (ContextCompat.checkSelfPermission(mActivity, CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                                (ContextCompat.checkSelfPermission(mActivity, READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED))
                        : ((ContextCompat.checkSelfPermission(mActivity, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(mActivity, CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(mActivity, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
        }
    }

}
