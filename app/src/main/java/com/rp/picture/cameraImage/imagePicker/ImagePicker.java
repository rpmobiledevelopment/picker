package com.rp.picture.cameraImage.imagePicker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.rp.picture.cameraImage.imageCompression.ImageCompression;
import com.rp.picture.cameraImage.imageCompression.ImageCompressionListener;
import com.rp.picture.onPermission.OnPermission;
import com.rp.uihelpher.log.IsLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ImagePicker {

    private String TAG = ImagePicker.class.getSimpleName();
    private Activity activity;
    private Fragment fragment;
    private boolean isCompress = true, isCamera = true, isGallery = true,isCameraYesNo = true,
            fontCameraOPT = false,isMultiple = false;
    public static final int SELECT_IMAGE = 121;
    public int selectedImage = 5;
    private ImageCompressionListener imageCompressionListener;

    public ImagePicker withActivity(Activity activity) {
        this.activity = activity;
        return this;
    }

    public ImagePicker withFragment(Fragment fragment) {
        this.fragment = fragment;
        return this;
    }

    public ImagePicker chooseFromCamera(boolean isCamera, boolean isCameraYesNo,boolean fontCameraOPT) {
        this.isCamera = isCamera;
        this.isCameraYesNo = isCameraYesNo;
        this.fontCameraOPT = fontCameraOPT;
        return this;
    }

    public ImagePicker chooseFromGallery(boolean isGallery) {
        this.isGallery = isGallery;
        return this;
    }

    public ImagePicker withCompression(boolean isCompress) {
        this.isCompress = isCompress;
        return this;
    }

    public ImagePicker isMultiple(boolean isMultiple,int selectPos) {
        this.isMultiple = isMultiple;
        this.selectedImage = selectPos;
        return this;
    }

    public void start() {
        if (activity != null && fragment != null) {
            throw new IllegalStateException("Cannot add both activity and fragment");
        } else if (activity == null && fragment == null) {
            throw new IllegalStateException("Activity and fragment both are null");
        } else {
            if (new OnPermission().isPermission(activity,"STORAGE")) {
                if (!isCamera && !isGallery) {
                    throw new IllegalStateException("select source to pick image");
                } else {
                    if (activity != null)
                        activity.startActivityForResult(getPickImageChooserIntent(), SELECT_IMAGE);
                    else
                        fragment.startActivityForResult(getPickImageChooserIntent(), SELECT_IMAGE);
                }
            }else {
                throw new IllegalStateException("Write External Permission not found");
            }
        }
    }

    private Intent getPickImageChooserIntent() {

        // Determine Uri of camera image to save.
        Uri outputFileUri = getCaptureImageOutputUri();

        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = activity != null ? activity.getPackageManager() :
                fragment.getActivity().getPackageManager();

        if (!isCameraYesNo) {
            if (isGallery) {
                Intent intent = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2) {

                    intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                    if (isMultiple) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, selectedImage); // Limit selection to 10
                    }
                } else {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                }
                intent.setType("image/*");
                allIntents.add(intent);


            }
        }


        if (isCameraYesNo) {
            if (isCamera) {
                // collect all camera intents
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                new IsLog(TAG,"fontCameraOPT "+fontCameraOPT);
                if (fontCameraOPT) {
                    captureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
                } else {
                    captureIntent.putExtra("android.intent.extras.CAMERA_FACING", 0);
                }

                List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
                for (ResolveInfo res : listCam) {
                    Intent intent = new Intent(captureIntent);
                    intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
                    intent.setPackage(res.activityInfo.packageName);
                    if (outputFileUri != null) {
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    }
                    allIntents.add(intent);
                }
            }
        } else {
            new IsLog(TAG,"fontCameraOPT1234590-== "+fontCameraOPT);
        }
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);
        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");
        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));
        return chooserIntent;
    }

    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = activity != null ? activity.getExternalFilesDir("") :
                fragment.getActivity().getExternalFilesDir("");
        if (getImage != null) {
            outputFileUri = FileProvider.getUriForFile(activity != null ? activity : fragment.getActivity(),
                    activity != null ? activity.getApplicationContext().getPackageName() + ".app_file_provider" :
                            fragment.getActivity().getApplicationContext().getPackageName() + ".app_file_provider",
                    new File(getImage.getPath(), "profile.png"));
        }
        return outputFileUri;
    }

    private String getPickImageResultFilePath(Intent data) {
        boolean isCamera = data == null || data.getData() == null;
        if (isCamera) return getCaptureImageOutputUri().getPath();
        else return getRealPathFromURI(data.getData());
    }

    private String getRealPathFromURI(Uri contentUri) {
        OutputStream out;
        File file = new File(getFilename());

        try {
            if (file.createNewFile()) {
                InputStream iStream = activity != null ? activity.getContentResolver().openInputStream(contentUri)
                        : fragment.getContext().getContentResolver().openInputStream(contentUri);
                byte[] inputData = getBytes(iStream);
                out = new FileOutputStream(file);
                out.write(inputData);
                out.close();
                return file.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    public String getImageFilePath(Intent data) {
        if (!isCompress)
            return getPickImageResultFilePath(data);
        else if (getPickImageResultFilePath(data) != null) {
            new ImageCompression(activity != null ? activity : fragment.getActivity(), getPickImageResultFilePath(data),
                    imageCompressionListener).execute();
        }
        return null;
    }

    public void addOnCompressListener(ImageCompressionListener imageCompressionListener) {
        this.imageCompressionListener = imageCompressionListener;
    }

    private String getFilename() {
        Context context = activity != null ? activity : fragment.getContext();
        File mediaStorageDir = new File(context.getExternalFilesDir(""), "uncompressed");
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }
        String mImageName = "IMG_" + String.valueOf(System.currentTimeMillis()) + ".png";
        return mediaStorageDir.getAbsolutePath() + "/" + mImageName;
    }
}
