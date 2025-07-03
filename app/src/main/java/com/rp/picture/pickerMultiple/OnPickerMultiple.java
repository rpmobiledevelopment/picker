package com.rp.picture.pickerMultiple;

import static android.os.Build.VERSION_CODES.M;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rp.picture.cameraImage.SaveBitmap;
import com.rp.uihelpher.log.IsLog;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;


public class OnPickerMultiple extends Fragment {

    private String TAG = OnPickerMultiple.class.getSimpleName();
    private OnPickerListener onPickerListener;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            onPickerListener = (OnPickerListener) getArguments().getSerializable("listener");
        }
    }

    // Dashboard
    public OnPickerMultiple(Activity mActivity, OnPickerListener listener) {
        onPickerListener = listener;

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(mActivity, ImgSelectMultipleAct.class);
            intent.putExtra(ImgSelectMultipleAct.FLAG_COMPRESS, true);
            intent.putExtra(ImgSelectMultipleAct.FLAG_CAMERA, true);
            intent.putExtra(ImgSelectMultipleAct.FLAG_GALLERY, true);
            intent.putExtra(ImgSelectMultipleAct.FLAG_CAMERA_YES_NO, false);
            someActivityResultLauncher.launch(intent);
        },200);

    }
    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    try {
                        assert data != null;
                        ArrayList<String> selectedImages = data.getStringArrayListExtra(ImgSelectMultipleAct.RESULT_FILE_PATH);
                        assert selectedImages != null;
                        for (String path : selectedImages) {
                            Uri imageUri = Uri.parse("");
                            if (Build.VERSION.SDK_INT >= M) {
                                imageUri = Uri.fromFile(new File(path));
                                //  onPickerListener.onViewImg(imageUri.toString(),"");
                            } else {
                                Bitmap selectedImage = BitmapFactory.decodeFile(path);
                                imageUri = Uri.fromFile(SaveBitmap.save(selectedImage));
                                //  onPickerListener.onViewImg(imageUri.toString(),"");
                            }
                            new IsLog(TAG,"imageUri=============="+ imageUri.toString());
                        }
                    } catch (NumberFormatException | NullPointerException e) {
                        new IsLog(TAG, "NumberFormatException ================ " + e.getMessage());
                    } catch (Exception e) {
                        new IsLog(TAG, "Exception ================ " + e.getMessage());
                    }
                }
            });

    public interface OnPickerListener extends Parcelable, Serializable {

        void onViewImg(String imageUri,String opt);

        @Override
        default int describeContents() {
            return 0;
        }

        @Override
        default void writeToParcel(@NonNull Parcel dest, int flags) {

        }
        Creator<OnPickerListener> CREATOR = new Creator<OnPickerListener>() {
            @Override
            public OnPickerListener createFromParcel(Parcel in) {
                return (imageUri, opt) -> {

                };
            }

            @Override
            public OnPickerListener[] newArray(int size) {
                return new OnPickerListener[size];
            }
        };
    }
}
