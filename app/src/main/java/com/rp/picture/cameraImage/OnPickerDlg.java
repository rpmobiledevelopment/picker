package com.rp.picture.cameraImage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.rp.picture.R;

public class OnPickerDlg {

    private String TAG = OnPickerDlg.class.getSimpleName();
    TextView tv_header,tv_camera,tv_gallery,tv_cancel;
    public OnPickerDlg(Activity mActivity,ActivityResultLauncher<Intent> someActivityResultLauncher) {

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
}
