package com.rp.picture.cameraImage;

import static android.os.Build.VERSION_CODES.M;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.rp.picture.R;
import com.rp.uihelpher.log.IsLog;

import java.io.File;
import java.io.Serializable;

public class OnPicker extends Fragment {

    private String TAG = OnPicker.class.getSimpleName();
    private OnPickerListener onPickerListener;
    private Activity mActivity;
    private String selectLang;
    TextView tv_header,tv_camera,tv_gallery,tv_cancel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mActivity = getActivity();
//        onDlg();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();

        if (getArguments() != null) {
            selectLang = getArguments().getString("selectLang");
            onPickerListener = (OnPickerListener) getArguments().getSerializable("listener");
        }
    }

    // Dashboard
    public OnPicker(Activity mActivity,String selectLang, OnPickerListener listener) {
        this.mActivity = mActivity;
        this.selectLang = selectLang;
        onPickerListener = listener;

        onDlg();
    }

        protected void onDlg() {

            final BottomSheetDialog dialog = new BottomSheetDialog(mActivity);
            LayoutInflater inflater = mActivity.getLayoutInflater();
            View convertView = inflater.inflate(R.layout.dlg_pic_opt, null);

            tv_header = convertView.findViewById(R.id.tv_header);
            tv_camera = convertView.findViewById(R.id.tv_camera);
            tv_gallery = convertView.findViewById(R.id.tv_gallery);
            tv_cancel = convertView.findViewById(R.id.tv_cancel);

            convertView.findViewById(R.id.cv_bg).setBackground(mActivity.getResources().getDrawable(R.drawable.bg_cv_dlg));

            convertView.findViewById(R.id.tv_cancel).setOnClickListener(view -> dialog.dismiss());

            convertView.findViewById(R.id.ll_gallery).setOnClickListener(view -> {
                Intent intent = new Intent(mActivity, ImgSelectAct.class);
                intent.putExtra(ImgSelectAct.FLAG_COMPRESS, true);
                intent.putExtra(ImgSelectAct.FLAG_CAMERA, true);
                intent.putExtra(ImgSelectAct.FLAG_GALLERY, true);
                intent.putExtra(ImgSelectAct.FLAG_CAMERA_YES_NO, false);
//                startActivityForResult(intent, selectPage);
                someActivityResultLauncher.launch(intent);

                dialog.dismiss();
            });
            convertView.findViewById(R.id.ll_camera).setOnClickListener(view -> {
                Intent intent = new Intent(mActivity, ImgSelectAct.class);
                intent.putExtra(ImgSelectAct.FLAG_COMPRESS, true);
                intent.putExtra(ImgSelectAct.FLAG_CAMERA, true);
                intent.putExtra(ImgSelectAct.FLAG_GALLERY, true);
                intent.putExtra(ImgSelectAct.FLAG_CAMERA_YES_NO, true);
                someActivityResultLauncher.launch(intent);
                dialog.dismiss();
            });

            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            Window window = dialog.getWindow();
            lp.copyFrom(window.getAttributes());

            Display display = mActivity.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            lp.width = size.x;
            lp.height = size.y;
            // This makes the dialog take up the full width
            window.setAttributes(lp);
            dialog.setCancelable(true);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(convertView);
            dialog.setTitle("");
            dialog.show();
        }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        try {
                            assert data != null;
                            String filePath = data.getStringExtra(ImgSelectAct.RESULT_FILE_PATH);
                            Uri imageUri = Uri.parse("");
                            if (Build.VERSION.SDK_INT >= M) {
                                imageUri = Uri.fromFile(new File(filePath));
                                onPickerListener.onViewImg(imageUri.toString(), "");
                            } else {
                                Bitmap selectedImage = BitmapFactory.decodeFile(filePath);
                                imageUri = Uri.fromFile(SaveBitmap.save(selectedImage));
                                onPickerListener.onViewImg(imageUri.toString(), "");
                            }

                            new IsLog(TAG, "imageUri======" + imageUri.toString());
                        } catch (NumberFormatException | NullPointerException e) {
                            new IsLog(TAG, "NumberFormatException " + e.getMessage());
                        } catch (Exception e) {
                            new IsLog(TAG, "Exception " + e.getMessage());
                        }
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
