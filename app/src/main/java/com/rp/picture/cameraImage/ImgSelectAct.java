package com.rp.picture.cameraImage;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Window;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.rp.picture.R;
import com.rp.picture.cameraImage.imageCompression.ImageCompressionListener;
import com.rp.picture.cameraImage.imagePicker.ImagePicker;
import com.rp.picture.onPermission.OnPermission;
import com.rp.uihelpher.helpher.OnSnackBar;
import com.rp.uihelpher.log.IsLog;

public class ImgSelectAct extends AppCompatActivity {

    private String TAG = ImgSelectAct.class.getSimpleName();
    private static final int EXTERNAL_PERMISSION_CODE = 1234;
    private ImagePicker imagePicker;
    private boolean isCompress = true, isCamera = true, isGallery = true, isCameraYesNo = true,
            isMultiple = false;
    private int isMultipleCount = 5;
    public static final String FLAG_COMPRESS = "flag_compress";
    public static final String FLAG_CAMERA = "flag_camera";
    public static final String FLAG_GALLERY = "flag_gallery";
    public static final String FLAG_CAMERA_YES_NO = "FLAG_CAMERA_YES_NO";
    public static final String FLAG_IS_MULTIPLE = "FLAG_IS_MULTIPLE";
    public static final String FLAG_IS_MULTIPLE_IMAGE_COUNT = "FLAG_IS_MULTIPLE_IMAGE_COUNT";
    public static final String RESULT_FILE_PATH = "result_file_path";

    private ProgressBar pb_load;
    private ActivityResultContracts.RequestMultiplePermissions multiplePermissionsContract;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;

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
            isMultiple = getIntent().getBooleanExtra(FLAG_IS_MULTIPLE,true);
            isMultipleCount = getIntent().getIntExtra(FLAG_IS_MULTIPLE_IMAGE_COUNT,5);
        }

        pb_load = findViewById(R.id.pb_load);

        imagePicker = new ImagePicker();

        multiplePermissionsContract = new ActivityResultContracts.RequestMultiplePermissions();
        multiplePermissionLauncher = registerForActivityResult(multiplePermissionsContract, isGranted -> {
            if (isGranted.containsValue(true) && new OnPermission().isPermission(this, "CAMERA_ACCESS")) {
                onAccessFile();
            } else {
                new OnSnackBar(this, pb_load, "Permission declined");
            }
        });

        onAccessFile();
    }

    private void onAccessFile() {
        if (isCameraYesNo) {
            if (new OnPermission().isPermission(this, "CAMERA_ACCESS")) {
                if (isMultiple) {
                    imagePicker.withActivity(this).chooseFromGallery(isGallery)
                            .chooseFromCamera(isCamera, isCameraYesNo, false)
                            .withCompression(isCompress).isMultiple(isMultiple,isMultipleCount).start();
                }else {
                    imagePicker.withActivity(this).chooseFromGallery(isGallery)
                            .chooseFromCamera(isCamera, isCameraYesNo, false)
                            .withCompression(isCompress).start();
                }

            } else {
                new OnPermission(multiplePermissionLauncher, "CAMERA_ACCESS");
            }
        }else {
            if (isMultiple) {
                imagePicker.withActivity(this).chooseFromGallery(isGallery)
                        .chooseFromCamera(isCamera, isCameraYesNo, false)
                        .withCompression(isCompress).isMultiple(isMultiple,isMultipleCount).start();
            }else {
                imagePicker.withActivity(this).chooseFromGallery(isGallery)
                        .chooseFromCamera(isCamera, isCameraYesNo, false)
                        .withCompression(isCompress).start();
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXTERNAL_PERMISSION_CODE) {
            if (grantResults.length == 2 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                onAccessFile();
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ImagePicker.SELECT_IMAGE) {

            if (resultCode == RESULT_OK) {
                imagePicker.addOnCompressListener(new ImageCompressionListener() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onCompressed(String filePath) {
                        if (filePath != null && isCompress) {
                            // return filepath
                            Intent intent = new Intent();
                            intent.putExtra(RESULT_FILE_PATH, filePath);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }
                });
                String filePath = imagePicker.getImageFilePath(data);
                new IsLog(TAG,"filePath======================================"+filePath);

                if (filePath != null && !isCompress) {
                    //return filepath
                    Intent intent = new Intent();
                    intent.putExtra(RESULT_FILE_PATH, filePath);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

}