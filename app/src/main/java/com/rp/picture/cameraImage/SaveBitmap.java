package com.rp.picture.cameraImage;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;

import com.rp.picture.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SaveBitmap {

    private static String TAG = SaveBitmap.class.getSimpleName();

    public static File save(Bitmap bmp) {
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        OutputStream outStream = null;
        File file = new File(extStorageDirectory, "temp.jpg");
        if (file.exists()) {
            file.delete();
            file = new File(extStorageDirectory, "temp.jpg");
        }
        try {
            outStream = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }
    public static File saveFile(Bitmap bmp, String s) {
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        OutputStream outStream = null;
        // String temp = null;
        File file = new File(extStorageDirectory, getDateTime()+"temp.jpg");
        if (file.exists()) {
            file.delete();
            file = new File(extStorageDirectory, getDateTime()+"temp.jpg");
        }
        try {
            outStream = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    public static File saveImage(Bitmap data,String imgPro) {
        File createFolder = new File(Environment.getExternalStorageDirectory(),"ScreenShotImg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            createFolder = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+
                    "/"+ R.string.folder_name );
        } else {
            createFolder = new File(Environment.getExternalStorageDirectory() + "/"+ R.string.folder_name);
        }

        if(!createFolder.exists())
            createFolder.mkdir();

        Date date = new Date();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(date);
        File saveImage = new File(createFolder,timeStamp+""+imgPro+".jpg");
        try {
            OutputStream outputStream = new FileOutputStream(saveImage);
            data.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return saveImage;
    }

    public static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}
