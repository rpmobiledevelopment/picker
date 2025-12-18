package com.rp.picture.cameraImage;

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
import com.rp.picture.cameraImage.imageCompression.ImageCompression;
import com.rp.picture.cameraImage.imageCompression.ImageCompressionListener;
import com.rp.picture.cameraImage.imagePicker.ImagePicker;
import com.rp.picture.onPermission.OnPermission;
import com.rp.uihelpher.helpher.OnSnackBar;
import com.rp.uihelpher.log.IsLog;

import java.util.ArrayList;
import java.util.List;

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
            isMultiple = getIntent().getBooleanExtra(FLAG_IS_MULTIPLE, false);
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
        if (requestCode == ImagePicker.SELECT_IMAGE && resultCode == RESULT_OK) {

            if (isMultiple) {
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
                    new ImageCompression(this, new ImageCompressionListener() {
                        @Override
                        public void onStart() {
                            Log.e(TAG, "Compression started...");
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

                        @Override
                        public void onCompressed(List<String> filePaths) {
                            Log.e(TAG, "Compression finished: " + filePaths.toString());
                            Intent intent = new Intent();
                            intent.putStringArrayListExtra(RESULT_FILE_PATH, new ArrayList<>(filePaths));
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }).execute(filePaths);
                }
                // Log the selected image paths
                Log.e(TAG, "Selected Images: " + filePaths);
            }else {
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

                    @Override
                    public void onCompressed(List<String> filePaths) {
                        Log.e(TAG, "Compression finished: " + filePaths.toString());
                        Intent intent = new Intent();
                        intent.putStringArrayListExtra(RESULT_FILE_PATH, new ArrayList<>(filePaths));
                        setResult(RESULT_OK, intent);
                        finish();
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
            }
        }else {
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