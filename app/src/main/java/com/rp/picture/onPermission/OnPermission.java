package com.rp.picture.onPermission;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class OnPermission {
    private String TAG = OnPermission.class.getSimpleName();

    public OnPermission() {}

    public OnPermission(ActivityResultLauncher<String[]> multiplePermissionLauncher,
                        String opt) {
        switch (opt) {
            case "NOTIFICATION":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    multiplePermissionLauncher.launch(new String[]{POST_NOTIFICATIONS});
                }
                break;
            case "CAMERA_ACCESS":
                multiplePermissionLauncher.launch(new String[]{ CAMERA });
                break;
            case "RECORD_AUDIO":
                multiplePermissionLauncher.launch(new String[]{ RECORD_AUDIO });
                break;
            case "LOCATION":
                multiplePermissionLauncher.launch( new String[] { ACCESS_FINE_LOCATION,
                        ACCESS_COARSE_LOCATION } );
                break;
            default:
                multiplePermissionLauncher.launch(new String[]{ CAMERA });
                break;
        }
    }


    public OnPermission(Activity mActivity, ActivityResultLauncher<String[]> multiplePermissionLauncher,
                        String opt) {
        switch (opt) {
            case "DOWNLOAD_ACCESS":
                multiplePermissionLauncher.launch(new String[]{ CAMERA });
                break;
            case "NOTIFICATION":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    multiplePermissionLauncher.launch(new String[]{POST_NOTIFICATIONS});
                }
                break;
            case "LOCATION":
                if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity,ACCESS_FINE_LOCATION)) {
                    buildAlertMessageNoGps(mActivity);
                } else {
                    multiplePermissionLauncher.launch(new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION});
                }

                break;
            default:
                multiplePermissionLauncher.launch(new String[]{ CAMERA });
                break;
        }
    }


    public boolean isPermission(Activity mActivity, String opt) {

        int currentAPIVersion = Build.VERSION.SDK_INT;

        switch (opt) {
            case "NOTIFICATION":
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ((ContextCompat.checkSelfPermission(mActivity, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED));
            case "LOCATION":
                return currentAPIVersion < Build.VERSION_CODES.M ||
                        ((ContextCompat.checkSelfPermission(mActivity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                                (ContextCompat.checkSelfPermission(mActivity, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED));
            default:
                return (ContextCompat.checkSelfPermission(mActivity, CAMERA) == PackageManager.PERMISSION_GRANTED);
        }
    }
    public boolean isPermission(Context mActivity, String opt) {

        int currentAPIVersion = Build.VERSION.SDK_INT;

        switch (opt) {
            case "DOWNLOAD_ACCESS":
                return (ContextCompat.checkSelfPermission(mActivity, CAMERA) == PackageManager.PERMISSION_GRANTED);
            case "NOTIFICATION":
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ((ContextCompat.checkSelfPermission(mActivity, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED));
            case "RECORD_AUDIO":
                return (ContextCompat.checkSelfPermission(mActivity, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
            case "LOCATION":
                return currentAPIVersion < Build.VERSION_CODES.M ||
                        ((ContextCompat.checkSelfPermission(mActivity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                                (ContextCompat.checkSelfPermission(mActivity, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED));
            default:
                return (ContextCompat.checkSelfPermission(mActivity, CAMERA) == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void buildAlertMessageNoGps(Activity mActivity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", mActivity.getPackageName(), null);
                    intent.setData(uri);
                    mActivity.startActivity(intent);

                }).setNegativeButton("No", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }

}
