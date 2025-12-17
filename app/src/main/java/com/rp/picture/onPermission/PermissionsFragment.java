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
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class PermissionsFragment extends Fragment {

    String TAG = PermissionsFragment.class.getSimpleName();
    private PermissionCallback callback;

    public interface PermissionCallback {
        void onPermissionsResult(boolean allPermissionsGranted);
    }

    private final ActivityResultLauncher<String[]> multiplePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = !permissions.containsValue(false);
                if (callback != null) {
                    callback.onPermissionsResult(allGranted);
                }
            });

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof PermissionCallback) {
            callback = (PermissionCallback) context;
        }
    }

    public void requestPermissions(Activity mActivity,String opt) {
        switch (opt) {
            case "DOWNLOAD_ACCESS_":
            case "DOWNLOAD_ACCESS":
                multiplePermissionLauncher.launch(new String[]{ CAMERA });
                break;
            case "NOTIFICATION":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    multiplePermissionLauncher.launch(new String[]{ POST_NOTIFICATIONS });
                }
                break;
            case "RECORD_AUDIO":
                multiplePermissionLauncher.launch(new String[]{ RECORD_AUDIO });
                break;
            case "LOCATION":
                if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity,ACCESS_FINE_LOCATION)) {
                    buildAlertMessageNoGps(mActivity);
                } else {
                    multiplePermissionLauncher.launch(new String[]{ ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION});
                }
                break;
            default:
                multiplePermissionLauncher.launch(new String[]{ CAMERA });
                break;
        }
    }

    public void requestPermissions(String opt) {
        switch (opt) {
            case "DOWNLOAD_ACCESS_":
            case "DOWNLOAD_ACCESS":
                multiplePermissionLauncher.launch(new String[]{ CAMERA });
                break;
            case "NOTIFICATION":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    multiplePermissionLauncher.launch(new String[]{ POST_NOTIFICATIONS });
                }
                break;
            case "RECORD_AUDIO":
                multiplePermissionLauncher.launch(new String[]{ RECORD_AUDIO });
                break;
            default:
                multiplePermissionLauncher.launch(new String[]{ CAMERA });
                break;
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
                })
                .setNegativeButton("No", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }

}
