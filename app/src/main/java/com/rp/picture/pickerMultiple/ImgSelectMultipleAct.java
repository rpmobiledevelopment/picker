package com.rp.picture.pickerMultiple;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Window;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.rp.picture.R;
import com.rp.picture.onPermission.OnPermission;
import com.rp.picture.pickerMultiple.imageCompression.ImageCompressionListener;
import com.rp.picture.pickerMultiple.imageCompression.ImageCompressionListenerArray;
import com.rp.picture.pickerMultiple.imageCompression.ImageCompressionTask;
import com.rp.picture.pickerMultiple.imagePicker.ImagePicker;
import com.rp.uihelpher.helpher.OnSnackBar;

import java.util.ArrayList;
import java.util.List;

public class ImgSelectMultipleAct extends AppCompatActivity {

    private String TAG = ImgSelectMultipleAct.class.getSimpleName();
    private static final int EXTERNAL_PERMISSION_CODE = 1234;
    private ImagePicker imagePicker;
    private boolean isCompress = true, isCamera = true, isGallery = true, isCameraYesNo = true;
    public static final String FLAG_COMPRESS = "flag_compress";
    public static final String FLAG_CAMERA = "flag_camera";
    public static final String FLAG_GALLERY = "flag_gallery";
    public static final String FLAG_CAMERA_YES_NO = "FLAG_CAMERA_YES_NO";
    public static final String RESULT_FILE_PATH = "result_file_path";

    private ProgressBar pb_load;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.act_image_cap);

        if (getIntent() != null) {
            isCompress = getIntent().getBooleanExtra(FLAG_COMPRESS,true);
            isCamera = getIntent().getBooleanExtra(FLAG_CAMERA,true);
            isGallery = getIntent().getBooleanExtra(FLAG_GALLERY,true);
            isCameraYesNo = getIntent().getBooleanExtra(FLAG_CAMERA_YES_NO,true);
        }

        pb_load = findViewById(R.id.pb_load);

        imagePicker = new ImagePicker();

        ActivityResultContracts.RequestMultiplePermissions multiplePermissionsContract
                = new ActivityResultContracts.RequestMultiplePermissions();
        ActivityResultLauncher<String[]> multiplePermissionLauncher
                = registerForActivityResult(multiplePermissionsContract, isGranted -> {
            if (isGranted.containsValue(true) && new OnPermission().isPermission(this, "STORAGE")) {
                imagePicker.withActivity(this).chooseFromGallery(isGallery).chooseFromCamera(isCamera, isCameraYesNo, false)
                        .withCompression(isCompress).start();
            } else {
                 new OnSnackBar(this, pb_load, "Permission declined");
            }
        });

        if (new OnPermission().isPermission(this, "STORAGE")) {
            imagePicker.withActivity(this).chooseFromGallery(isGallery)
                    .chooseFromCamera(isCamera, isCameraYesNo, false)
                    .withCompression(isCompress).start();
        } else {
            new OnPermission(multiplePermissionLauncher, "STORAGE");
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXTERNAL_PERMISSION_CODE) {
            if (grantResults.length == 2 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                imagePicker.withActivity(this).chooseFromGallery(isGallery).chooseFromCamera(isCamera, isCameraYesNo, false)
                        .withCompression(isCompress).start();
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ImagePicker.SELECT_IMAGE && resultCode == RESULT_OK) {
            ArrayList<String> filePaths = new ArrayList<>();

            if (data.getClipData() != null) { // Multiple images selected
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    String filePath = getRealPathFromURI(imageUri);
                    if (filePath != null) {
                        filePaths.add(filePath);
                    }
                }
            } else if (data.getData() != null) { // Single image selected
                Uri imageUri = data.getData();
                String filePath = getRealPathFromURI(imageUri);
                if (filePath != null) {
                    filePaths.add(filePath);
                }
            }
            if (!filePaths.isEmpty()) {
                new ImageCompressionTask(this, new ImageCompressionListener() {
                    @Override
                    public void onStart() {
                        Log.e(TAG, "Compression started...");
                    }

                    @Override
                    public void onCompressed(String filePath) {

                    }

                    @Override
                    public void onCompressed(List<String> compressedPaths) {
                        Log.e(TAG, "Compression finished: " + compressedPaths.toString());
                        Intent intent = new Intent();
                        intent.putStringArrayListExtra(RESULT_FILE_PATH, new ArrayList<>(compressedPaths));
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }).execute(filePaths);
            }
            // Log the selected image paths
            Log.e(TAG, "Selected Images: " + filePaths);

            // Return the list of image file paths
//            Intent intent = new Intent();
//            intent.putStringArrayListExtra(RESULT_FILE_PATH, filePaths);
//            setResult(RESULT_OK, intent);
//            finish();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
    private String getRealPathFromURI(Uri uri) {
        String result = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            if (index != -1) {
                cursor.moveToFirst();
                result = cursor.getString(index);
            }
            cursor.close();
        }
        return result;
    }
}